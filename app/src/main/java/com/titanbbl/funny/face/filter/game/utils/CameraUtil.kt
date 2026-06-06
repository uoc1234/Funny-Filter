package com.titanbbl.funny.face.filter.game.utils

import android.content.Context
import android.os.Environment
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
\\ \\ \\ \\ \\ \\ \\ \\ || || || || || || // // // // // // // //
\\ \\ \\ \\ \\ \\ \\        _ooOoo_          // // // // // // //
\\ \\ \\ \\ \\ \\          o8888888o            // // // // // //
\\ \\ \\ \\ \\             88" . "88               // // // // //
\\ \\ \\ \\                (| -_- |)                  // // // //
\\ \\ \\                   O\  =  /O                     // // //
\\ \\                   ____/`---'\____                     // //
\\                    .'  \\|     |//  `.                      //
==                   /  \\|||  :  |||//  \                     ==
==                  /  _||||| -:- |||||-  \                    ==
==                  |   | \\\  -  /// |   |                    ==
==                  | \_|  ''\---/''  |   |                    ==
==                  \  .-\__  `-`  ___/-. /                    ==
==                ___`. .'  /--.--\  `. . ___                  ==
==              ."" '<  `.___\_<|>_/___.'  >'"".               ==
==            | | :  `- \`.;`\ _ /`;.`/ - ` : | |              \\
//            \  \ `-.   \_ __\ /__ _/   .-` /  /              \\
//      ========`-.____`-.___\_____/___.-`____.-'========      \\
//                           `=---='                           \\
// //   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  \\ \\
// // //    Buddha blessed    Never BUG    Never modify   \\ \\ \\
 **/
object CameraUtil {

    fun createVideoFile(context: Context): File {
        return createVideoFile(context, "video")
    }

    fun createVideoFile(context: Context, prefix: String): File {
        val timeStamp = SimpleDateFormat("HH_mm_ss_dd_MM_yyyy", Locale.getDefault()).format(Date())
        val fileName = "${prefix}_$timeStamp.mp4"
        val videoDir = File(context.filesDir, "Videos")
        if (!videoDir.exists()) videoDir.mkdirs()
        return File(videoDir, fileName)
    }

    fun getVideoFilePath(context: Context): String {
        val videoDir = File(context.filesDir, "Videos")
        if (!videoDir.exists()) videoDir.mkdirs()
        return videoDir.absolutePath
    }

    fun isVideoFileValid(file: File): Boolean {
        return file.exists() && file.length() > 0 && file.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv")
    }

    fun getVideoFileSize(file: File): String {
        return if (file.exists()) {
            val sizeInBytes = file.length()
            when {
                sizeInBytes < 1024 -> "$sizeInBytes B"
                sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
                else -> "${sizeInBytes / (1024 * 1024)} MB"
            }
        } else "0 B"
    }

    fun cleanupOldVideoFiles(context: Context, maxAgeInHours: Long = 24) {
        try {
            val videoDir = File(context.filesDir, "Videos")
            if (!videoDir.exists()) return

            val currentTime = System.currentTimeMillis()
            val maxAgeInMillis = maxAgeInHours * 60 * 60 * 1000

            videoDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv")) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAgeInMillis) {
                        if (file.delete()) {
                            Timber.d("Deleted old video file: ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up old video files")
        }
    }
    
    /**
     * Coroutine-based version of cleanupOldVideoFiles for background execution
     * Use this when calling from UI thread to avoid blocking
     */
    suspend fun cleanupOldVideoFilesAsync(context: Context, maxAgeInHours: Long = 24) {
        withContext(Dispatchers.IO) {
            try {
                val videoDir = File(context.filesDir, "Videos")
                if (!videoDir.exists()) return@withContext

                val currentTime = System.currentTimeMillis()
                val maxAgeInMillis = maxAgeInHours * 60 * 60 * 1000

                videoDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv")) {
                        val fileAge = currentTime - file.lastModified()
                        if (fileAge > maxAgeInMillis) {
                            if (file.delete()) {
                                Timber.d("Deleted old video file: ${file.name}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cleaning up old video files")
            }
        }
    }

    fun getVideoFilesCount(context: Context): Int {
        return try {
            val videoDir = File(context.filesDir, "Videos")
            if (!videoDir.exists()) return 0

            videoDir.listFiles()?.count { file ->
                file.isFile && file.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv")
            } ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Error counting video files")
            0
        }
    }

}