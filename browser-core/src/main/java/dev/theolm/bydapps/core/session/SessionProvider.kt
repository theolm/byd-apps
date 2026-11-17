package dev.theolm.bydapps.core.session

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * Shares the last exported session bundle with the sibling BYD apps.
 *
 * The provider only serves a file written by [SessionStore]; it never starts
 * GeckoView. That keeps a cross-app read cheap even when the owning app is not
 * running. Access is limited to apps signed with the same certificate through
 * the signature-level permission declared in the manifest.
 */
class SessionProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_GET) return null
        val context = context ?: return null
        val bundle = runCatching { SessionStore(context).read() }.getOrNull()
        return Bundle().apply {
            putString(KEY_JSON, bundle?.toJson())
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        const val AUTHORITY_SUFFIX = ".sessionsync"
        const val PERMISSION_SUFFIX = ".permission.SESSION_SYNC"
        const val METHOD_GET = "getSession"
        const val KEY_JSON = "json"
    }
}
