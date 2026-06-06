package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.face

import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.otaliastudios.cameraview.frame.Frame
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService

class FaceAnalyzer(
    private val faceDetectionExecutor: ExecutorService,
    private val bitmapProcessingExecutor: ExecutorService,
    private val listener: (Bitmap?, Bitmap?, Bitmap?, Bitmap?, Bitmap?, Bitmap?, Map<String, List<PointF>>, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "FaceAnalyzer"
    }

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE) // Disable landmarks for speed
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.3f) // Larger minimum face size for faster detection
            .build()
    )
    
    private val nativeProcessor = NativeImageProcessor()

    /**
     * Analyze CameraView Frame
     */
    fun analyzeFrame(frame: Frame) {
        try {
            Log.d(TAG, "analyzeFrame: Starting frame analysis...")
            
            // Convert Frame to bitmap
            val bitmap = frame.toBitmap()
            Log.d(TAG, "analyzeFrame: Bitmap created: ${bitmap.width}x${bitmap.height}")
            
            val rotation = frame.rotation
            Log.d(TAG, "analyzeFrame: Rotation: $rotation")
            
            val image = InputImage.fromBitmap(bitmap, rotation)
            Log.d(TAG, "analyzeFrame: InputImage created, processing with MLKit...")
            
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "analyzeFrame: MLKit success! Found ${faces.size} faces")
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        
                        // Process bitmap operations on separate thread pool
                        bitmapProcessingExecutor.execute {
                            try {
                                // Rotate bitmap to correct orientation
                                val rotatedBitmap = when (rotation) {
                                    90 -> rotateBitmap(bitmap, 90f)
                                    180 -> rotateBitmap(bitmap, 180f)
                                    270 -> rotateBitmap(bitmap, 270f)
                                    else -> bitmap
                                }

                                var leftEyeBitmap: Bitmap? = null
                                var rightEyeBitmap: Bitmap? = null
                                var leftEyebrowBitmap: Bitmap? = null
                                var rightEyebrowBitmap: Bitmap? = null
                                var noseBitmap: Bitmap? = null
                                var mouthBitmap: Bitmap? = null
                                
                                val contours = mutableMapOf<String, List<PointF>>()

                                // Process face parts extraction in parallel
                                val extractionTasks = mutableListOf<Runnable>()
                                
                                // Extract left eye using native code
                                val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)
                                if (leftEyeContour != null && leftEyeContour.points.isNotEmpty()) {
                                    extractionTasks.add {
                                        leftEyeBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, leftEyeContour.points, "Left Eye")
                                        contours["Left Eye"] = leftEyeContour.points
                                    }
                                }

                                // Extract right eye using native code
                                val rightEyeContour = face.getContour(FaceContour.RIGHT_EYE)
                                if (rightEyeContour != null && rightEyeContour.points.isNotEmpty()) {
                                    extractionTasks.add {
                                        rightEyeBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, rightEyeContour.points, "Right Eye")
                                        contours["Right Eye"] = rightEyeContour.points
                                    }
                                }

                                // Extract left eyebrow using native code
                                val leftEyebrowTopContour = face.getContour(FaceContour.LEFT_EYEBROW_TOP)
                                val leftEyebrowBottomContour = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM)
                                if (leftEyebrowTopContour != null && leftEyebrowBottomContour != null) {
                                    extractionTasks.add {
                                        val leftEyebrowPoints = mutableListOf<PointF>()
                                        leftEyebrowPoints.addAll(leftEyebrowTopContour.points)
                                        leftEyebrowPoints.addAll(leftEyebrowBottomContour.points.reversed())
                                        if (leftEyebrowPoints.isNotEmpty()) {
                                            leftEyebrowBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, leftEyebrowPoints, "Left Eyebrow")
                                            contours["Left Eyebrow"] = leftEyebrowPoints
                                        }
                                    }
                                }

                                // Extract right eyebrow using native code
                                val rightEyebrowTopContour = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)
                                val rightEyebrowBottomContour = face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM)
                                if (rightEyebrowTopContour != null && rightEyebrowBottomContour != null) {
                                    extractionTasks.add {
                                        val rightEyebrowPoints = mutableListOf<PointF>()
                                        rightEyebrowPoints.addAll(rightEyebrowTopContour.points)
                                        rightEyebrowPoints.addAll(rightEyebrowBottomContour.points.reversed())
                                        if (rightEyebrowPoints.isNotEmpty()) {
                                            rightEyebrowBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, rightEyebrowPoints, "Right Eyebrow")
                                            contours["Right Eyebrow"] = rightEyebrowPoints
                                        }
                                    }
                                }

                                // Extract nose using native code
                                val noseBridgeContour = face.getContour(FaceContour.NOSE_BRIDGE)
                                val noseBottomContour = face.getContour(FaceContour.NOSE_BOTTOM)
                                if (noseBridgeContour != null && noseBottomContour != null) {
                                    extractionTasks.add {
                                        val nosePoints = mutableListOf<PointF>()
                                        nosePoints.addAll(noseBridgeContour.points)
                                        nosePoints.addAll(noseBottomContour.points)
                                        if (nosePoints.isNotEmpty()) {
                                            noseBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, nosePoints, "Nose")
                                            contours["Nose"] = nosePoints
                                        }
                                    }
                                }

                                // Extract mouth using native code
                                val upperLipTopContour = face.getContour(FaceContour.UPPER_LIP_TOP)
                                val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)
                                val lowerLipTopContour = face.getContour(FaceContour.LOWER_LIP_TOP)
                                val lowerLipBottomContour = face.getContour(FaceContour.LOWER_LIP_BOTTOM)
                                if (upperLipTopContour != null && lowerLipBottomContour != null) {
                                    extractionTasks.add {
                                        val mouthPoints = mutableListOf<PointF>()
                                        mouthPoints.addAll(upperLipTopContour.points)
                                        upperLipBottomContour?.let { mouthPoints.addAll(it.points) }
                                        lowerLipTopContour?.let { mouthPoints.addAll(it.points) }
                                        mouthPoints.addAll(lowerLipBottomContour.points.reversed())
                                        if (mouthPoints.isNotEmpty()) {
                                            mouthBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, mouthPoints, "Mouth")
                                            contours["Mouth"] = mouthPoints
                                        }
                                    }
                                }

                                // Execute extraction tasks in parallel for speed
                                extractionTasks.parallelStream().forEach { it.run() }
                                
                                listener(leftEyeBitmap, rightEyeBitmap, leftEyebrowBitmap, rightEyebrowBitmap, noseBitmap, mouthBitmap, contours, rotatedBitmap.width, rotatedBitmap.height)
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error extracting face parts from Frame", e)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "analyzeFrame: MLKit face detection failed!", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Frame", e)
        }
    }

    /**
     * Convert CameraView Frame to Bitmap
     */
    private fun Frame.toBitmap(): Bitmap {
        try {
            val data = this.getData<ByteArray>()
            val size = this.size
            Log.d(TAG, "Frame.toBitmap: Frame size=${size.width}x${size.height}, data length=${data.size}, format=${this.format}")
            
            // Try different formats based on actual frame format
            val format = when (this.format) {
                17 -> android.graphics.ImageFormat.NV21 // Most common
                35 -> android.graphics.ImageFormat.YUV_420_888
                else -> {
                    Log.w(TAG, "Frame.toBitmap: Unknown format ${this.format}, defaulting to NV21")
                    android.graphics.ImageFormat.NV21
                }
            }
            
            // Frame data is usually in YUV format, convert to RGB
            val yuvImage = android.graphics.YuvImage(
                data,
                format,
                size.width,
                size.height,
                null
            )
            
            val out = java.io.ByteArrayOutputStream()
            val success = yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, size.width, size.height),
                100,
                out
            )
            
            Log.d(TAG, "Frame.toBitmap: YUV compression success=$success, JPEG size=${out.size()}")
            
            val jpegData = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            
            Log.d(TAG, "Frame.toBitmap: Final bitmap: ${bitmap?.width}x${bitmap?.height}")
            return bitmap ?: throw IllegalStateException("Failed to decode bitmap")
            
        } catch (e: Exception) {
            Log.e(TAG, "Frame.toBitmap: Conversion failed!", e)
            throw e
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        
                        // Process bitmap operations on separate thread pool
                        bitmapProcessingExecutor.execute {
                            try {
                                val bitmap = imageProxy.toBitmap()
                                
                                // Rotate bitmap to correct orientation
                                val rotatedBitmap = when (rotation) {
                                    90 -> rotateBitmap(bitmap, 90f)
                                    180 -> rotateBitmap(bitmap, 180f)
                                    270 -> rotateBitmap(bitmap, 270f)
                                    else -> bitmap
                                }

                                var leftEyeBitmap: Bitmap? = null
                                var rightEyeBitmap: Bitmap? = null
                                var leftEyebrowBitmap: Bitmap? = null
                                var rightEyebrowBitmap: Bitmap? = null
                                var noseBitmap: Bitmap? = null
                                var mouthBitmap: Bitmap? = null
                                
                                val contours = mutableMapOf<String, List<PointF>>()

                                // Process face parts extraction in parallel
                                val extractionTasks = mutableListOf<Runnable>()
                                
                                // Extract left eye using native code
                                val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)
                                if (leftEyeContour != null && leftEyeContour.points.isNotEmpty()) {
                                    extractionTasks.add {
                                        leftEyeBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, leftEyeContour.points, "Left Eye")
                                        contours["Left Eye"] = leftEyeContour.points
                                    }
                                }

                                // Extract right eye using native code
                                val rightEyeContour = face.getContour(FaceContour.RIGHT_EYE)
                                if (rightEyeContour != null && rightEyeContour.points.isNotEmpty()) {
                                    extractionTasks.add {
                                        rightEyeBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, rightEyeContour.points, "Right Eye")
                                        contours["Right Eye"] = rightEyeContour.points
                                    }
                                }

                                // Extract left eyebrow using native code
                                val leftEyebrowTopContour = face.getContour(FaceContour.LEFT_EYEBROW_TOP)
                                val leftEyebrowBottomContour = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM)
                                if (leftEyebrowTopContour != null && leftEyebrowBottomContour != null) {
                                    extractionTasks.add {
                                        val leftEyebrowPoints = mutableListOf<PointF>()
                                        leftEyebrowPoints.addAll(leftEyebrowTopContour.points)
                                        leftEyebrowPoints.addAll(leftEyebrowBottomContour.points.reversed())
                                        if (leftEyebrowPoints.isNotEmpty()) {
                                            leftEyebrowBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, leftEyebrowPoints, "Left Eyebrow")
                                            contours["Left Eyebrow"] = leftEyebrowPoints
                                        }
                                    }
                                }

                                // Extract right eyebrow using native code
                                val rightEyebrowTopContour = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)
                                val rightEyebrowBottomContour = face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM)
                                if (rightEyebrowTopContour != null && rightEyebrowBottomContour != null) {
                                    extractionTasks.add {
                                        val rightEyebrowPoints = mutableListOf<PointF>()
                                        rightEyebrowPoints.addAll(rightEyebrowTopContour.points)
                                        rightEyebrowPoints.addAll(rightEyebrowBottomContour.points.reversed())
                                        if (rightEyebrowPoints.isNotEmpty()) {
                                            rightEyebrowBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, rightEyebrowPoints, "Right Eyebrow")
                                            contours["Right Eyebrow"] = rightEyebrowPoints
                                        }
                                    }
                                }

                                // Extract nose using native code
                                val noseBridgeContour = face.getContour(FaceContour.NOSE_BRIDGE)
                                val noseBottomContour = face.getContour(FaceContour.NOSE_BOTTOM)
                                if (noseBridgeContour != null && noseBottomContour != null) {
                                    extractionTasks.add {
                                        val nosePoints = mutableListOf<PointF>()
                                        nosePoints.addAll(noseBridgeContour.points)
                                        nosePoints.addAll(noseBottomContour.points)
                                        if (nosePoints.isNotEmpty()) {
                                            noseBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, nosePoints, "Nose")
                                            contours["Nose"] = nosePoints
                                        }
                                    }
                                }

                                // Extract mouth using native code
                                val upperLipTopContour = face.getContour(FaceContour.UPPER_LIP_TOP)
                                val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)
                                val lowerLipTopContour = face.getContour(FaceContour.LOWER_LIP_TOP)
                                val lowerLipBottomContour = face.getContour(FaceContour.LOWER_LIP_BOTTOM)
                                if (upperLipTopContour != null && lowerLipBottomContour != null) {
                                    extractionTasks.add {
                                        val mouthPoints = mutableListOf<PointF>()
                                        mouthPoints.addAll(upperLipTopContour.points)
                                        upperLipBottomContour?.let { mouthPoints.addAll(it.points) }
                                        lowerLipTopContour?.let { mouthPoints.addAll(it.points) }
                                        mouthPoints.addAll(lowerLipBottomContour.points.reversed())
                                        if (mouthPoints.isNotEmpty()) {
                                            mouthBitmap = nativeProcessor.extractFeatureOptimized(rotatedBitmap, mouthPoints, "Mouth")
                                            contours["Mouth"] = mouthPoints
                                        }
                                    }
                                }

                                // Execute extraction tasks in parallel for speed
                                extractionTasks.parallelStream().forEach { it.run() }
                                
                                listener(leftEyeBitmap, rightEyeBitmap, leftEyebrowBitmap, rightEyebrowBitmap, noseBitmap, mouthBitmap, contours, rotatedBitmap.width, rotatedBitmap.height)
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error extracting face parts", e)
                            } finally {
                                // Close imageProxy after bitmap processing is complete
                                imageProxy.close()
                            }
                        }
                    } else {
                        // No faces detected, close immediately
                        imageProxy.close()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}