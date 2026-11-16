package dev.tunderlucky.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil

data class DlInfo(val status: Int, val done: Long, val total: Long, val reason: Int)

object DownloadHelper {
    fun fileName(url: String): String = URLUtil.guessFileName(url, null, null)

    fun enqueue(ctx: Context, url: String): Long {
        val uri = Uri.parse(url)
        require(uri.scheme == "https" || uri.scheme == "http") { "Only http(s) direct links are supported" }
        val name = fileName(url)
        val r = DownloadManager.Request(uri).setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        return ctx.getSystemService(DownloadManager::class.java).enqueue(r)
    }

    fun query(ctx: Context, id: Long): DlInfo? {
        val dm = ctx.getSystemService(DownloadManager::class.java)
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) return null
            fun i(n: String) = c.getInt(c.getColumnIndexOrThrow(n))
            fun l(n: String) = c.getLong(c.getColumnIndexOrThrow(n))
            return DlInfo(i(DownloadManager.COLUMN_STATUS), l(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                l(DownloadManager.COLUMN_TOTAL_SIZE_BYTES), i(DownloadManager.COLUMN_REASON))
        }
    }

    fun open(ctx: Context, id: Long): Boolean {
        val dm = ctx.getSystemService(DownloadManager::class.java)
        val uri = dm.getUriForDownloadedFile(id) ?: return false
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val mime = dm.getMimeTypeForDownloadedFile(id) ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        return try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (e: Exception) { false }
    }
}
