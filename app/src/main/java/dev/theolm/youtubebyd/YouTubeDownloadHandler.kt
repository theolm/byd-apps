package dev.theolm.youtubebyd

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Handles file downloads from the WebView
 */
internal class YouTubeDownloadHandler(
    private val context: Context
) : DownloadListener {
    
    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        val request = DownloadManager.Request(url.toUri()).apply {
            setMimeType(mimeType)
            addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            addRequestHeader("User-Agent", userAgent)
            setDescription("Downloading...")
            setTitle(url.toUri().lastPathSegment)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                url.toUri().lastPathSegment
            )
        }
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
    }
}