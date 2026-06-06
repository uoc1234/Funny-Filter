package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.face

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityFaceBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Audio
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import com.otaliastudios.cameraview.size.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FacePuzzleActivity : BaseActivity<ActivityFaceBinding>() {

    private lateinit var cameraView: CameraView
    private lateinit var leftEyeView: ImageView
    private lateinit var rightEyeView: ImageView
    private lateinit var leftEyebrowView: ImageView
    private lateinit var rightEyebrowView: ImageView
    private lateinit var noseView: ImageView
    private lateinit var mouthView: ImageView
    private lateinit var faceDetectionExecutor: ExecutorService
    private lateinit var bitmapProcessingExecutor: ExecutorService
    
    // Camera listener - định nghĩa trước
    private val cameraListener = object : CameraListener() {
        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            // VideoResult always indicates success if callback is called
            // File path is available via result.file
            Log.d(TAG, "Video recording completed: ${result.file.absolutePath}")
            runOnUiThread {
                showSuccessDialog()
            }
            isRecording = false
            runOnUiThread {
                // Update UI
                mBinding.btnRecord.setImageResource(R.drawable.ic_record)
            }
        }
        
        override fun onVideoRecordingStart() {
            super.onVideoRecordingStart()
            Log.d(TAG, "Video recording started")
        }
        
        override fun onVideoRecordingEnd() {
            super.onVideoRecordingEnd()
            Log.d(TAG, "Video recording ended")
        }
    }
    
    // Video recording variables
    private var isRecording = false
    private var outputFile: File? = null
    private var progressDialog: AlertDialog? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isProcessingFrame = false
    private var frameSkipCounter = 0
    private val FRAME_SKIP_RATE = 1 // Process every frame for better detection chance
    private var lastProcessTime = 0L
    private val MIN_PROCESS_INTERVAL = 100L // Increase to 100ms to give more time for detection
    
    // Animation variables
    private var leftEyeAnimator: ValueAnimator? = null
    private var rightEyeAnimator: ValueAnimator? = null
    private var leftEyebrowAnimator: ValueAnimator? = null
    private var rightEyebrowAnimator: ValueAnimator? = null
    private var noseAnimator: ValueAnimator? = null
    private var mouthAnimator: ValueAnimator? = null
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var cameraWidth = 0
    private var cameraHeight = 0
    
    private var leftEyeTargetX = 0f
    private var rightEyeTargetX = 0f
    private var leftEyebrowTargetX = 0f
    private var rightEyebrowTargetX = 0f
    private var noseTargetX = 0f
    private var mouthTargetX = 0f
    
    // Track last set X positions to avoid unnecessary updates
    private var leftEyeLastX = Float.MIN_VALUE
    private var rightEyeLastX = Float.MIN_VALUE
    private var leftEyebrowLastX = Float.MIN_VALUE
    private var rightEyebrowLastX = Float.MIN_VALUE
    private var noseLastX = Float.MIN_VALUE
    private var mouthLastX = Float.MIN_VALUE
    
    private var isAnimationRunning = false
    private var currentFallingIndex = 0
    private val facePartsList = listOf("Left Eye", "Right Eye", "Left Eyebrow", "Right Eyebrow", "Nose", "Mouth")
    private val stoppedParts = mutableSetOf<String>() // Track which parts are stopped and can update
    
    // Threshold for position change detection (in pixels)
    private val POSITION_CHANGE_THRESHOLD = 5f
    
    companion object {
        private const val TAG = "FacePuzzleActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        
        when {
            cameraGranted && audioGranted -> startCamera()
            cameraGranted && !audioGranted -> {
                Toast.makeText(this, "Audio permission required for recording", Toast.LENGTH_LONG).show()
                startCamera() // Still start camera without recording capability
            }
            !cameraGranted -> {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun getLayoutActivity(): Int = R.layout.activity_face

    override fun initViews() {
        // Initialize camera view - directly use CameraView from layout
        cameraView = mBinding.cameraView
        
        // Initialize face part views
        leftEyeView = mBinding.leftEyeView
        rightEyeView = mBinding.rightEyeView
        leftEyebrowView = mBinding.leftEyebrowView
        rightEyebrowView = mBinding.rightEyebrowView
        noseView = mBinding.noseView
        mouthView = mBinding.mouthView
        
        // DEBUG: Thêm placeholder để test overlay hiển thị
        leftEyeView.setBackgroundColor(android.graphics.Color.RED)
        rightEyeView.setBackgroundColor(android.graphics.Color.GREEN)
        leftEyebrowView.setBackgroundColor(android.graphics.Color.BLUE)
        rightEyebrowView.setBackgroundColor(android.graphics.Color.YELLOW)
        noseView.setBackgroundColor(android.graphics.Color.MAGENTA)
        mouthView.setBackgroundColor(android.graphics.Color.CYAN)
        
        // Đặt views ở vị trí hiển thị để test
        leftEyeView.x = 100f
        leftEyeView.y = 200f
        rightEyeView.x = 250f
        rightEyeView.y = 200f
        leftEyebrowView.x = 100f
        leftEyebrowView.y = 150f
        rightEyebrowView.x = 250f
        rightEyebrowView.y = 150f
        noseView.x = 175f
        noseView.y = 250f
        mouthView.x = 175f
        mouthView.y = 320f
        
        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        
        // Setup touch listener for camera - move to next face part only during animation
        cameraView.setOnClickListener {
            if (isAnimationRunning) {
                // If animation is running, stop current part and start next
                stopCurrentAndStartNext()
                Toast.makeText(this, "Moved to next face part!", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup record button - click to start animation + recording together
        mBinding.btnRecord.setOnClickListener {
            if (isRecording) {
                // Currently recording - stop recording
                cameraView.stopVideo()
                isRecording = false
                mBinding.btnRecord.setImageResource(R.drawable.ic_record)
            } else {
                // Start both animation and recording together
                if (!isAnimationRunning) {
                    Log.d(TAG, "Starting animation from record button")
                    startAllFalling()
                }
                startVideoRecording()
            }
        }
        
        // DEBUG: Add test button for animation (use camera flip button temporarily)
        mBinding.btnCamera?.setOnClickListener {
            Log.d(TAG, "Test button clicked - starting animation")
            if (!isAnimationRunning) {
                startAllFalling()
            } else {
                pauseAllFalling()
            }
        }
        
        // Face detection will start automatically when camera is ready
        
        // Create optimized thread pools for real-time performance
        faceDetectionExecutor = Executors.newSingleThreadExecutor() // Single thread for face detection
        bitmapProcessingExecutor = Executors.newFixedThreadPool(2) // 2 threads for faster bitmap processing

        // Check both camera and audio permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            // Request both permissions at once
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ))
        }
    }

    private fun startCamera() {
        // Setup CameraView theo pattern của TimeDelayVerticalActivity
        cameraView.setLifecycleOwner(this)
        cameraView.facing = Facing.FRONT
        cameraView.mode = Mode.VIDEO
        cameraView.audio = Audio.ON
        
        // Add gesture mapping for tap to focus (như TimeDelayVerticalActivity)
        cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)
        
        // Add camera listener
        cameraView.addCameraListener(cameraListener)
        
                // Add frame processor for face detection - Fix the issue
        cameraView.addFrameProcessor(object : com.otaliastudios.cameraview.frame.FrameProcessor {
            override fun process(frame: com.otaliastudios.cameraview.frame.Frame) {
                val currentTime = System.currentTimeMillis()
                
                // Frame skipping and time-based throttling for better performance
                frameSkipCounter++
                if (frameSkipCounter % FRAME_SKIP_RATE != 0 || 
                    currentTime - lastProcessTime < MIN_PROCESS_INTERVAL) {
                        return
                    }
                
                if (!isProcessingFrame) {
                    isProcessingFrame = true
                    lastProcessTime = currentTime
                    cameraWidth = frame.size.width
                    cameraHeight = frame.size.height
                    
                    Log.d(TAG, "Processing frame for face detection... Frame: ${frame.size.width}x${frame.size.height}")
                    
                    // Extract frame data SYNCHRONOUSLY before moving to background thread
                    try {
                        val frameData = frame.getData<ByteArray>().clone() // Clone to avoid reference issues
                        val frameSize = frame.size
                        val frameRotation = frame.rotationToUser
                        val frameFormat = frame.format
                        
                        Log.d(TAG, "Frame data extracted: ${frameData.size} bytes, ${frameSize.width}x${frameSize.height}, format=$frameFormat, rotation=$frameRotation")
                        
                        // Now process on background thread with copied data
                        faceDetectionExecutor.execute {
                            try {
                                processFrameDataForFaceDetection(frameData, frameSize, frameRotation, frameFormat)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing frame data", e)
                                isProcessingFrame = false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting frame data", e)
                        isProcessingFrame = false
                    }
                }
            }
        })
        
        // Mở camera nếu chưa mở (theo pattern TimeDelayVerticalActivity)
        if (!cameraView.isOpened) {
            cameraView.open()
        }
        
        Log.d(TAG, "CameraView started successfully with overlay support")
    }
    
    /**
     * Process Frame data for face detection (with copied data to avoid Frame access issues)
     */
    private fun processFrameDataForFaceDetection(
        data: ByteArray, 
        size: Size, 
        rotation: Int, 
        format: Int
    ) {
        try {
            Log.d(TAG, "processFrameDataForFaceDetection: Processing ${size.width}x${size.height}, format=$format, rotation=$rotation")
            
            // Debug: First try to create bitmap to verify data integrity
            val testBitmap = convertDataToBitmap(data, size, format)
            if (testBitmap == null) {
                Log.e(TAG, "processFrameDataForFaceDetection: Failed to create bitmap from frame data!")
                isProcessingFrame = false
                return
            }
            Log.d(TAG, "processFrameDataForFaceDetection: Bitmap created successfully ${testBitmap.width}x${testBitmap.height}")
            
            // Convert frame data to InputImage for MLKit
            // Try different approaches based on format
            val image = when (format) {
                17 -> { // ImageFormat.YUV_420_888 = 17
                    Log.d(TAG, "processFrameDataForFaceDetection: Using YUV_420_888 format")
                    // Try using rotation for better detection
                    com.google.mlkit.vision.common.InputImage.fromByteArray(
                        data,
                        size.width,
                        size.height,
                        rotation, // Use actual rotation instead of 0
                        com.google.mlkit.vision.common.InputImage.IMAGE_FORMAT_YV12
                    )
                }
                android.graphics.ImageFormat.NV21 -> {
                    Log.d(TAG, "processFrameDataForFaceDetection: Using NV21 format")
                    com.google.mlkit.vision.common.InputImage.fromByteArray(
                        data,
                        size.width,
                        size.height,
                        rotation, // Use actual rotation
                        com.google.mlkit.vision.common.InputImage.IMAGE_FORMAT_NV21
                    )
                }
                else -> {
                    Log.w(TAG, "processFrameDataForFaceDetection: Unknown format $format, trying as NV21")
                    com.google.mlkit.vision.common.InputImage.fromByteArray(
                        data,
                        size.width,
                        size.height,
                        rotation, // Use actual rotation
                        com.google.mlkit.vision.common.InputImage.IMAGE_FORMAT_NV21
                    )
                }
            }
            
            // Create face detector with more sensitive settings
            val faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient(
                com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                    .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Change to ACCURATE for better detection
                    .setLandmarkMode(com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_ALL) // Enable landmarks
                    .setContourMode(com.google.mlkit.vision.face.FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .setClassificationMode(com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Enable classification
                    .setMinFaceSize(0.1f) // Smaller min face size for better detection
                    .enableTracking() // Enable face tracking
                    .build()
            )
            
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "processFrameDataForFaceDetection: MLKit success! Found ${faces.size} faces")
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        Log.d(TAG, "processFrameDataForFaceDetection: Face detected - bounds: ${face.boundingBox}, head rotation: ${face.headEulerAngleY}")
                        
                        // Process face parts on background thread
                        bitmapProcessingExecutor.execute {
                            try {
                                val bitmap = convertDataToBitmap(data, size, format)
                                Log.d(TAG, "processFrameDataForFaceDetection: Bitmap created: ${bitmap?.width}x${bitmap?.height}")
                                
                                if (bitmap != null) {
                                    // Apply rotation correction to bitmap based on camera orientation
                                    val correctedBitmap = rotateBitmapIfNeeded(bitmap, rotation)
                                    Log.d(TAG, "processFrameDataForFaceDetection: Corrected bitmap: ${correctedBitmap.width}x${correctedBitmap.height}")
                                    
                                    // Extract face parts from corrected bitmap
                                    extractFacePartsFromBitmap(correctedBitmap, face, rotation)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error extracting face parts", e)
                            } finally {
                                isProcessingFrame = false
                            }
                        }
                    } else {
                        Log.w(TAG, "processFrameDataForFaceDetection: No faces detected - checking bitmap quality")
                        // Try alternative approach: Use bitmap-based detection
                        bitmapProcessingExecutor.execute {
                            try {
                                tryBitmapBasedDetection(data, size, format, rotation)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in bitmap-based detection", e)
                            } finally {
                                isProcessingFrame = false
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "processFrameDataForFaceDetection: MLKit face detection failed!", e)
                    // Try bitmap-based detection as fallback
                    bitmapProcessingExecutor.execute {
                        try {
                            tryBitmapBasedDetection(data, size, format, rotation)
                        } catch (e2: Exception) {
                            Log.e(TAG, "Error in fallback bitmap detection", e2)
                        } finally {
                            isProcessingFrame = false
                        }
                    }
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error in processFrameDataForFaceDetection", e)
            isProcessingFrame = false
        }
    }
    
    /**
     * Try bitmap-based face detection as fallback
     */
    private fun tryBitmapBasedDetection(data: ByteArray, size: Size, format: Int, rotation: Int) {
        try {
            Log.d(TAG, "tryBitmapBasedDetection: Attempting bitmap-based detection")
            
            val bitmap = convertDataToBitmap(data, size, format)
            if (bitmap == null) {
                Log.e(TAG, "tryBitmapBasedDetection: Could not create bitmap")
                return
            }
            
            // Apply rotation to bitmap
            val correctedBitmap = rotateBitmapIfNeeded(bitmap, rotation)
            Log.d(TAG, "tryBitmapBasedDetection: Created corrected bitmap ${correctedBitmap.width}x${correctedBitmap.height}")
            
            // Create InputImage from bitmap instead of raw data
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(correctedBitmap, 0)
            
            // Create face detector with even more sensitive settings
            val faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient(
                com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                    .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setContourMode(com.google.mlkit.vision.face.FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .setClassificationMode(com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setMinFaceSize(0.05f) // Even smaller minimum face size
                    .enableTracking()
                    .build()
            )
            
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "tryBitmapBasedDetection: Found ${faces.size} faces using bitmap approach")
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        Log.d(TAG, "tryBitmapBasedDetection: Face detected via bitmap - bounds: ${face.boundingBox}")
                        extractFacePartsFromBitmap(correctedBitmap, face, rotation)
                    } else {
                        Log.w(TAG, "tryBitmapBasedDetection: Still no faces found in bitmap")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "tryBitmapBasedDetection: Bitmap detection also failed", e)
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "tryBitmapBasedDetection: Exception in bitmap detection", e)
        }
    }
    
    /**
     * Rotate bitmap if needed based on camera rotation
     */
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, rotation: Int): Bitmap {
        // For front camera, we need different rotation handling
        val rotationAngle = when {
            cameraView.facing == Facing.FRONT -> {
                when (rotation) {
                    90 -> 270f  // Front camera mirror effect
                    270 -> 90f
                    180 -> 180f
                    else -> 0f
                }
            }
            else -> {
                when (rotation) {
                    90 -> 90f
                    180 -> 180f
                    270 -> 270f
                    else -> 0f
                }
            }
        }
        
        Log.d(TAG, "rotateBitmapIfNeeded: rotation=$rotation, rotationAngle=$rotationAngle, facing=${cameraView.facing}")
        
        return if (rotationAngle != 0f) {
            rotateBitmap(bitmap, rotationAngle)
        } else {
            bitmap
        }
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Convert Frame data to Bitmap with better format handling
     */
    private fun convertDataToBitmap(
        data: ByteArray, 
        size: Size, 
        format: Int
    ): Bitmap? {
        return try {
            Log.d(TAG, "convertDataToBitmap: Converting ${size.width}x${size.height}, format=$format, data size=${data.size}")
            
            // Handle different formats more carefully
            val imageFormat = when (format) {
                17 -> { // ImageFormat.YUV_420_888
                    Log.d(TAG, "convertDataToBitmap: Using YUV_420_888 format")
                    android.graphics.ImageFormat.NV21
                }
                android.graphics.ImageFormat.NV21 -> {
                    Log.d(TAG, "convertDataToBitmap: Using NV21 format")
                    android.graphics.ImageFormat.NV21
                }
                else -> {
                    Log.d(TAG, "convertDataToBitmap: Unknown format $format, defaulting to NV21")
                    android.graphics.ImageFormat.NV21
                }
            }
            
            // Try different quality settings for better compatibility
            val qualityLevels = arrayOf(100, 90, 80, 70) // Try higher quality first
            
            for (quality in qualityLevels) {
                try {
                    val yuvImage = android.graphics.YuvImage(
                        data,
                        imageFormat,
                        size.width,
                        size.height,
                        null
                    )
                    
                    val out = java.io.ByteArrayOutputStream()
                    val success = yuvImage.compressToJpeg(
                        android.graphics.Rect(0, 0, size.width, size.height),
                        quality,
                        out
                    )
                    
                    if (success) {
                        val jpegData = out.toByteArray()
                        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
                        if (bitmap != null) {
                            Log.d(TAG, "convertDataToBitmap: Successfully created bitmap ${bitmap.width}x${bitmap.height} with quality $quality")
                            return bitmap
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "convertDataToBitmap: Failed with quality $quality", e)
                    continue
                }
            }
            
            Log.e(TAG, "convertDataToBitmap: All quality levels failed")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "convertDataToBitmap: Conversion failed!", e)
            null
        }
    }
    
    /**
     * Extract face parts from bitmap using face contours
     */
    private fun extractFacePartsFromBitmap(bitmap: Bitmap, face: com.google.mlkit.vision.face.Face, rotation: Int) {
        try {
            val contours = mutableMapOf<String, List<PointF>>()
            
            // Extract left eye
            val leftEyeBitmap = extractFacePart(bitmap, face.getContour(com.google.mlkit.vision.face.FaceContour.LEFT_EYE), "Left Eye", rotation)
            leftEyeBitmap?.let { contours["Left Eye"] = face.getContour(com.google.mlkit.vision.face.FaceContour.LEFT_EYE)?.points ?: emptyList() }
            
            // Extract right eye
            val rightEyeBitmap = extractFacePart(bitmap, face.getContour(com.google.mlkit.vision.face.FaceContour.RIGHT_EYE), "Right Eye", rotation)
            rightEyeBitmap?.let { contours["Right Eye"] = face.getContour(com.google.mlkit.vision.face.FaceContour.RIGHT_EYE)?.points ?: emptyList() }
            
            // Extract left eyebrow (combine top and bottom)
            val leftEyebrowBitmap = extractEyebrow(bitmap, 
                face.getContour(com.google.mlkit.vision.face.FaceContour.LEFT_EYEBROW_TOP),
                face.getContour(com.google.mlkit.vision.face.FaceContour.LEFT_EYEBROW_BOTTOM),
                "Left Eyebrow",
                rotation
            )
            leftEyebrowBitmap?.let { 
                val leftEyebrowPoints = mutableListOf<PointF>()
                face.getContour(com.google.mlkit.vision.face.FaceContour.LEFT_EYEBROW_TOP)?.points?.let { leftEyebrowPoints.addAll(it) }
                face.getContour(com.google.mlkit.vision.face.FaceContour.LEFT_EYEBROW_BOTTOM)?.points?.let { leftEyebrowPoints.addAll(it.reversed()) }
                contours["Left Eyebrow"] = leftEyebrowPoints
            }
            
            // Extract right eyebrow (combine top and bottom)
            val rightEyebrowBitmap = extractEyebrow(bitmap,
                face.getContour(com.google.mlkit.vision.face.FaceContour.RIGHT_EYEBROW_TOP),
                face.getContour(com.google.mlkit.vision.face.FaceContour.RIGHT_EYEBROW_BOTTOM),
                "Right Eyebrow",
                rotation
            )
            rightEyebrowBitmap?.let {
                val rightEyebrowPoints = mutableListOf<PointF>()
                face.getContour(com.google.mlkit.vision.face.FaceContour.RIGHT_EYEBROW_TOP)?.points?.let { rightEyebrowPoints.addAll(it) }
                face.getContour(com.google.mlkit.vision.face.FaceContour.RIGHT_EYEBROW_BOTTOM)?.points?.let { rightEyebrowPoints.addAll(it.reversed()) }
                contours["Right Eyebrow"] = rightEyebrowPoints
            }
            
            // Extract nose (combine bridge and bottom)
            val noseBitmap = extractNose(bitmap,
                face.getContour(com.google.mlkit.vision.face.FaceContour.NOSE_BRIDGE),
                face.getContour(com.google.mlkit.vision.face.FaceContour.NOSE_BOTTOM),
                "Nose",
                rotation
            )
            noseBitmap?.let {
                val nosePoints = mutableListOf<PointF>()
                face.getContour(com.google.mlkit.vision.face.FaceContour.NOSE_BRIDGE)?.points?.let { nosePoints.addAll(it) }
                face.getContour(com.google.mlkit.vision.face.FaceContour.NOSE_BOTTOM)?.points?.let { nosePoints.addAll(it) }
                contours["Nose"] = nosePoints
            }
            
            // Extract mouth (combine all lip contours)
            val mouthBitmap = extractMouth(bitmap,
                face.getContour(com.google.mlkit.vision.face.FaceContour.UPPER_LIP_TOP),
                face.getContour(com.google.mlkit.vision.face.FaceContour.UPPER_LIP_BOTTOM),
                face.getContour(com.google.mlkit.vision.face.FaceContour.LOWER_LIP_TOP),
                face.getContour(com.google.mlkit.vision.face.FaceContour.LOWER_LIP_BOTTOM),
                "Mouth",
                rotation
            )
            mouthBitmap?.let {
                val mouthPoints = mutableListOf<PointF>()
                face.getContour(com.google.mlkit.vision.face.FaceContour.UPPER_LIP_TOP)?.points?.let { mouthPoints.addAll(it) }
                face.getContour(com.google.mlkit.vision.face.FaceContour.UPPER_LIP_BOTTOM)?.points?.let { mouthPoints.addAll(it) }
                face.getContour(com.google.mlkit.vision.face.FaceContour.LOWER_LIP_TOP)?.points?.let { mouthPoints.addAll(it) }
                face.getContour(com.google.mlkit.vision.face.FaceContour.LOWER_LIP_BOTTOM)?.points?.let { mouthPoints.addAll(it.reversed()) }
                contours["Mouth"] = mouthPoints
            }
            
            Log.d(TAG, "extractFacePartsFromBitmap: Parts extracted - leftEye=${leftEyeBitmap != null}, rightEye=${rightEyeBitmap != null}, nose=${noseBitmap != null}, mouth=${mouthBitmap != null}")
            
            // Update UI on main thread
            runOnUiThread {
                updateFaceParts(leftEyeBitmap, rightEyeBitmap, leftEyebrowBitmap, rightEyebrowBitmap, noseBitmap, mouthBitmap, contours)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractFacePartsFromBitmap", e)
        }
    }
    
    /**
     * Extract single face part using contour
     */
    private fun extractFacePart(bitmap: Bitmap, contour: com.google.mlkit.vision.face.FaceContour?, partName: String, rotation: Int): Bitmap? {
        if (contour == null || contour.points.isEmpty()) return null
        
        return try {
            val points = contour.points
            
            // Calculate bounding box
            var minX = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE
            
            for (point in points) {
                minX = kotlin.math.min(minX, point.x)
                maxX = kotlin.math.max(maxX, point.x)
                minY = kotlin.math.min(minY, point.y)
                maxY = kotlin.math.max(maxY, point.y)
            }
            
            // Add padding
            val padding = 20f
            minX = kotlin.math.max(0f, minX - padding)
            maxX = kotlin.math.min(bitmap.width.toFloat(), maxX + padding)
            minY = kotlin.math.max(0f, minY - padding)
            maxY = kotlin.math.min(bitmap.height.toFloat(), maxY + padding)
            
            val width = (maxX - minX).toInt()
            val height = (maxY - minY).toInt()
            
            // Ensure minimum size
            if (width > 20 && height > 20) {
                Bitmap.createBitmap(bitmap, minX.toInt(), minY.toInt(), width, height)
            } else {
                Log.w(TAG, "extractFacePart: $partName too small: ${width}x${height}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractFacePart: Error extracting $partName", e)
            null
        }
    }
    
    /**
     * Extract eyebrow from top and bottom contours
     */
    private fun extractEyebrow(bitmap: Bitmap, topContour: com.google.mlkit.vision.face.FaceContour?, bottomContour: com.google.mlkit.vision.face.FaceContour?, partName: String, rotation: Int): Bitmap? {
        if (topContour == null && bottomContour == null) return null
        
        val allPoints = mutableListOf<PointF>()
        topContour?.points?.let { allPoints.addAll(it) }
        bottomContour?.points?.let { allPoints.addAll(it) }
        
        if (allPoints.isEmpty()) return null
        
        return extractFacePartFromPoints(bitmap, allPoints, partName, 15f, rotation)
    }
    
    /**
     * Extract nose from bridge and bottom contours
     */
    private fun extractNose(bitmap: Bitmap, bridgeContour: com.google.mlkit.vision.face.FaceContour?, bottomContour: com.google.mlkit.vision.face.FaceContour?, partName: String, rotation: Int): Bitmap? {
        if (bridgeContour == null && bottomContour == null) return null
        
        val allPoints = mutableListOf<PointF>()
        bridgeContour?.points?.let { allPoints.addAll(it) }
        bottomContour?.points?.let { allPoints.addAll(it) }
        
        if (allPoints.isEmpty()) return null
        
        return extractFacePartFromPoints(bitmap, allPoints, partName, 25f, rotation)
    }
    
    /**
     * Extract mouth from all lip contours
     */
    private fun extractMouth(bitmap: Bitmap, upperTop: com.google.mlkit.vision.face.FaceContour?, upperBottom: com.google.mlkit.vision.face.FaceContour?, 
                           lowerTop: com.google.mlkit.vision.face.FaceContour?, lowerBottom: com.google.mlkit.vision.face.FaceContour?, partName: String, rotation: Int): Bitmap? {
        val allPoints = mutableListOf<PointF>()
        upperTop?.points?.let { allPoints.addAll(it) }
        upperBottom?.points?.let { allPoints.addAll(it) }
        lowerTop?.points?.let { allPoints.addAll(it) }
        lowerBottom?.points?.let { allPoints.addAll(it) }
        
        if (allPoints.isEmpty()) return null
        
        return extractFacePartFromPoints(bitmap, allPoints, partName, 30f, rotation)
    }
    
    /**
     * Extract face part from list of points
     */
    private fun extractFacePartFromPoints(bitmap: Bitmap, points: List<PointF>, partName: String, padding: Float, rotation: Int): Bitmap? {
        if (points.isEmpty()) return null
        
        return try {
            // Calculate bounding box
            var minX = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE
            
            for (point in points) {
                minX = kotlin.math.min(minX, point.x)
                maxX = kotlin.math.max(maxX, point.x)
                minY = kotlin.math.min(minY, point.y)
                maxY = kotlin.math.max(maxY, point.y)
            }
            
            // Add padding
            minX = kotlin.math.max(0f, minX - padding)
            maxX = kotlin.math.min(bitmap.width.toFloat(), maxX + padding)
            minY = kotlin.math.max(0f, minY - padding)
            maxY = kotlin.math.min(bitmap.height.toFloat(), maxY + padding)
            
            val width = (maxX - minX).toInt()
            val height = (maxY - minY).toInt()
            
            // Ensure minimum size
            if (width > 30 && height > 20) {
                Bitmap.createBitmap(bitmap, minX.toInt(), minY.toInt(), width, height)
            } else {
                Log.w(TAG, "extractFacePartFromPoints: $partName too small: ${width}x${height}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractFacePartFromPoints: Error extracting $partName", e)
            null
        }
    }

    /**
     * Update face parts with bitmaps and contours
     */
    private fun updateFaceParts(
        leftEyeBitmap: Bitmap?,
        rightEyeBitmap: Bitmap?,
        leftEyebrowBitmap: Bitmap?,
        rightEyebrowBitmap: Bitmap?,
        noseBitmap: Bitmap?,
        mouthBitmap: Bitmap?,
        contours: Map<String, List<PointF>>
    ) {
        // Update bitmaps
        leftEyeBitmap?.let { 
            leftEyeView.setImageBitmap(it)
            leftEyeView.visibility = View.VISIBLE
        }
        rightEyeBitmap?.let { 
            rightEyeView.setImageBitmap(it)
            rightEyeView.visibility = View.VISIBLE
        }
        leftEyebrowBitmap?.let { 
            leftEyebrowView.setImageBitmap(it)
            leftEyebrowView.visibility = View.VISIBLE
        }
        rightEyebrowBitmap?.let { 
            rightEyebrowView.setImageBitmap(it)
            rightEyebrowView.visibility = View.VISIBLE
        }
        noseBitmap?.let { 
            noseView.setImageBitmap(it)
            noseView.visibility = View.VISIBLE
        }
        mouthBitmap?.let { 
            mouthView.setImageBitmap(it)
            mouthView.visibility = View.VISIBLE
        }

        // Update positions using contours
        contours["Left Eye"]?.let { updateFacePartPosition(it, "leftEye") }
        contours["Right Eye"]?.let { updateFacePartPosition(it, "rightEye") }
        contours["Left Eyebrow"]?.let { updateFacePartPosition(it, "leftEyebrow") }
        contours["Right Eyebrow"]?.let { updateFacePartPosition(it, "rightEyebrow") }
        contours["Nose"]?.let { updateFacePartPosition(it, "nose") }
        contours["Mouth"]?.let { updateFacePartPosition(it, "mouth") }
    }

    /**
     * Start video recording using CameraView - records camera + overlay together
     */
    private fun startVideoRecording() {
        if (isRecording) return
        
        // Check audio permission for recording
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permission required for recording", Toast.LENGTH_SHORT).show()
            return
        }

        isRecording = true
        
        // Create output file
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "FacePuzzle")
        if (!videoDir.exists()) videoDir.mkdirs()
        
        outputFile = File(videoDir, "face_puzzle_$timeStamp.mp4")
        
        // Start video recording with CameraView - includes overlay automatically
        // Sử dụng takeVideoSnapshot như TimeDelayVerticalActivity để đảm bảo overlay được ghi
        Log.d(TAG, "Starting video recording to: ${outputFile!!.absolutePath}")
        Log.d(TAG, "Overlay views visible? leftEye=${leftEyeView.visibility == View.VISIBLE}, rightEye=${rightEyeView.visibility == View.VISIBLE}")
        
        cameraView.takeVideoSnapshot(outputFile!!)
        
        // Update UI
        mBinding.btnRecord.setImageResource(R.drawable.ic_record_stop)
        Toast.makeText(this, "🔴 Recording camera + face parts animation to MP4...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Started recording with CameraView - overlay included automatically")
    }
    

    
    /**
     * Show success dialog
     */
    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("✅ Success!")
            .setMessage("MP4 video has been saved successfully!\n\nYou can find it in your FacePuzzle folder.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Play Again") { dialog, _ ->
                dialog.dismiss()
                // Reset everything for new session
                resetForNewSession()
            }
            .show()
    }
    
    /**
     * Reset for new session
     */
    private fun resetForNewSession() {
        stopAllAnimations()
        isAnimationRunning = false
        stoppedParts.clear()
        currentFallingIndex = 0
        
        // Reset button icon
        mBinding.btnRecord.setImageResource(R.drawable.ic_record)
        
        Toast.makeText(this, "Ready for new recording!", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Hide progress dialog
     */
    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }




    private fun updateFacePartPosition(contour: List<PointF>?, partType: String) {
        if (contour == null || contour.isEmpty()) return

        // Calculate center of contour
        var centerX = 0f
        var centerY = 0f
        contour.forEach { point ->
            centerX += point.x
            centerY += point.y
        }
        centerX /= contour.size
        centerY /= contour.size

        // Convert camera coordinates to screen coordinates
        val scaleX = screenWidth.toFloat() / cameraWidth
        val scaleY = screenHeight.toFloat() / cameraHeight

        // For front camera with 270° rotation, we need to adjust coordinates
        val screenX = when (cameraView.facing) {
            Facing.FRONT -> {
                // Front camera: Mirror X coordinate
                (cameraWidth - centerX) * scaleX
            }
            else -> {
                // Back camera: Normal X coordinate
                centerX * scaleX
            }
        }

        // Fix Y coordinate inversion by using centerY * scaleY instead of (cameraHeight - centerY) * scaleY
        val screenY = centerY * scaleY

        Log.d(TAG, "updateFacePartPosition: $partType - centerX=$centerX, centerY=$centerY, screenX=$screenX, screenY=$screenY")
        Log.d(TAG, "updateFacePartPosition: Camera ${cameraWidth}x${cameraHeight}, Screen ${screenWidth}x${screenHeight}, Facing=${cameraView.facing}")

        // Update target position only if there's significant change
        when (partType) {
            "leftEye" -> {
                val newTargetX = screenX - leftEyeView.width / 2
                if (kotlin.math.abs(newTargetX - leftEyeTargetX) > POSITION_CHANGE_THRESHOLD) {
                    leftEyeTargetX = newTargetX
                    if (leftEyeAnimator?.isRunning != true) {
                        leftEyeView.x = leftEyeTargetX
                        leftEyeView.y = screenY - leftEyeView.height / 2
                        leftEyeLastX = leftEyeTargetX
                    }
                }
            }
            "rightEye" -> {
                val newTargetX = screenX - rightEyeView.width / 2
                if (kotlin.math.abs(newTargetX - rightEyeTargetX) > POSITION_CHANGE_THRESHOLD) {
                    rightEyeTargetX = newTargetX
                    if (rightEyeAnimator?.isRunning != true) {
                        rightEyeView.x = rightEyeTargetX
                        rightEyeView.y = screenY - rightEyeView.height / 2
                        rightEyeLastX = rightEyeTargetX
                    }
                }
            }
            "leftEyebrow" -> {
                val newTargetX = screenX - leftEyebrowView.width / 2
                if (kotlin.math.abs(newTargetX - leftEyebrowTargetX) > POSITION_CHANGE_THRESHOLD) {
                    leftEyebrowTargetX = newTargetX
                    if (leftEyebrowAnimator?.isRunning != true) {
                        leftEyebrowView.x = leftEyebrowTargetX
                        leftEyebrowView.y = screenY - leftEyebrowView.height / 2
                        leftEyebrowLastX = leftEyebrowTargetX
                    }
                }
            }
            "rightEyebrow" -> {
                val newTargetX = screenX - rightEyebrowView.width / 2
                if (kotlin.math.abs(newTargetX - rightEyebrowTargetX) > POSITION_CHANGE_THRESHOLD) {
                    rightEyebrowTargetX = newTargetX
                    if (rightEyebrowAnimator?.isRunning != true) {
                        rightEyebrowView.x = rightEyebrowTargetX
                        rightEyebrowView.y = screenY - rightEyebrowView.height / 2
                        rightEyebrowLastX = rightEyebrowTargetX
                    }
                }
            }
            "nose" -> {
                val newTargetX = screenX - noseView.width / 2
                if (kotlin.math.abs(newTargetX - noseTargetX) > POSITION_CHANGE_THRESHOLD) {
                    noseTargetX = newTargetX
                    if (noseAnimator?.isRunning != true) {
                        noseView.x = noseTargetX
                        noseView.y = screenY - noseView.height / 2
                        noseLastX = noseTargetX
                    }
                }
            }
            "mouth" -> {
                val newTargetX = screenX - mouthView.width / 2
                if (kotlin.math.abs(newTargetX - mouthTargetX) > POSITION_CHANGE_THRESHOLD) {
                    mouthTargetX = newTargetX
                    if (mouthAnimator?.isRunning != true) {
                        mouthView.x = mouthTargetX
                        mouthView.y = screenY - mouthView.height / 2
                        mouthLastX = mouthTargetX
                    }
                }
            }
        }
    }

    private fun startAllFalling() {
        // DEBUG: Log view visibility and status
        Log.d(TAG, "Starting animation - Views status:")
        Log.d(TAG, "leftEye: drawable=${leftEyeView.drawable != null}, x=${leftEyeView.x}, y=${leftEyeView.y}")
        Log.d(TAG, "rightEye: drawable=${rightEyeView.drawable != null}, x=${rightEyeView.x}, y=${rightEyeView.y}")
        Log.d(TAG, "nose: drawable=${noseView.drawable != null}, x=${noseView.x}, y=${noseView.y}")
        Log.d(TAG, "mouth: drawable=${mouthView.drawable != null}, x=${mouthView.x}, y=${mouthView.y}")
        
        // Allow animation to start regardless of face detection status
        // Animation will work with placeholder colors until real face parts are detected
        
        // Stop any existing animations and reset state
        stopAllAnimations()
        stoppedParts.clear()
        
        // Set animation running flag and reset index
        isAnimationRunning = true
        currentFallingIndex = 0
        
        // Start the first available face part
        startNextFacePart()
        
        Toast.makeText(this, "🎭 Face parts falling! Tap camera to control", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Started sequential face parts falling animation")
    }
    
    private fun startNextFacePart() {
        // Find next available face part to animate
        while (currentFallingIndex < facePartsList.size) {
            val partName = facePartsList[currentFallingIndex]
            val (view, targetX) = when (partName) {
                "Left Eye" -> Pair(leftEyeView, if (leftEyeTargetX != 0f) leftEyeTargetX else screenWidth * 0.3f)
                "Right Eye" -> Pair(rightEyeView, if (rightEyeTargetX != 0f) rightEyeTargetX else screenWidth * 0.7f)
                "Left Eyebrow" -> Pair(leftEyebrowView, if (leftEyebrowTargetX != 0f) leftEyebrowTargetX else screenWidth * 0.25f)
                "Right Eyebrow" -> Pair(rightEyebrowView, if (rightEyebrowTargetX != 0f) rightEyebrowTargetX else screenWidth * 0.75f)
                "Nose" -> Pair(noseView, if (noseTargetX != 0f) noseTargetX else screenWidth * 0.5f)
                "Mouth" -> Pair(mouthView, if (mouthTargetX != 0f) mouthTargetX else screenWidth * 0.5f)
                else -> Pair(null, 0f)
            }
            
            if (view != null) {
                // Start animation - use detected position or default fallback
                startFacePartFalling(view, targetX, partName)
                break
            } else {
                // Skip this part if view is null, move to next
                currentFallingIndex++
            }
        }
        
        // If we've gone through all parts, complete the sequence
        if (currentFallingIndex >= facePartsList.size) {
            completeSequentialFalling()
        }
    }
    
    private fun startFacePartFalling(view: ImageView, targetX: Float, partName: String) {
        // Position part at top of screen with correct X coordinate
        view.x = targetX
        view.y = -view.height.toFloat()
        view.alpha = 1f
        
        // Store initial X position to track changes
        when (partName) {
            "Left Eye" -> leftEyeLastX = targetX
            "Right Eye" -> rightEyeLastX = targetX
            "Left Eyebrow" -> leftEyebrowLastX = targetX
            "Right Eyebrow" -> rightEyebrowLastX = targetX
            "Nose" -> noseLastX = targetX
            "Mouth" -> mouthLastX = targetX
        }
        
        // Create falling animation with optimized frame rate
        val animator = ValueAnimator.ofFloat(-view.height.toFloat(), screenHeight.toFloat())
        animator.duration = 8000L // 8 seconds to fall (slower)
        
        // Optimized animation updates - only update Y position in animator
        animator.addUpdateListener { animation ->
            val currentY = animation.animatedValue as Float
            view.y = currentY
            
            // X position is updated separately in updateFacePartPosition() only when needed
            // This reduces the frequency of X updates and prevents jitter
            
            // Stop if reached bottom - trigger next part
            if (currentY >= screenHeight - view.height) {
                onFacePartReachedBottom(partName)
            }
        }
        
        // Store animator reference
        when (partName) {
            "Left Eye" -> leftEyeAnimator = animator
            "Right Eye" -> rightEyeAnimator = animator
            "Left Eyebrow" -> leftEyebrowAnimator = animator
            "Right Eyebrow" -> rightEyebrowAnimator = animator
            "Nose" -> noseAnimator = animator
            "Mouth" -> mouthAnimator = animator
        }
        
        animator.start()
        Toast.makeText(this, "$partName is falling!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Started $partName falling animation (index: $currentFallingIndex)")
    }
    
    private fun onFacePartReachedBottom(partName: String) {
        Log.d(TAG, "$partName reached bottom, starting next part")
        
        // Stop current animation
        when (partName) {
            "Left Eye" -> { leftEyeAnimator?.cancel(); leftEyeAnimator = null }
            "Right Eye" -> { rightEyeAnimator?.cancel(); rightEyeAnimator = null }
            "Left Eyebrow" -> { leftEyebrowAnimator?.cancel(); leftEyebrowAnimator = null }
            "Right Eyebrow" -> { rightEyebrowAnimator?.cancel(); rightEyebrowAnimator = null }
            "Nose" -> { noseAnimator?.cancel(); noseAnimator = null }
            "Mouth" -> { mouthAnimator?.cancel(); mouthAnimator = null }
        }
        
        // Move to next part
        currentFallingIndex++
        startNextFacePart()
    }
    
    private fun completeSequentialFalling() {
        isAnimationRunning = false
        currentFallingIndex = 0
        stoppedParts.clear()
        Toast.makeText(this, "All face parts completed falling!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Sequential falling animation completed - full bitmap updates resumed")
    }
    
    private fun stopCurrentAndStartNext() {
        if (currentFallingIndex < facePartsList.size) {
            val currentPartName = facePartsList[currentFallingIndex]
            
            // Stop current animation and mark as stopped (can update bitmap)
            when (currentPartName) {
                "Left Eye" -> { leftEyeAnimator?.cancel(); leftEyeAnimator = null }
                "Right Eye" -> { rightEyeAnimator?.cancel(); rightEyeAnimator = null }
                "Left Eyebrow" -> { leftEyebrowAnimator?.cancel(); leftEyebrowAnimator = null }
                "Right Eyebrow" -> { rightEyebrowAnimator?.cancel(); rightEyebrowAnimator = null }
                "Nose" -> { noseAnimator?.cancel(); noseAnimator = null }
                "Mouth" -> { mouthAnimator?.cancel(); mouthAnimator = null }
            }
            
            // Add to stopped parts so it can update bitmap
            stoppedParts.add(currentPartName)
            
            Toast.makeText(this, "$currentPartName stopped! Starting next...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "$currentPartName stopped at current position, added to stopped parts")
            
            // Move to next part
            currentFallingIndex++
            startNextFacePart()
        } else {
            // No more parts, complete sequence
            completeSequentialFalling()
        }
    }
    
    private fun pauseAllFalling() {
        stopAllAnimations()
        isAnimationRunning = false
        stoppedParts.clear()
        
        Toast.makeText(this, "All animations stopped!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "All animations stopped - full bitmap updates resumed")
    }
    
    private fun stopAllAnimations() {
        leftEyeAnimator?.cancel()
        rightEyeAnimator?.cancel()
        leftEyebrowAnimator?.cancel()
        rightEyebrowAnimator?.cancel()
        noseAnimator?.cancel()
        mouthAnimator?.cancel()
        
        leftEyeAnimator = null
        rightEyeAnimator = null
        leftEyebrowAnimator = null
        rightEyebrowAnimator = null
        noseAnimator = null
        mouthAnimator = null
        
        // Reset last X positions to allow fresh updates
        leftEyeLastX = Float.MIN_VALUE
        rightEyeLastX = Float.MIN_VALUE
        leftEyebrowLastX = Float.MIN_VALUE
        rightEyebrowLastX = Float.MIN_VALUE
        noseLastX = Float.MIN_VALUE
        mouthLastX = Float.MIN_VALUE
    }

    override fun onResizeViews() {
        // Handle view resizing if needed
    }

    override fun onClickViews() {
        mBinding.btnBack.setOnClickListener {
            // Stop recording if active
            if (isRecording) {
                cameraView.stopVideo()
                isRecording = false
            }
            finish()
        }

        mBinding.btnCloseFilterTop?.setOnClickListener {
            // Stop recording if active
            if (isRecording) {
                cameraView.stopVideo()
                isRecording = false
            }
            finish()
        }
    }

    override fun observerData() {
        // Observe data changes if needed
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Stop recording if active
        if (isRecording) {
            cameraView.stopVideo()
            isRecording = false
        }
        
        // Hide progress dialog
        hideProgressDialog()
        
        stopAllAnimations()
        isAnimationRunning = false
        stoppedParts.clear()
        
        // Reset all position tracking variables
        leftEyeLastX = Float.MIN_VALUE
        rightEyeLastX = Float.MIN_VALUE
        leftEyebrowLastX = Float.MIN_VALUE
        rightEyebrowLastX = Float.MIN_VALUE
        noseLastX = Float.MIN_VALUE
        mouthLastX = Float.MIN_VALUE
        
        // Shutdown all thread pools
        faceDetectionExecutor.shutdown()
        bitmapProcessingExecutor.shutdown()
        scope.cancel()
        
        // Cleanup CameraView resources theo TimeDelayVerticalActivity pattern
        if (cameraView.isOpened) {
            cameraView.close()
        }
        cameraView.destroy()
    }
}