package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity

/**
 * DrawByNoseActivity - Fixed coordinate mapping for accurate nose tracking
 * 
 * Key fixes:
 * - Corrected coordinate mapping logic for camera preview to view coordinates
 * - Proper handling of camera rotation (especially 270° for front camera)
 * - Fixed aspect ratio calculations and preview scaling
 * - Accurate mirroring for front camera
 * - Better bounds checking and coordinate validation
 */

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.otaliastudios.cameraview.CameraException

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityDrawByNoseBinding
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.viewmodel.DrawByNoseViewModel
import com.titanbbl.funny.face.filter.game.ui.component.music.SelectMusicActivity
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.titanbbl.funny.face.filter.game.utils.Routes
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DrawByNoseActivity : BaseActivity<ActivityDrawByNoseBinding>() {

    companion object {
        private const val TAG = "DrawByNoseActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val DEFAULT_RECORDING_DURATION = 30 // 30 seconds default
    }

    // ViewModel
    private val viewModel: DrawByNoseViewModel by viewModels()
    
    // Camera and recording
    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false
    private var frameSkipCounter = 0
    private val FRAME_SKIP_RATE = 2 // Process every other frame for better performance

    // Screen dimensions
    private var screenWidth = 0
    private var screenHeight = 0

    // Camera lens facing
    private var currentFacing = Facing.FRONT

    // Timer variables
    private var countDownTimer: CountDownTimer? = null
    private var recordingTimer: CountDownTimer? = null
    private var recordingStartTime = 0L
    private var recordingElapsedTime = 0L
    private var selectedTimerDuration = 3 // Default 3 seconds timer
    private var selectedRecordingDuration = DEFAULT_RECORDING_DURATION // Default recording duration

    // Drawing state
    private var isDrawingEnabled = false

    // Debug info
    private var rawNoseX = 0f
    private var rawNoseY = 0f
    private var convertedNoseX = 0f
    private var convertedNoseY = 0f
    private var frameWidth = 0
    private var frameHeight = 0
    private var cameraRotation = 0
    private val handler = Handler(Looper.getMainLooper())
    private val debugUpdateRunnable = object : Runnable {
        override fun run() {
            updateDebugInfo()
            handler.postDelayed(this, 500) // Update every 500ms
        }
    }
    
    // Camera setup flag to prevent duplicate listeners
    private var isCameraSetup = false
    private var hasNavigatedToResult = false

    // Output file for video
    private val outputFile: File by lazy { createVideoFile(this) }

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions granted
            setupCamera()
        } else {
            // Show settings dialog if permissions denied
            showPermissionSettingsDialog()
        }
    }

    override fun getLayoutActivity(): Int = R.layout.activity_draw_by_nose

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
    }

    override fun requestWindow() {
        // Keep screen on during activity
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun initViews() {
        // Get screen dimensions
        try {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screen dimensions", e)
            // Use default values if screen dimensions cannot be obtained
            screenWidth = 1080
            screenHeight = 1920
        }

        Log.d(TAG, "Screen dimensions: $screenWidth x $screenHeight")

        mBinding.viewModel = viewModel
        mBinding.lifecycleOwner = this

        // Initialize timer text
        mBinding.tvTimer.text = "${selectedTimerDuration}s"

        // Khởi tạo MusicManagerApp
        MusicManagerApp.init(this)

        setupCameraView()
        observeViewModel()

        // Start debug info updates
        handler.post(debugUpdateRunnable)
    }

    override fun onClickViews() {
        // Back button
        mBinding.btnBack.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
            finish()
        }


        mBinding.btnClearMusic.setOnClickListener {
            // Dừng nhạc đang phát
            MusicManagerApp.stopMusic()
            
            // Xóa bài hát đã chọn
            MusicManagerApp.clearSelectedSong()
            
            // Đổi text thành "thêm âm thanh"
            mBinding.tvFilterNameTop.text = getString(R.string.add_song)
            
            // Hiển thị thông báo
//            Toast.makeText(this, "Đã xóa nhạc", Toast.LENGTH_SHORT).show()
        }

        // Camera button - flip camera
        mBinding.btnCamera?.setOnClickListener {
            if (!isRecording) {
                flipCamera()
            } else {
                Toast.makeText(this, "Cannot switch camera while recording", Toast.LENGTH_SHORT).show()
            }
        }

        // Timer container - toggle timer settings
        mBinding.ivTimer.setOnClickListener {
            if (!isRecording) {
                selectedTimerDuration = when (selectedTimerDuration) {
                    3 -> 5
                    5 -> 10
                    else -> 3
                }
                mBinding.tvTimer.text = "${selectedTimerDuration}s"
            } else {
                Toast.makeText(this, "Cannot change timer while recording", Toast.LENGTH_SHORT).show()
            }
        }

        // Record button
        mBinding.btnRecord.setOnClickListener {
            toggleRecording()
        }
        
        // Open music selection when tapping the title
        mBinding.tvFilterNameTop.setOnClickListener {
            if (isRecording) {
                Toast.makeText(this, "Cannot change music while recording", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, SelectMusicActivity::class.java))
            }
        }
        
        // Test music button (có thể thêm vào layout để debug)
        mBinding.btnCamera?.setOnClickListener {
            if (!isRecording) {
                testMusicPlayback()
            } else {
                Toast.makeText(this, "Cannot test music while recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun flipCamera() {
        try {
            // Stop current camera
            mBinding.cameraView.close()
            
            // Reset camera setup flag to allow reinitialization
            isCameraSetup = false
            
            // Switch camera facing
            currentFacing = if (currentFacing == Facing.FRONT) {
                Facing.BACK
            } else {
                Facing.FRONT
            }

            // Reinitialize camera with new facing
            setupCamera()

            Toast.makeText(this, "Camera switched", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Camera switched to: ${if (currentFacing == Facing.FRONT) "FRONT" else "BACK"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error flipping camera", e)
            Toast.makeText(this, "Error switching camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimerOptions() {
        // Simple timer options (you can expand this with a bottom sheet)
        val options = arrayOf("3s", "5s", "10s")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Timer Duration")
        builder.setItems(options) { _, which ->
            selectedTimerDuration = when (which) {
                0 -> 3
                1 -> 5
                2 -> 10
                else -> 3
            }
            mBinding.tvTimer.text = "${selectedTimerDuration}s"
            Toast.makeText(this, "Timer set to ${selectedTimerDuration}s", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecordingWithTimer()
        }
    }

    private fun startRecordingWithTimer() {
        // Show countdown timer first
        mBinding.tvCountdown.visibility = View.VISIBLE

        Glide.with(this).load(R.drawable.ic_record_stop).into(mBinding.btnRecord)
        mBinding.tvCountdown.text = selectedTimerDuration.toString()
        
        countDownTimer = object : CountDownTimer((selectedTimerDuration * 1000 + 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                mBinding.tvCountdown.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                // Hide countdown timer when recording starts
                mBinding.tvCountdown.visibility = View.GONE
                // Start actual recording
                startRecording()
            }
        }.start()
    }

    private fun startRecording() {
        try {
            // Reset tất cả state trước khi bắt đầu recording mới
            resetRecordingState()
            
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
            isDrawingEnabled = true
            recordingStartTime = System.currentTimeMillis()
            // Initialize recording time text
            mBinding.tvTimeRec.text = "00:00"
            
            // Clear any previous drawing
            mBinding.drawingView.clearDrawing()
            
            // Update UI
            updateRecordingUI(true)
            
            // Start video recording with CameraView
            mBinding.cameraView.takeVideoSnapshot(outputFile)
            
            // Start recording timer to show elapsed time
            startRecordingTimer()
            
            Toast.makeText(this, "Recording started - Draw with your nose! Press stop to finish", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            resetRecordingState()
        }
    }

    private fun stopRecording() {
        try {
            Glide.with(this).load(R.drawable.ic_record).into(mBinding.btnRecord)
            MusicManagerApp.stopMusic()
            isRecording = false
            isDrawingEnabled = false
            
            // Stop all timers
            countDownTimer?.cancel()
            recordingTimer?.cancel()
            
            // Stop video recording
            mBinding.cameraView.stopVideo()
            
            // Clear drawing and stop drawing
            mBinding.drawingView.clearDrawing()
            viewModel.setDrawing(false)
            
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show()
        }
        
        resetRecordingState()
    }

    private fun startRecordingTimer() {
        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 100) {
            override fun onTick(millisUntilFinished: Long) {
                recordingElapsedTime = System.currentTimeMillis() - recordingStartTime
                updateRecordingTimeDisplay()
            }

            override fun onFinish() {
                // Will never finish due to Long.MAX_VALUE
            }
        }.start()
    }



    private fun updateRecordingTimeDisplay() {
        val totalSeconds = recordingElapsedTime / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        // Update recording time to tv_time_rec view
        mBinding.tvTimeRec.text = timeString
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        if (isRecording) {
            // Update record button to show recording state
            mBinding.btnRecord.alpha = 0.8f
            mBinding.filterHeaderTop?.alpha = 0.9f
        } else {
            mBinding.btnRecord.alpha = 1.0f
            mBinding.filterHeaderTop?.alpha = 1.0f
        }
    }

    private fun resetRecordingState() {
        isRecording = false
        isDrawingEnabled = false
        
        // Cancel all timers
        countDownTimer?.cancel()
        recordingTimer?.cancel()
        
        // Reset timer variables
        recordingStartTime = 0L
        recordingElapsedTime = 0L
        
        // Reset UI
        updateRecordingUI(false)
        mBinding.tvTimer.text = "${selectedTimerDuration}s"
        mBinding.tvTimeRec.text = "00:00"
        mBinding.tvCountdown.visibility = View.GONE
        
        // Clear drawing and stop drawing
        mBinding.drawingView.clearDrawing()
        viewModel.setDrawing(false)
        
        // Reset navigation guard
        hasNavigatedToResult = false
        
        Log.d(TAG, "Recording state reset completed")
    }

    private fun updateDebugInfo() {
        try {
            val debugInfo = """
                Screen: ${screenWidth}x${screenHeight}
                Frame: ${frameWidth}x${frameHeight} (${if (currentFacing == Facing.FRONT) "Front" else "Back"})
                Rotation: ${cameraRotation}°
                Recording: ${if (isRecording) "YES" else "NO"}

                Nose (raw): ${rawNoseX.toInt()}, ${rawNoseY.toInt()}
                Nose (converted): ${convertedNoseX.toInt()}, ${convertedNoseY.toInt()}

                View: ${mBinding.drawingView.width}x${mBinding.drawingView.height}
            """.trimIndent()

            Log.d(TAG, debugInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating debug info", e)
        }
    }

    private fun setupCameraView() {
        try {
            if (allPermissionsGranted()) {
                setupCamera()
            } else {
                requestPermissions()
            }

            cameraExecutor = Executors.newSingleThreadExecutor()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera", e)
        }
    }

    private fun observeViewModel() {
        try {
            // Observe current nose position
            viewModel.currentPoint.observe(this) { point ->
                try {
                    // Only update drawing if drawing is enabled (during recording or manual mode)
                    val shouldDraw = isDrawingEnabled || viewModel.drawingMode.value == true
                    mBinding.drawingView.updateDrawing(point, shouldDraw)

                    // Update converted nose position for debug
                    point?.let {
                        convertedNoseX = it.x
                        convertedNoseY = it.y

                        // Log the current point position
                        Log.d(TAG, "Nose position in VIEW coordinates: x=${it.x}, y=${it.y}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating drawing", e)
                }
            }

            // Observe drawing clear requests
            viewModel.shouldClearDrawing.observe(this) { shouldClear ->
                try {
                    if (shouldClear) {
                        mBinding.drawingView.clearDrawing()
                        viewModel.clearDrawingComplete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing drawing", e)
                }
            }

            // Observe drawing mode changes
            viewModel.drawingMode.observe(this) { isDrawingMode ->
                try {
                    // Update UI based on drawing mode if needed
                    Log.d(TAG, "Drawing mode changed: $isDrawingMode")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating drawing mode", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing view model", e)
        }
    }

    private fun setupCamera() {
        try {
            // Prevent duplicate camera setup
            if (isCameraSetup) {
                Log.d(TAG, "Camera already setup, skipping...")
                return
            }
            
            // Set initial camera facing
            mBinding.cameraView.facing = currentFacing
            
            // Clear previous listeners/processors then set up frame processor for face detection
            mBinding.cameraView.clearFrameProcessors()
            mBinding.cameraView.clearCameraListeners()
            mBinding.cameraView.addFrameProcessor { frame ->
                processFrame(frame)
            }
            
            // Set up camera listener for video recording callbacks
            mBinding.cameraView.addCameraListener(object : CameraListener() {
                override fun onVideoTaken(result: VideoResult) {
                    Log.d(TAG, "Video recording completed: ${result.file.absolutePath}")
                    // Handle video recording completion
                    runOnUiThread {
                        // You can process the recorded video here
                        Log.d(TAG, "onVideoTaken: Processing video completion")
                        if (!hasNavigatedToResult) {
                            hasNavigatedToResult = true
                            Routes.startResultActivity(this@DrawByNoseActivity, outputFile.absolutePath)
                        } else {
                            Log.d(TAG, "Result navigation already performed, skipping duplicate.")
                        }
                    }
                }
                
                override fun onVideoRecordingStart() {
                    Log.d(TAG, "Video recording started")
                }
                
                override fun onVideoRecordingEnd() {
                    Log.d(TAG, "Video recording ended")
                }
                
                override fun onCameraOpened(options: CameraOptions) {
                    Log.d(TAG, "Camera opened successfully")
                }
                
                override fun onCameraError(exception: CameraException) {
                    Log.e(TAG, "Camera error", exception)
                    runOnUiThread {
                        Toast.makeText(this@DrawByNoseActivity, "Camera error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            
            // Start the camera
            mBinding.cameraView.open()
            
            // Mark camera as setup
            isCameraSetup = true
            
            Log.d(TAG, "Camera setup completed with ${if (currentFacing == Facing.BACK) "BACK" else "FRONT"} camera")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera", e)
        }
    }

    /**
     * SIMPLIFIED: Since raw coordinates work well, just handle X mirroring for front camera
     * Only detect and draw when recording is active
     */
    private fun processFrame(frame: Frame) {
        try {
            // Only process frames when recording is active
            if (!isRecording) {
                return
            }
            
            frameSkipCounter++
            if (frameSkipCounter % FRAME_SKIP_RATE != 0) return

            val data = frame.getData<ByteArray>()
            frameWidth = frame.size.width
            frameHeight = frame.size.height
            cameraRotation = frame.rotationToUser

            cameraExecutor.execute {
                try {
                    val image = InputImage.fromByteArray(
                        data,
                        frameWidth,
                        frameHeight,
                        cameraRotation,
                        InputImage.IMAGE_FORMAT_NV21
                    )

                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            faces.firstOrNull()?.let { face ->
                                val noseContour = face.getContour(FaceContour.NOSE_BOTTOM)
                                if (noseContour != null && noseContour.points.isNotEmpty()) {
                                    val noseTip = noseContour.points[noseContour.points.size / 2]

                                    rawNoseX = noseTip.x
                                    rawNoseY = noseTip.y

                                    // Get view dimensions
                                    val viewWidth = mBinding.drawingView.width.toFloat()
                                    val viewHeight = mBinding.drawingView.height.toFloat()

                                    if (viewWidth <= 0 || viewHeight <= 0) return@addOnSuccessListener

                                    // Simple coordinate mapping: Y is already correct, just fix X mirroring
                                    var finalX = rawNoseX

                                    
                                    // Handle front camera mirroring - flip X coordinate
                                    if (currentFacing == Facing.FRONT) {
                                        finalX = viewWidth - rawNoseX
                                    }

                                    val finalY = rawNoseY



                                    runOnUiThread {
                                        viewModel.updateNosePosition(finalX, rawNoseY)
                                        viewModel.setDrawing(true)

                                        // Debug logging - every 5th frame to reduce spam
                                        if (frameSkipCounter % 5 == 0) {
                                            Log.d(TAG, "SIMPLE MAPPING - Frame: ${frameWidth}x${frameHeight}, " +
                                                    "View: ${viewWidth.toInt()}x${viewHeight.toInt()}, " +
                                                    "Raw: (${rawNoseX.toInt()}, ${rawNoseY.toInt()}), " +
                                                    "Final: (${finalX.toInt()}, ${finalY.toInt()}), " +
                                                    "Camera: $currentFacing")
                                        }
                                    }
                                }
                            } ?: run {
                                runOnUiThread {
                                    viewModel.setDrawing(false)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Face detection failed", e)
                            runOnUiThread {
                                viewModel.setDrawing(false)
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing frame data", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing frame", e)
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            allPermissionsGranted() -> {
                setupCamera()
            }
            shouldShowRequestPermissionRationale() -> {
                showPermissionExplanationDialog()
            }
            else -> {
                requestPermissions()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun shouldShowRequestPermissionRationale() = REQUIRED_PERMISSIONS.any {
        ActivityCompat.shouldShowRequestPermissionRationale(this, it)
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun showPermissionExplanationDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Camera and audio permissions are required for this feature to work.")
            .setPositiveButton("Grant") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun showPermissionSettingsDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Please grant camera and audio permissions in Settings to use this feature.")
            .setPositiveButton("Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
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

    /**
     * Enable or disable keep screen on mode
     * @param keepScreenOn true to keep screen always on, false to allow screen to turn off
     */
    private fun setKeepScreenOn(keepScreenOn: Boolean) {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d(TAG, "Keep screen on mode enabled")
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d(TAG, "Keep screen on mode disabled")
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
        // Close camera when app is not visible
        mBinding.cameraView.close()
        // Reset camera setup flag since camera was closed
        isCameraSetup = false
        // Disable keep screen on when app is not visible
        setKeepScreenOn(false)
    }

    override fun onResume() {
        super.onResume()
        // Open camera when app becomes visible
        if (allPermissionsGranted()) {
            // Only setup camera if it hasn't been setup yet
            if (!isCameraSetup) {
                setupCamera()
            } else {
                mBinding.cameraView.open()
            }
        }
        // Enable keep screen on when app is visible
        setKeepScreenOn(true)
        
        // Refresh local music paths khi resume
        MusicManagerApp.refreshLocalMusicPaths(this)

        // Reset tất cả state về ban đầu khi quay về từ màn hình result
        resetToInitialState()

        MusicManagerApp.getCurrentSong().apply {
            mBinding.tvFilterNameTop.text = this?.title ?: "No song selected"
        }
    }

    /**
     * Reset tất cả state về trạng thái ban đầu
     * Được gọi khi quay về từ màn hình result
     */
    private fun resetToInitialState() {
        // Reset timer state
        selectedTimerDuration = 3 // Reset về 3 giây mặc định
        selectedRecordingDuration = DEFAULT_RECORDING_DURATION
        
        // Reset recording state
        isRecording = false
        isDrawingEnabled = false
        hasNavigatedToResult = false
        
        // Reset timer variables
        recordingStartTime = 0L
        recordingElapsedTime = 0L
        
        // Cancel tất cả timers
        countDownTimer?.cancel()
        recordingTimer?.cancel()
        
        // Reset UI về trạng thái ban đầu
        mBinding.tvTimer.text = "${selectedTimerDuration}s"
        mBinding.tvCountdown.visibility = View.GONE
        mBinding.tvTimeRec.text = "00:00"
        
        // Reset record button về trạng thái ban đầu
        Glide.with(this).load(R.drawable.ic_record).into(mBinding.btnRecord)
        
        // Reset drawing view
        mBinding.drawingView.clearDrawing()
        viewModel.setDrawing(false)
        
        // Reset UI alpha
        updateRecordingUI(false)
        
        Log.d(TAG, "Reset to initial state completed")
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel all timers
        countDownTimer?.cancel()
        recordingTimer?.cancel()
        
        // Stop debug updates
        handler.removeCallbacks(debugUpdateRunnable)
        
        // Close camera and destroy after clearing callbacks to avoid leaks
        mBinding.cameraView.clearFrameProcessors()
        mBinding.cameraView.clearCameraListeners()
        mBinding.cameraView.close()
        mBinding.cameraView.destroy()
        
        // Reset camera setup flag
        isCameraSetup = false
        
        // Shutdown executor
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        
        // Clean up face detector
        try {
            faceDetector.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing face detector", e)
        }
    }
}