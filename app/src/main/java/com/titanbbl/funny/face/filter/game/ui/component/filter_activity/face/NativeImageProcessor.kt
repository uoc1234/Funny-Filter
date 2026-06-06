package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.face

import android.graphics.*
import android.util.Log
import kotlin.math.max
import kotlin.math.min

class NativeImageProcessor {
    
    companion object {
        private const val TAG = "NativeImageProcessor"
    }
    
    /**
     * Extract face feature using optimized native processing
     * This is a simplified version that mimics the native C++ functionality
     * with improved transparency handling
     */
    fun extractFeatureOptimized(bitmap: Bitmap, contourPoints: List<PointF>, featureName: String): Bitmap? {
        try {
            if (contourPoints.isEmpty()) return null
            
            // Calculate bounding box from contour points
            var minX = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE
            
            for (point in contourPoints) {
                minX = min(minX, point.x)
                maxX = max(maxX, point.x)
                minY = min(minY, point.y)
                maxY = max(maxY, point.y)
            }
            
            // Add padding around the feature - increase padding for better visibility
            val padding = when (featureName) {
                "Left Eye", "Right Eye" -> 25f
                "Left Eyebrow", "Right Eyebrow" -> 20f
                "Nose" -> 30f
                "Mouth" -> 35f
                else -> 25f
            }
            
            minX = max(0f, minX - padding)
            maxX = min(bitmap.width.toFloat(), maxX + padding)
            minY = max(0f, minY - padding)
            maxY = min(bitmap.height.toFloat(), maxY + padding)
            
            val width = (maxX - minX).toInt()
            val height = (maxY - minY).toInt()
            
            // Ensure minimum dimensions for very small features
            val minWidth = 80
            val minHeight = 40
            
            val finalWidth = max(width, minWidth)
            val finalHeight = max(height, minHeight)
            
            // Adjust bounding box to meet minimum dimensions
            val widthDiff = finalWidth - width
            val heightDiff = finalHeight - height
            
            minX = max(0f, minX - (widthDiff / 2))
            maxX = min(bitmap.width.toFloat(), maxX + (widthDiff / 2))
            minY = max(0f, minY - (heightDiff / 2))
            maxY = min(bitmap.height.toFloat(), maxY + (heightDiff / 2))
            
            // Recalculate final dimensions
            val finalActualWidth = (maxX - minX).toInt()
            val finalActualHeight = (maxY - minY).toInt()
            
            if (finalActualWidth <= 0 || finalActualHeight <= 0) return null
            
            // Log the extraction parameters
            Log.d(TAG, "Extracting $featureName: Bounds: ($minX,$minY)-($maxX,$maxY), " +
                    "Size: ${finalActualWidth}x${finalActualHeight}")
            
            // Create a transparent bitmap as base
            val result = Bitmap.createBitmap(finalActualWidth, finalActualHeight, Bitmap.Config.ARGB_8888)
            result.eraseColor(Color.TRANSPARENT)
            
            // Create canvas to draw on transparent bitmap
            val canvas = Canvas(result)
            
            // Create path from contour points
            val path = Path()
            val adjustedPoints = contourPoints.map { 
                PointF(it.x - minX, it.y - minY) 
            }
            
            if (adjustedPoints.isNotEmpty()) {
                path.moveTo(adjustedPoints[0].x, adjustedPoints[0].y)
                for (i in 1 until adjustedPoints.size) {
                    path.lineTo(adjustedPoints[i].x, adjustedPoints[i].y)
                }
                path.close()
            }
            
            // Create paint for drawing the bitmap
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // Create a shader from the source bitmap
            val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            
            // Create a matrix to translate the shader
            val matrix = Matrix()
            matrix.setTranslate(-minX, -minY)
            shader.setLocalMatrix(matrix)
            
            // Set the shader to the paint
            paint.shader = shader
            
            // Draw the path with the bitmap shader
            canvas.drawPath(path, paint)
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting feature $featureName", e)
            return null
        }
    }
    
    // No need for separate processing methods anymore since we handle everything in extractFeatureOptimized
}