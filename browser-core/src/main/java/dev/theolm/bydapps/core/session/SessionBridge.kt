package dev.theolm.bydapps.core.session

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import java.util.concurrent.Executors

/**
 * Talks to the built-in session bridge extension.
 *
 * The extension is the only part of GeckoView that can read and write the
 * cookie jar: [org.mozilla.geckoview.StorageController] no longer exposes
 * cookies in GeckoView 160. Requests are queued until the extension's
 * background page connects and fail by timeout, so a broken extension never
 * blocks the browser.
 */
internal class SessionBridge private constructor(
    private val runtime: GeckoRuntime,
) {

    /** Outcome of restoring a bundle into the cookie jar. */
    data class ImportResult(
        val applied: Int,
        val failed: Int,
        val firstError: String?,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = LinkedHashMap<Int, PendingRequest>()
    private var nextId = 1
    private var port: WebExtension.Port? = null
    private var installRequested = false

    /** Installs the built-in extension; safe to call on every activity start. */
    fun start() {
        if (installRequested) return
        installRequested = true
        runtime.webExtensionController
            .ensureBuiltIn(EXTENSION_URI, EXTENSION_ID)
            .accept({ ext ->
                if (ext == null) {
                    Log.e(TAG, "Session bridge extension is unavailable")
                    return@accept
                }
                // setMessageDelegate must run on the UI thread; GeckoResult
                // callbacks are not guaranteed to land there.
                mainHandler.post {
                    ext.setMessageDelegate(messageDelegate, NATIVE_APP)
                    Log.d(TAG, "Session bridge ${ext.metaData.version} ready")
                }
            }, { error ->
                Log.e(TAG, "Session bridge install failed", error)
            })
    }

    fun status(callback: (Boolean) -> Unit) {
        send(TYPE_STATUS, null, TIMEOUT_MS) { result ->
            callback(result?.optBoolean("signedIn") == true)
        }
    }

    fun export(callback: (SessionBundle?) -> Unit) {
        send(TYPE_EXPORT, null, TIMEOUT_MS) { result ->
            if (result == null) {
                callback(null)
                return@send
            }
            val array = result.optJSONArray(FIELD_COOKIES) ?: JSONArray()
            val cookies = (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(SessionCookie::fromJson)
            }
            callback(
                SessionBundle(
                    createdAt = System.currentTimeMillis(),
                    signedIn = result.optBoolean(FIELD_SIGNED_IN),
                    cookies = cookies,
                )
            )
        }
    }

    /** Restores [bundle] and reports how many cookies were written. */
    fun restore(bundle: SessionBundle, callback: (ImportResult) -> Unit) {
        val payload = JSONObject().put(
            FIELD_COOKIES,
            JSONArray().apply { bundle.cookies.forEach { put(it.toJson()) } }
        )
        send(TYPE_IMPORT, payload, IMPORT_TIMEOUT_MS) { result ->
            if (result == null) {
                callback(ImportResult(0, bundle.cookies.size, null))
                return@send
            }
            val errors = result.optJSONArray(FIELD_ERRORS)
            callback(
                ImportResult(
                    applied = result.optInt(FIELD_APPLIED),
                    failed = result.optInt(FIELD_FAILED),
                    firstError = errors?.optJSONObject(0)?.let {
                        "${it.optString("name")} (${it.optString("domain")}): " +
                            it.optString("error")
                    },
                )
            )
        }
    }

    private fun send(
        type: String,
        payload: JSONObject?,
        timeoutMs: Long,
        callback: (JSONObject?) -> Unit
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            sendOnMain(type, payload, timeoutMs, callback)
        } else {
            mainHandler.post { sendOnMain(type, payload, timeoutMs, callback) }
        }
    }

    private fun sendOnMain(
        type: String,
        payload: JSONObject?,
        timeoutMs: Long,
        callback: (JSONObject?) -> Unit
    ) {
        val id = nextId++
        val message = JSONObject().put(FIELD_ID, id).put(FIELD_TYPE, type)
        if (payload != null) message.put(FIELD_PAYLOAD, payload)

        val timeout = Runnable {
            val request = pending.remove(id) ?: return@Runnable
            Log.w(TAG, "Request $type timed out")
            request.callback(null)
        }
        pending[id] = PendingRequest(message, callback, timeout)
        mainHandler.postDelayed(timeout, timeoutMs)

        val currentPort = port
        if (currentPort != null) {
            currentPort.postMessage(message)
        }
    }

    private val messageDelegate = object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            this@SessionBridge.port = port
            port.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    handleResponse(message)
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@SessionBridge.port === port) {
                        this@SessionBridge.port = null
                    }
                }
            })
            Log.d(TAG, "Session bridge connected")
            // The background can connect more than once, for example while an
            // updated version of the extension replaces the old one. Re-send
            // pending requests to the newest port so none are stranded on a
            // port that is about to be dropped.
            pending.values.forEach {
                port.postMessage(it.message)
            }
        }
    }

    private fun handleResponse(message: Any?) {
        val json = message as? JSONObject ?: return
        val id = json.optInt(FIELD_ID, -1)
        val request = pending.remove(id) ?: return
        mainHandler.removeCallbacks(request.timeout)
        if (!json.optBoolean(FIELD_OK)) {
            Log.w(TAG, "Session request failed: ${json.optString(FIELD_ERROR)}")
            request.callback(null)
            return
        }
        request.callback(json.optJSONObject(FIELD_RESULT))
    }

    private class PendingRequest(
        val message: JSONObject,
        val callback: (JSONObject?) -> Unit,
        val timeout: Runnable,
    )

    companion object {
        private const val TAG = "SessionBridge"
        private const val EXTENSION_ID = "session-bridge@byd.theolm.dev"
        // GeckoView installs built-in extensions from an unpacked directory in
        // the APK assets, not from an XPI file.
        private const val EXTENSION_URI =
            "resource://android/assets/web_extensions/session-bridge/"
        private const val NATIVE_APP = "browser"

        private const val TYPE_STATUS = "status"
        private const val TYPE_EXPORT = "export"
        private const val TYPE_IMPORT = "import"

        private const val FIELD_ID = "id"
        private const val FIELD_TYPE = "type"
        private const val FIELD_PAYLOAD = "payload"
        private const val FIELD_OK = "ok"
        private const val FIELD_ERROR = "error"
        private const val FIELD_RESULT = "result"
        private const val FIELD_COOKIES = "cookies"
        private const val FIELD_SIGNED_IN = "signedIn"
        private const val FIELD_APPLIED = "applied"
        private const val FIELD_FAILED = "failed"
        private const val FIELD_ERRORS = "errors"

        private const val TIMEOUT_MS = 4_000L
        private const val IMPORT_TIMEOUT_MS = 8_000L

        private val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "session-sync").apply { isDaemon = true }
        }

        @Volatile
        private var instance: SessionBridge? = null

        /**
         * One bridge per process: requests must survive activity recreation so
         * an export started in onStop can still finish.
         */
        fun get(runtime: GeckoRuntime): SessionBridge = instance ?: synchronized(this) {
            instance ?: SessionBridge(runtime).also { instance = it }
        }

        fun runInBackground(block: () -> Unit) {
            executor.execute(block)
        }

        fun runOnMain(block: () -> Unit) {
            Handler(Looper.getMainLooper()).post(block)
        }
    }
}
