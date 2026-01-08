package info.benjaminhill.videogofaster

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Responsibility: Handles all platform-specific storage interactions.
 * Separation of Concerns: This object isolates low-level File I/O and Android MediaStore
 * operations from the business logic, ensuring other components don't need to know
 * how bytes are moved or where files are stored.
 */
object StorageUtils {

    fun copyUriToCache(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "source_video.mp4")

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun saveVideoToMediaStore(
        context: Context,
        videoFile: File,
        originalName: String,
        speed: Int
    ): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${originalName}_${speed}x_10bit.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/VideoGoFaster"
            )
        }

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        return if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(videoFile).use { input ->
                        input.copyTo(output)
                    }
                }
                // Return a user-friendly string (e.g., filename or URI)
                "${originalName}_${speed}x_10bit.mp4"
            } catch (e: Exception) {
                e.printStackTrace()
                // cleanup empty entry
                contentResolver.delete(uri, null, null)
                null
            }
        } else {
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "video"
        val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                if (index != -1) {
                    name = cursor.getString(index)
                }
            }
        }
        // Remove extension
        return name.substringBeforeLast('.')
    }
}
