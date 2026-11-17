package dev.theolm.bydapps.core.session

import android.content.Context
import android.net.Uri
import android.util.Log

/**
 * Keeps this app's Gecko profile in step with the other BYD apps.
 *
 * Import only happens when the local profile is signed out: importing over an
 * account that is already signed in could silently switch accounts. Export
 * only happens with a signed-in session, so a signed-out app never overwrites
 * a good bundle.
 *
 * The page is never blocked on this work. [prepare] runs after the Activity has
 * started the first load and calls [onLateImport] if a restore lands after the
 * page has already begun.
 */
internal class SessionSync(
    private val context: Context,
    private val partnerPackages: List<String>,
    private val bridge: SessionBridge,
) {

    private val store = SessionStore(context)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Restores a sibling session when needed.
     *
     * The last known sign-in state is cached so the common signed-in case does
     * not wait for the bridge extension. A cached signed-in profile is still
     * refreshed in the background; a stale "true" only delays an import until
     * the next launch and never overwrites an account that is already signed
     * in.
     */
    fun prepare(onLateImport: () -> Unit) {
        if (lastKnownSignedIn() == true) {
            Log.d(TAG, "Local session cached signedIn=true")
            refreshStatus()
            return
        }

        bridge.status { signedIn ->
            if (signedIn == null) {
                Log.w(TAG, "Local session status unavailable")
                return@status
            }
            Log.d(TAG, "Local session signedIn=$signedIn")
            setLastKnownSignedIn(signedIn)
            if (signedIn || partnerPackages.isEmpty()) return@status

            findPartnerBundle { bundle ->
                if (bundle == null) return@findPartnerBundle
                bridge.restore(bundle) { result ->
                    if (result.applied <= 0) {
                        Log.w(
                            TAG,
                            "Sibling session could not be restored: " +
                                "${result.failed} cookies failed, ${result.firstError}"
                        )
                        return@restore
                    }
                    setLastKnownSignedIn(true)
                    Log.d(
                        TAG,
                        "Restored ${result.applied} cookies " +
                            "(${result.failed} failed, ${result.firstError}), " +
                            "hash=${bundle.hash.take(8)}"
                    )
                    onLateImport()
                }
            }
        }
    }

    /** Exports the signed-in session so a sibling app can pick it up. */
    fun save() {
        // export() already reports the sign-in state, so it replaces the old
        // status + export pair and scans the cookie jar only once.
        bridge.export { bundle ->
            if (bundle == null) {
                Log.d(TAG, "Session export unavailable")
                return@export
            }
            setLastKnownSignedIn(bundle.signedIn)
            Log.d(
                TAG,
                "Export bundle signedIn=${bundle.signedIn}, " +
                    "cookies=${bundle.cookies.size}"
            )
            if (!bundle.signedIn || bundle.cookies.isEmpty()) return@export

            SessionBridge.runInBackground {
                val current = runCatching { store.read() }.getOrNull()
                if (current?.hash == bundle.hash) {
                    // Switching apps is common in the car; do not rewrite an
                    // identical bundle on every stop.
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

    private fun refreshStatus() {
        bridge.status { signedIn ->
            if (signedIn == null) {
                Log.d(TAG, "Local session refresh unavailable")
                return@status
            }
            Log.d(TAG, "Local session refresh signedIn=$signedIn")
            setLastKnownSignedIn(signedIn)
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

    private fun lastKnownSignedIn(): Boolean? =
        if (prefs.contains(KEY_SIGNED_IN)) prefs.getBoolean(KEY_SIGNED_IN, false) else null

    private fun setLastKnownSignedIn(signedIn: Boolean) {
        if (prefs.contains(KEY_SIGNED_IN) && prefs.getBoolean(KEY_SIGNED_IN, false) == signedIn) {
            return
        }
        prefs.edit().putBoolean(KEY_SIGNED_IN, signedIn).apply()
    }

    private companion object {
        private const val TAG = "SessionSync"
        private const val PREFS_NAME = "session-sync"
        private const val KEY_SIGNED_IN = "signedIn"
    }
}
