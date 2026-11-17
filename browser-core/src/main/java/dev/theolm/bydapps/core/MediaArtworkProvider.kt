package dev.theolm.bydapps.core

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves the latest GeckoView artwork to car launchers that require a local
 * content URI in MediaMetadata, as recommended for Android Automotive.
 */
class MediaArtworkProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (uri.path != "/current") {
            throw FileNotFoundException("Unknown artwork path: ${uri.path}")
        }
        val file = File(requireNotNull(context).cacheDir, ARTWORK_FILE)
        if (!file.exists()) {
            throw FileNotFoundException("No artwork available")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "image/png"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        const val AUTHORITY_SUFFIX = ".mediaartwork"
        const val ARTWORK_FILE = "media_artwork.png"
    }
}
