package dev.theolm.bydapps.core.session

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps this app's Gecko profile in step with the other BYD apps.
 *
 * Import only happens when the local profile is signed out: importing over an
 * account that is already signed in could silently switch accounts. Export
 * only happens with a signed-in session, so a signed-out app never overwrites
 * a good bundle.
 */
internal class SessionSync(
    private val context: Context,
    private val partnerPackages: List<String>,
    private val bridge: SessionBridge,
) {

    private val store = SessionStore(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Restores a sibling session when needed, then calls [onReady].
     *
     * The page is never blocked for longer than [READY_TIMEOUT_MS]; if the
     * import lands after the page started loading, [onLateImport] lets the
     * activity reload it.
     */
    fun prepare(onReady: () -> Unit, onLateImport: () -> Unit) {
        val ready = AtomicBoolean(false)
        val proceed = {
            if (ready.compareAndSet(false, true)) {
                onReady()
            }
        }

        mainHandler.postDelayed(proceed, READY_TIMEOUT_MS)

        bridge.status { signedIn ->
            Log.d(TAG, "Local session signedIn=$signedIn")
            if (signedIn || partnerPackages.isEmpty()) {
                proceed()
                return@status
            }
            findPartnerBundle { bundle ->
                if (bundle == null) {
                    proceed()
                    return@findPartnerBundle
                }
                bridge.restore(bundle) { result ->
                    if (result.applied <= 0) {
                        Log.w(
                            TAG,
                            "Sibling session could not be restored: " +
                                "${result.failed} cookies failed, ${result.firstError}"
                        )
                        proceed()
                        return@restore
                    }
                    Log.d(
                        TAG,
                        "Restored ${result.applied} cookies " +
                            "(${result.failed} failed, ${result.firstError}), " +
                            "hash=${bundle.hash.take(8)}"
                    )
                    if (ready.get()) onLateImport() else proceed()
                }
            }
        }
    }

    /** Exports the signed-in session so a sibling app can pick it up. */
    fun save() {
        bridge.status { signedIn ->
            Log.d(TAG, "Export check signedIn=$signedIn")
            if (!signedIn) return@status
            bridge.export { bundle ->
                if (bundle == null || !bundle.signedIn || bundle.cookies.isEmpty()) {
                    return@export
                }
                SessionBridge.runInBackground {
                    val current = runCatching { store.read() }.getOrNull()
                    if (current?.hash == bundle.hash) {
                        // Switching apps is common in the car; do not rewrite
                        // an identical bundle on every stop.
                        return@runInBackground
                    }
                    runCatching { store.write(bundle) }
                        .onSuccess {
                            Log.d(
                                TAG,
                                "Exported ${bundle.cookies.size} cookies, " +
                                    "hash=${bundle.hash.take(8)}"
                            )
                        }
                        .onFailure { Log.w(TAG, "Cannot write session bundle", it) }
                }
            }
        }
    }

    private fun findPartnerBundle(callback: (SessionBundle?) -> Unit) {
        SessionBridge.runInBackground {
            val newest = partnerPackages
                .mapNotNull { readPartnerBundle(it) }
                .filter { it.signedIn && it.cookies.isNotEmpty() }
                .maxByOrNull { it.createdAt }
            SessionBridge.runOnMain { callback(newest) }
        }
    }

    private fun readPartnerBundle(packageName: String): SessionBundle? {
        val uri = Uri.parse("content://$packageName${SessionProvider.AUTHORITY_SUFFIX}")
        return runCatching {
            context.contentResolver
                .call(uri, SessionProvider.METHOD_GET, null, null)
                ?.getString(SessionProvider.KEY_JSON)
                ?.let(SessionBundle::fromJson)
        }.onFailure {
            // Expected when the sibling is not installed or is signed with a
            // different certificate; the app simply starts signed out.
            Log.d(TAG, "No session available from $packageName: ${it.message}")
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "SessionSync"
        private const val READY_TIMEOUT_MS = 1_500L
    }
}
