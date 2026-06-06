package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.prediction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityPredictionBinding
import com.titanbbl.funny.face.filter.game.app.AppConstants
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.bases.ext.click
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.titanbbl.funny.face.filter.game.utils.Routes
import com.titanbbl.funny.face.filter.game.utils.Routes.startResultActivity
import com.titanbbl.funny.face.filter.game.utils.parcelable
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class PredictionActivity : BaseActivity<ActivityPredictionBinding>() {

    private lateinit var overlayText: PredictionLayout
    private lateinit var cameraExecutor: ExecutorService
    private var currentTextIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var screenWidth = 0
    private var screenHeight = 0
    private var isProcessingFrame = false
    private var lastProcessedTime = 0L
    private val PROCESS_INTERVAL = 16 // Decreased from 16ms for more frequent updates

    // Animation properties
    private val interpolator = AccelerateDecelerateInterpolator()
    private var lastRotationX = 0f
    private var lastRotationY = 0f
    private var lastRotationZ = 0f
    private val ROTATION_SMOOTHING_FACTOR = 0.3f
    private val ROTATION_THRESHOLD = 0.2f
    private val TEXT_CHANGE_INTERVAL = 400L

    // Recording variables
    private var isRecording = false
    private var countDownTimer: CountDownTimer? = null
    private var recordingTimer: CountDownTimer? = null
    private var recordingStartTime = 0L
    private var recordingElapsedTime = 0L
    private var selectedTimerDuration = 3 // Default 3 seconds timer
    // Recording duration options: 10s, 20s, 30s
    private var selectedRecordingDuration = 10 // Default 10 seconds recording duration
    private val outputFile: File by lazy { createVideoFile(this) }
    private var hasNavigatedToResult = false

    // List of prediction texts instead of images
    private var listPredictionTexts = listOf(
        "You will find love soon!",
        "A great opportunity is coming",
        "You will travel to a new place",
        "Good fortune awaits you",
        "You will meet someone special",
        "Your dreams will come true",
        "Success is in your future",
        "You will receive good news",
        "A surprise awaits you",
        "You will achieve your goals"
    ).toMutableList()

    private val textRunnable by lazy {
        object : Runnable {
            override fun run() {
                // Check if we've reached 5 seconds of recording
                val currentRecordingTime = if (isRecording) {
                    (System.currentTimeMillis() - recordingStartTime) / 1000
                } else 0
                
                if (currentRecordingTime >= 5) {
                    // Stop text animation at 5 seconds
                    Log.d(TAG, "Text animation stopped at 5 seconds")
                    return
                }
                
                currentTextIndex = (currentTextIndex + 1) % listPredictionTexts.size
                overlayText.setTextWithAnimation(listPredictionTexts[currentTextIndex])
                handler.postDelayed(this, TEXT_CHANGE_INTERVAL)
            }
        }
    }

    companion object {
        private const val TAG = "PredictionActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build()
        )
    }

    private val cameraListener = object : CameraListener() {
        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            Log.e(TAG, "Camera error: ${exception.message}")
            Toast.makeText(this@PredictionActivity, "Camera error occurred", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onCameraOpened(options: CameraOptions) {
            super.onCameraOpened(options)
            Log.d(TAG, "Camera opened successfully")
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            Log.d(TAG, "Video recorded successfully: ${result.file.absolutePath}")
            
            // Dừng nhạc khi video hoàn thành
            MusicManagerApp.stopMusic()
            
            if (!hasNavigatedToResult) {
                hasNavigatedToResult = true
                startResultActivity(this@PredictionActivity, result.file.absolutePath)
                // Reset UI after video is saved
                resetRecordingState()
            } else {
                Log.d(TAG, "Navigation to result already performed. Skipping duplicate.")
            }
        }
    }

    override fun getLayoutActivity(): Int = R.layout.activity_prediction

    override fun initViews() {
        overlayText = mBinding.overlayImage

        // Enable hardware acceleration and improve rendering
        overlayText.setLayerType(ImageView.LAYER_TYPE_HARDWARE, null)
        overlayText.alpha = 0.9f

        // Set background drawable for text display
//        overlayText.setBackgroundDrawable(R.drawable.bg_prediction_text)

        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // Set overlayText size to 40% of screen width
//        val imageSize = (screenWidth * 0.4).toInt()
//        overlayText.layoutParams.width = imageSize
//        overlayText.layoutParams.height = imageSize
//        overlayText.requestLayout()

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize timer text
        mBinding.tvTimer.text = "${selectedTimerDuration}s"
        // Initialize recording duration text
        mBinding.tvTimerPlay.text = "${selectedRecordingDuration}s"

        // Khởi tạo MusicManagerApp
        MusicManagerApp.init(this)

        // Check permissions immediately at startup
        checkAndRequestPermissions()

        // Don't start textRunnable here - only start when recording begins

        predictionItem = intent.parcelable(AppConstants.KEY_PREDICTION_ITEM)
        listPredictionTexts = predictionItem?.predictions?.toMutableList() ?: listOf(
            "You will find love soon!",
            "A great opportunity is coming",
            "You will travel to a new place",
            "Good fortune awaits you",
            "You will meet someone special",
            "Your dreams will come true",
            "Success is in your future",
            "You will receive good news",
            "A surprise awaits you",
            "You will achieve your goals"
        ).toMutableList()
        listPredictionTexts.shuffle()
        overlayText.setQuestion(predictionItem?.question ?: "What does your future hold?")
    }

    var predictionItem: PredictionResponseItem? = null

    private fun checkAndRequestPermissions() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            // Show rationale before requesting permissions
            if (shouldShowRequestPermissionRationale()) {
                showPermissionRationaleDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
        return REQUIRED_PERMISSIONS.any {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera and Microphone Permissions Required")
            .setMessage("This app needs camera and microphone access to function. Please grant these permissions to continue.")
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Camera and microphone permissions are required for this app. Please enable them in app settings.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        try {
            hasNavigatedToResult = false
            mBinding.cameraView.setLifecycleOwner(this)
            mBinding.cameraView.facing = Facing.FRONT
            mBinding.cameraView.mode = Mode.VIDEO
            mBinding.cameraView.addCameraListener(cameraListener)
            mBinding.cameraView.previewFrameRate = 24f

            mBinding.cameraView.addFrameProcessor(object : FrameProcessor {
                override fun process(frame: Frame) {
                    val currentTime = System.currentTimeMillis()
                    if (!isProcessingFrame && currentTime - lastProcessedTime > PROCESS_INTERVAL) {
                        isProcessingFrame = true
                        lastProcessedTime = currentTime

                        processFrame(frame)
                    }
                }
            })

            if (!mBinding.cameraView.isOpened) {
                mBinding.cameraView.open()
            }

            Log.d(TAG, "Camera started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera", e)
            Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processFrame(frame: Frame) {
        try {
            // Get frame data - CameraView's Frame has getData() method with explicit type
            val data = frame.getData<ByteArray>()
            val size = frame.size
            val rotation = frame.rotationToUser

            if (frame.format == android.graphics.ImageFormat.NV21 ||
                frame.format == android.graphics.ImageFormat.YUV_420_888
            ) {

                val image = InputImage.fromByteArray(
                    data,
                    size.width,
                    size.height,
                    rotation,
                    InputImage.IMAGE_FORMAT_NV21
                )

                detector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            updateOverlayPosition(faces[0])
                            updateOverlayRotation(faces[0])
                        }
                        isProcessingFrame = false
                    }
                    .addOnFailureListener {
                        isProcessingFrame = false
                    }
            } else {
                isProcessingFrame = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
            isProcessingFrame = false
        }
    }

    private fun updateOverlayPosition(face: Face) {
        try {
            val bounds = face.boundingBox
            val centerX = bounds.centerX().toFloat()
            val topY = bounds.top.toFloat()

            val scaleX = screenWidth.toFloat() / mBinding.cameraView.width
            val scaleY = screenHeight.toFloat() / mBinding.cameraView.height

            // Mirror the X coordinate for front camera
            val mirroredX = mBinding.cameraView.width - centerX

            val screenX = mirroredX * scaleX - (overlayText.width / 2)
            val screenY = maxOf(0f, topY * scaleY - overlayText.height - 50)

            runOnUiThread {
                overlayText.x = screenX.coerceIn(0f, (screenWidth - overlayText.width).toFloat())
                overlayText.y =
                    screenY.coerceIn(0f, (screenHeight - overlayText.height).toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOverlayRotation(face: Face) {
        try {
            // Get face rotation angles
            var rotationX = face.headEulerAngleX
            var rotationY = face.headEulerAngleY
            var rotationZ = face.headEulerAngleZ

            // Mirror rotationY for front camera
            rotationY = -rotationY

            // Limit rotation angles for more stable appearance
            rotationX = rotationX.coerceIn(-30f, 30f)
            rotationY = rotationY.coerceIn(-30f, 30f)
            rotationZ = rotationZ.coerceIn(-25f, 25f)

            // Apply smoothing to rotations with improved thresholds
            lastRotationX += (rotationX - lastRotationX) * ROTATION_SMOOTHING_FACTOR
            lastRotationY += (rotationY - lastRotationY) * ROTATION_SMOOTHING_FACTOR
            lastRotationZ += (rotationZ - lastRotationZ) * ROTATION_SMOOTHING_FACTOR

            // Apply 3D rotation with improved perspective
            runOnUiThread {
                overlayText.rotation = lastRotationZ
                overlayText.rotationX = lastRotationX
                overlayText.rotationY = lastRotationY

                // Improved scale calculation for better perspective
                val scaleY = 1.0f - abs(lastRotationY) / 180.0f * 0.15f
                val scaleX = scaleY * (1.0f - abs(lastRotationX) / 180.0f * 0.08f)

                overlayText.scaleX = scaleX
                overlayText.scaleY = scaleY

                // Add subtle alpha changes based on rotation for better depth perception
                overlayText.alpha = 0.95f - (abs(lastRotationY) / 180.0f * 0.15f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecordingWithTimer()
        }
    }

    private fun startRecordingWithTimer() {
        mBinding.tvTimer.visibility = View.VISIBLE
        Glide.with(this).load(R.drawable.ic_record_stop).into(mBinding.btnRecord)
        // Keep showing the selected timer mode, don't count down
        mBinding.tvTimer.text = "${selectedTimerDuration}s"

        // Show countdown text and start counting down
        mBinding.tvTimerCoundown.visibility = View.VISIBLE
        mBinding.tvTimerCoundown.text = selectedTimerDuration.toString()

        countDownTimer = object : CountDownTimer((selectedTimerDuration * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                mBinding.tvTimerCoundown.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                // Hide countdown text when countdown finishes
                mBinding.tvTimerCoundown.visibility = View.GONE
                startRecording()
            }
        }.start()
    }

    private fun startRecording() {
        try {
            // Kiểm tra và phát nhạc
            val currentSong = MusicManagerApp.getCurrentSong()
            if (currentSong != null) {
                Log.d(TAG, "Starting music playback for song: ${currentSong.title}")
                Log.d(TAG, "Song URL: ${currentSong.url}")
                
                // Kiểm tra xem có file local không
                if (MusicManagerApp.isLocalMusicAvailable(this, currentSong)) {
                    val localPath = MusicManagerApp.getCurrentLocalMusicPath(this)
                    Log.d(TAG, "Local music available at: $localPath")
                } else {
                    Log.d(TAG, "No local music available, will use URL")
                }
                
                MusicManagerApp.playMusic(this, {})
            } else {
                Log.w(TAG, "No song selected for music playback")
            }
            
            isRecording = true
            hasNavigatedToResult = false
            recordingStartTime = System.currentTimeMillis()

            updateRecordingUI(true)
            // Use selected recording duration
            mBinding.cameraView.takeVideoSnapshot(outputFile)
            startRecordingTimer()
            
            // Start text animation when recording begins
            startTextAnimation()

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            resetRecordingState()
        }
    }

    private fun stopRecording() {
        try {
            Glide.with(this).load(R.drawable.ic_record).into(mBinding.btnRecord)
            
            // Dừng nhạc khi dừng ghi video
            MusicManagerApp.stopMusic()
            
            isRecording = false

            countDownTimer?.cancel()
            recordingTimer?.cancel()

            if (mBinding.cameraView.isTakingVideo) {
                mBinding.cameraView.stopVideo()
            }
            
            // Stop text animation when recording stops
            stopTextAnimation()

            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show()
        }

        resetRecordingState()
    }

    private fun startRecordingTimer() {
        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                recordingElapsedTime = System.currentTimeMillis() - recordingStartTime
                updateRecordingTimeDisplay()
            }

            override fun onFinish() {
            }
        }.start()
    }

    /**
     * Auto-stop timer that automatically stops recording after selected duration
     * and triggers navigation to result screen
     */
    private fun startAutoStopTimer() {
        // Use selected recording duration
        val totalDurationMs = selectedRecordingDuration * 1000L

        object : CountDownTimer(totalDurationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Don't update tvTimer text - keep showing the selected mode
            }

            override fun onFinish() {
                if (isRecording) {
                    stopRecording()
                    Toast.makeText(
                        this@PredictionActivity,
                        "Recording completed (${selectedRecordingDuration}s)",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Recording will automatically stop and onVideoTaken will be called
                    // which will navigate to the result screen
                }
            }
        }.start()
    }

    private fun updateRecordingTimeDisplay() {
        // Update the on-screen recording timer (tv_time_rec)
        val totalSeconds = (recordingElapsedTime / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val formatted = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        mBinding.tvTimeRec?.text = formatted
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        if (isRecording) {
            mBinding.btnRecord.alpha = 0.8f
            mBinding.filterHeaderTop.alpha = 0.9f
        } else {
            mBinding.btnRecord.alpha = 1.0f
            mBinding.filterHeaderTop.alpha = 1.0f
        }
    }

    private fun resetRecordingState() {
        isRecording = false

        countDownTimer?.cancel()
        recordingTimer?.cancel()

        // Dừng nhạc khi reset trạng thái ghi video
        MusicManagerApp.stopMusic()

        updateRecordingUI(false)
        
        // Reset displays to show selected modes
        mBinding.tvTimer.text = "${selectedTimerDuration}s"
        mBinding.tvTimerPlay.text = "${selectedRecordingDuration}s"
        mBinding.overlayImage.setTextWithAnimation("???")
        
        // Ensure text animation is stopped when recording state is reset
        stopTextAnimation()
        
        // Hide countdown text when resetting
        mBinding.tvTimerCoundown.visibility = View.GONE

        // Reset recording time display
        mBinding.tvTimeRec?.text = "00:00"
    }

    /**
     * Start the text animation when recording begins
     */
    private fun startTextAnimation() {
        try {
            // Remove any existing callbacks first to avoid duplicates
            handler.removeCallbacks(textRunnable)
            // Start the text animation
            handler.post(textRunnable)
            Log.d(TAG, "Text animation started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start text animation", e)
        }
    }

    /**
     * Stop the text animation when recording stops or activity pauses
     */
    private fun stopTextAnimation() {
        try {
            // Remove the text animation callback
            handler.removeCallbacks(textRunnable)
            Log.d(TAG, "Text animation stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop text animation", e)
        }
    }

    /**
     * Test việc phát nhạc để debug
     */
    private fun testMusicPlayback() {
        try {
            val currentSong = MusicManagerApp.getCurrentSong()
            if (currentSong != null) {
                Log.d(TAG, "=== MUSIC DEBUG INFO ===")
                Log.d(TAG, "Song: ${currentSong.title}")
                Log.d(TAG, "URL: ${currentSong.url}")
                Log.d(TAG, "Local available: ${MusicManagerApp.isLocalMusicAvailable(this, currentSong)}")
                
                val localPath = MusicManagerApp.getCurrentLocalMusicPath(this)
                Log.d(TAG, "Local path: $localPath")
                
                // Thử phát nhạc
                MusicManagerApp.playMusic(this) {
                    Log.d(TAG, "Music playback completed")
                }
                
                Toast.makeText(this, "Testing music playback...", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "No song selected")
                Toast.makeText(this, "No song selected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing music playback", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun flipCamera() {
        try {
            // Close current camera
            if (mBinding.cameraView.isOpened) {
                mBinding.cameraView.close()
            }

            // Toggle facing
            mBinding.cameraView.facing =
                if (mBinding.cameraView.facing == Facing.FRONT) Facing.BACK else Facing.FRONT

            // Restart camera
            mBinding.cameraView.open()

            Toast.makeText(this, "Camera switched", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flip camera", e)
            Toast.makeText(this, "Failed to switch camera", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // Check if user clicked "Don't ask again"
                if (!shouldShowRequestPermissionRationale()) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(
                        this,
                        "Camera and microphone permissions are required",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permissions again when resuming
        if (allPermissionsGranted() && !mBinding.cameraView.isOpened) {
            startCamera()
        }
        // Restart text animation if recording is active
        if (isRecording) {
            startTextAnimation()
        }
        
        // Refresh local music paths khi resume
        MusicManagerApp.refreshLocalMusicPaths(this)

        // Hiển thị tên bài hát hiện tại
        MusicManagerApp.getCurrentSong().apply {
            mBinding.tvFilterNameTop.text = this?.title ?: "No song selected"
        }
    }

    override fun onResizeViews() {
        // Not needed for this activity
    }

    override fun onClickViews() {
        mBinding.btnBack.click {
            if (isRecording) {
                stopRecording()
            }
            finish()
        }

        mBinding.tvFilterNameTop.click {
            Routes.startSelectMusicActivity(this)
        }
        
        mBinding.btnCloseFilterTop?.click {
            MusicManagerApp.stopMusic()

            // Xóa bài hát đã chọn
            MusicManagerApp.clearSelectedSong()

            // Đổi text thành "thêm âm thanh"
            mBinding.tvFilterNameTop.text = getString(R.string.add_song)
        }

        mBinding.btnCamera?.click {
            if (!isRecording) {
                flipCamera()
            } else {
                Toast.makeText(this, "Cannot switch camera while recording", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        mBinding.ivTimer?.click {
            if (!isRecording) {
                // Cycle through timer options: 3s -> 5s -> 10s -> 3s
                selectedTimerDuration = when (selectedTimerDuration) {
                    3 -> 5
                    5 -> 10
                    10 -> 3
                    else -> 3
                }
                mBinding.tvTimer.text = "${selectedTimerDuration}s"
                Toast.makeText(this, "Timer set to ${selectedTimerDuration}s", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot change timer while recording", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        
        mBinding.ivTimerPlay?.click {
            if (!isRecording) {
                // Cycle through recording duration options: 10s -> 20s -> 30s -> 10s
                selectedRecordingDuration = when (selectedRecordingDuration) {
                    10 -> 20
                    20 -> 30
                    30 -> 10
                    else -> 10
                }
                mBinding.tvTimerPlay.text = "${selectedRecordingDuration}s"
                Toast.makeText(this, "Recording duration set to ${selectedRecordingDuration}s", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot change recording duration while recording", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        mBinding.btnRecord.click {
            toggleRecording()
        }
    }

    override fun observerData() {
        // No LiveData to observe in this activity
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
        // Always stop text animation when activity pauses
        stopTextAnimation()
        
        // Dừng nhạc khi activity pause
        MusicManagerApp.stopMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            countDownTimer?.cancel()
            recordingTimer?.cancel()
            // Ensure text animation is stopped
            stopTextAnimation()

            // Dừng nhạc khi activity destroy
            MusicManagerApp.stopMusic()

            if (::cameraExecutor.isInitialized) {
                cameraExecutor.shutdown()
            }

            if (mBinding.cameraView.isOpened) {
                mBinding.cameraView.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}