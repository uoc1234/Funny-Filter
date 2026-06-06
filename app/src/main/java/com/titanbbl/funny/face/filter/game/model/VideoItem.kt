package com.titanbbl.funny.face.filter.game.model

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.titanbbl.funny.face.filter.game.ui.component.main.utils.BitmapCache
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VideoItem(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val dateCreated: Long,
    val duration: Long,
    var isSelected: Boolean = false
) {
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
            else -> "${fileSize / (1024 * 1024 * 1024)} GB"
        }
    }

    fun getFormattedDate(): String {
        val date = Date(dateCreated)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    fun getFormattedDuration(): String {
        if (duration <= 0) return "Unknown"

        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return if (minutes > 0) {
            String.format("%d:%02d", minutes, remainingSeconds)
        } else {
            String.format("%ds", remainingSeconds)
        }
    }

    /**
     * Lấy frame cuối cùng từ video và trả về Bitmap (sử dụng cache)
     * @param context Context để truy cập MediaStore
     * @return Bitmap của frame cuối cùng hoặc null nếu có lỗi
     */
    suspend fun getLastFrameBitmap(context: Context): Bitmap? {
        return BitmapCache.Companion.getInstance().getBitmap(context, filePath, true)
    }

    /**
     * Lấy thumbnail từ video sử dụng MediaStore (hiệu quả hơn)
     * @param context Context để truy cập MediaStore
     * @return Bitmap thumbnail hoặc null nếu có lỗi
     */


    /**
     * Lấy frame đầu tiên từ video (sử dụng cache)
     * @param context Context để truy cập MediaStore
     * @return Bitmap của frame đầu tiên hoặc null nếu có lỗi
     */
    suspend fun getFirstFrameBitmap(context: Context): Bitmap? {
        return BitmapCache.Companion.getInstance().getBitmap(context, filePath, false)
    }

    /**
     * Lấy frame tại thời điểm cụ thể
     * @param timeMs Thời gian tính bằng milliseconds
     * @return Bitmap của frame tại thời điểm đó hoặc null nếu có lỗi
     */
    fun getFrameAtTime(timeMs: Long): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)

            // Lấy frame tại thời điểm cụ thể
            val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}