package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import com.otaliastudios.cameraview.PictureResult
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.WhoLookLikeActivity
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for handling image capture and file operations
 */
object ImageCaptureUtils {
    
    /**
     * Create a temporary file for storing captured images
     * @param context Application context
     * @return File object for the temporary file
     */
    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    /**
     * Save a picture result to a file
     * @param pictureResult Picture result from camera
     * @param context Application context
     * @return Path to the saved file
     */
    fun savePictureToFile(pictureResult: PictureResult, context: Context): String {
        val file = createTempImageFile(context)
        
        try {
            pictureResult.data?.let { data ->
                // Save the original image
                val fos = FileOutputStream(file)
                fos.write(data)
                fos.close()
                
                Timber.d("Image saved to ${file.absolutePath}")
                return file.absolutePath
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving picture to file")
        }
        
        throw IllegalStateException("Failed to save picture to file")
    }


    // 1) Extension: await toFile()
    suspend fun PictureResult.saveToFileAwait(out: File): File =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            try {
                toFile(out) { f ->
                    if (f != null) cont.resume(f) {}
                    else cont.cancel(IllegalStateException("toFile() returned null"))
                }
            } catch (e: Exception) {
                cont.cancel(e)
            }
        }


    /**
     * Load an image from a file path
     * @param filePath Path to the image file
     * @return Bitmap of the loaded image
     */
    fun loadImageFromFile(filePath: String): Bitmap {
        return BitmapFactory.decodeFile(filePath)
    }


    fun savePictureToFile(result: PictureResult, context: Context, outputFile: File): File? {
        try {
            result.toFile(outputFile) { file ->

            }
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Rotate a bitmap
     * @param source Source bitmap
     * @param angle Rotation angle in degrees
     * @return Rotated bitmap
     */
    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun createImageFile(activity: Activity): File  {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir).apply {
            Timber.d("Image file created at: ${absolutePath}")
        }
    }
} 