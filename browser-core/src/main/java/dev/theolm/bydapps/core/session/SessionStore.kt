package dev.theolm.bydapps.core.session

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Persists the last exported session bundle in the app's private storage.
 *
 * Writes are atomic so a process death in the middle of an export never
 * leaves a truncated bundle behind.
 */
internal class SessionStore(context: Context) {

    private val file = File(context.filesDir, DIRECTORY + File.separator + FILE_NAME)

    fun read(): SessionBundle? {
        val raw = synchronized(LOCK) {
            if (!file.exists()) return null
            runCatching { file.readText() }.getOrNull()
        } ?: return null
        return SessionBundle.fromJson(raw)
    }

    fun write(bundle: SessionBundle) {
        synchronized(LOCK) {
            val directory = file.parentFile ?: return
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Cannot create $directory")
            }
            val temp = File(directory, "$FILE_NAME.tmp")
            temp.writeText(bundle.toJson())
            if (!temp.renameTo(file)) {
                file.delete()
                if (!temp.renameTo(file)) {
                    temp.delete()
                    throw IOException("Cannot replace $file")
                }
            }
        }
    }

    companion object {
        const val DIRECTORY = "session"
        const val FILE_NAME = "session-bundle.json"

        private val LOCK = Any()
    }
}
