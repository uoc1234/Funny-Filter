package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
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
import java.io.ByteArrayOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityHandDetchDrawingBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.viewmodel.HandDetectViewModel
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.titanbbl.funny.face.filter.game.utils.Routes
import com.titanbbl.funny.face.filter.game.ui.component.music.SelectMusicActivity
import com.bumptech.glide.Glide
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.size.SizeSelectors
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.sqrt

class HandDetectActivity : BaseActivity<ActivityHandDetchDrawingBinding>() {

    companion object {
        private const val TAG = "HandDetectActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val DEFAULT_RECORDING_DURATION = 30 // 30 seconds default
    }

    // ViewModel
    private val viewModel: HandDetectViewModel by viewModels()
    
    // Camera and recording
    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false
    private var frameSkipCounter = 0
    private val FRAME_SKIP_RATE = 4 // Process every 4th frame for better performance and memory usage

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
    private var isDrawingEnabled = true // Bật mặc định để test

    // MediaPipe Hands
    private var hands: Hands? = null

    // Debug info
    private var rawHandX = 0f
    private var rawHandY = 0f
    private var convertedHandX = 0f
    private var convertedHandY = 0f
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

    // Output file for video
    private val outputFile: File by lazy { createVideoFile(this) }

    // Camera setup and navigation guards
    private var isCameraSetup = false
    private var hasNavigatedToResult = false

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

    override fun getLayoutActivity(): Int = R.layout.activity_hand_detch_drawing

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


        mBinding.lifecycleOwner = this

        // Khởi tạo MusicManagerApp để quản lý nhạc
        MusicManagerApp.init(this)

        // Initialize timer text
        mBinding.tvTimer.text = "${selectedTimerDuration}s"

        setupMediaPipeHands()
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

        // Close filter button
        mBinding.btnCloseFilterTop.setOnClickListener {
            MusicManagerApp.stopMusic()

            // Xóa bài hát đã chọn
            MusicManagerApp.clearSelectedSong()

            // Đổi text thành "thêm âm thanh"
            mBinding.tvFilterNameTop.text = getString(R.string.add_song)
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
        mBinding.ivTimer?.setOnClickListener {
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

        // Mở chọn nhạc khi nhấn tiêu đề
        mBinding.tvFilterNameTop.setOnClickListener {
            if (isRecording) {
                Toast.makeText(this, "Cannot change music while recording", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, SelectMusicActivity::class.java))
            }
        }

        // Add a test button to toggle drawing mode
        mBinding.btnCamera?.setOnClickListener {
            if (!isRecording) {
                // Toggle drawing mode for testing
                isDrawingEnabled = !isDrawingEnabled
                Log.d(TAG, "Chế độ vẽ: ${if (isDrawingEnabled) "BẬT" else "TẮT"}")
                Toast.makeText(this, "Drawing mode: ${if (isDrawingEnabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot switch camera while recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMediaPipeHands() {
        try {
            Log.d(TAG, "Bắt đầu setup MediaPipe Hands")
            
            val options = HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(1)
                .setMinDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            hands = Hands(this, options)
            Log.d(TAG, "MediaPipe Hands instance created")

            hands?.setErrorListener { message, _ ->
                Log.e(TAG, "MediaPipe Hands error: $message")
            }

            hands?.setResultListener { handsResult ->
                Log.d(TAG, "Nhận kết quả từ MediaPipe Hands")
                processHandsResult(handsResult)
            }
            
            Log.d(TAG, "MediaPipe Hands setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up MediaPipe Hands", e)
        }
    }

    private fun processHandsResult(result: HandsResult) {
        try {
            Log.d(TAG, "processHandsResult: Xử lý kết quả từ MediaPipe Hands")

            if (result.multiHandLandmarks().isEmpty()) {
                Log.d(TAG, "Không phát hiện thấy tay")
                runOnUiThread {
                    viewModel.setDrawing(false)
                }
                return
            }

            // Luôn xử lý kết quả, bất kể trạng thái isDrawingEnabled
            // Chỉ kiểm tra isDrawingEnabled khi thực sự vẽ
            Log.d(TAG, "Phát hiện thấy ${result.multiHandLandmarks().size} bàn tay")

            val landmarks = result.multiHandLandmarks()[0].landmarkList
            Log.d(TAG, "Số lượng landmarks: ${landmarks.size}")

            // Phát hiện trạng thái bàn tay (mở/đóng)
            val isHandOpen = detectHandOpenState(landmarks)
            Log.d(TAG, "Trạng thái bàn tay: ${if (isHandOpen) "MỞ" else "ĐÓNG"}")

            if (landmarks.size >= 9) { // Index finger tip is landmark 8
                val indexFingerTip = landmarks[8]
                val point = android.graphics.PointF(indexFingerTip.x, indexFingerTip.y)

                // Lưu lại kết quả phát hiện
                rawHandX = point.x
                rawHandY = point.y

                // Log vị trí ngón trỏ từ MediaPipe (chuẩn hóa 0-1)
                Log.d(TAG, "MediaPipe ngón trỏ: x=${point.x}, y=${point.y}")

                // Map to screen coordinates and draw
                val screenPoint = mapToScreenCoordinates(point)

                // Log vị trí ngón trỏ trên màn hình
                Log.d(TAG, "Màn hình ngón trỏ: x=${screenPoint.x}, y=${screenPoint.y}")

                runOnUiThread {
                    // Luôn cập nhật vị trí, bất kể trạng thái vẽ
                    viewModel.updateHandPosition(screenPoint.x, screenPoint.y, isHandOpen)
                    
                    // Chỉ set drawing = true nếu chế độ vẽ được bật
                    if (isDrawingEnabled) {
                        viewModel.setDrawing(true)
                        Log.d(TAG, "Đang vẽ với tọa độ: (${screenPoint.x}, ${screenPoint.y})")
                    } else {
                        viewModel.setDrawing(false)
                        Log.d(TAG, "Chế độ vẽ không được bật, chỉ tracking vị trí")
                    }
                }
            } else {
                Log.d(TAG, "Không đủ landmarks để xử lý (cần ít nhất 9, có ${landmarks.size})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing hands result", e)
            runOnUiThread {
                viewModel.setDrawing(false)
            }
        }
    }

    private fun mapToScreenCoordinates(point: android.graphics.PointF): android.graphics.PointF {
        // Get the dimensions of the drawing view
        val viewWidth = mBinding.drawingView.width.toFloat()
        val viewHeight = mBinding.drawingView.height.toFloat()

        if (viewWidth <= 0 || viewHeight <= 0) {
            return android.graphics.PointF(0f, 0f)
        }

        // Map the normalized point (0-1) to screen coordinates
        // Camera trước hiển thị ảnh gương, nên cần đảo ngược tọa độ X
        val screenX = if (currentFacing == Facing.FRONT) {
            viewWidth - (point.x * viewWidth)
        } else {
            point.x * viewWidth
        }
        val screenY = point.y * viewHeight

        return android.graphics.PointF(screenX, screenY)
    }

    /**
     * Phương thức chính để phát hiện bàn tay đóng/mở
     */
    private fun detectHandOpenState(landmarks: List<com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark>): Boolean {
        if (landmarks.size < 21) return true

        // Chỉ số của các điểm mốc quan trọng
        val THUMB_TIP = 4     // Đầu ngón cái
        val INDEX_TIP = 8     // Đầu ngón trỏ
        val MIDDLE_TIP = 12   // Đầu ngón giữa
        val RING_TIP = 16     // Đầu ngón áp út
        val PINKY_TIP = 20    // Đầu ngón út

        val THUMB_MCP = 1     // Gốc ngón cái
        val INDEX_MCP = 5     // Gốc ngón trỏ
        val MIDDLE_MCP = 9    // Gốc ngón giữa
        val RING_MCP = 13     // Gốc ngón áp út
        val PINKY_MCP = 17    // Gốc ngón út

        // Tính khoảng cách từ đầu ngón tay đến gốc ngón tay tương ứng
        val indexFingerExtension = distance(landmarks[INDEX_TIP], landmarks[INDEX_MCP])
        val middleFingerExtension = distance(landmarks[MIDDLE_TIP], landmarks[MIDDLE_MCP])
        val ringFingerExtension = distance(landmarks[RING_TIP], landmarks[RING_MCP])
        val pinkyFingerExtension = distance(landmarks[PINKY_TIP], landmarks[PINKY_MCP])

        // Tính tổng độ duỗi của các ngón tay
        val totalExtension = indexFingerExtension + middleFingerExtension + ringFingerExtension + pinkyFingerExtension

        // Ngưỡng để xác định bàn tay đóng hay mở
        val threshold = 0.4f

        // Nếu tổng độ duỗi lớn hơn ngưỡng, coi như bàn tay mở
        return totalExtension > threshold
    }

    /**
     * Tính khoảng cách Euclidean giữa hai điểm mốc
     */
    private fun distance(p1: com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark,
                         p2: com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    private fun flipCamera() {
        currentFacing = if (currentFacing == Facing.FRONT) {
            Facing.BACK
        } else {
            Facing.FRONT
        }

        // Switch camera facing
        mBinding.cameraView.facing = currentFacing

        Toast.makeText(this, "Camera switched", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Camera switched to: ${if (currentFacing == Facing.FRONT) "FRONT" else "BACK"}")
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
            
            // Phát nhạc nếu có bài hát được chọn
            val currentSong = MusicManagerApp.getCurrentSong()
            if (currentSong != null) {
                Log.d(TAG, "Starting music playback for song: ${currentSong.title}")
                MusicManagerApp.playMusic(this) {}
            } else {
                Log.w(TAG, "No song selected for music playback")
            }
            isRecording = true
            // Allow navigation once for this recording session
            hasNavigatedToResult = false
            isDrawingEnabled = true
            recordingStartTime = System.currentTimeMillis()
            
            // Clear any previous drawing
            mBinding.drawingView.clearDrawing()
            
            // Update UI
            updateRecordingUI(true)

            // Initialize and show recording time view
            mBinding.tvTimeRec.text = "00:00"
            mBinding.tvTimeRec.visibility = View.VISIBLE
            
            // Start video recording with CameraView
            mBinding.cameraView.takeVideoSnapshot(outputFile)
            
            // Start recording timer to show elapsed time
            startRecordingTimer()
            
            Toast.makeText(this, "Recording started - Draw with your hand! Press stop to finish", Toast.LENGTH_SHORT).show()
            
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
            
            // Clear drawing
            mBinding.drawingView.clearDrawing()
            
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
        
        // Clear drawing
        mBinding.drawingView.clearDrawing()
        
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

                Hand (raw): ${rawHandX.toInt()}, ${rawHandY.toInt()}
                Hand (converted): ${convertedHandX.toInt()}, ${convertedHandY.toInt()}

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
            // Observe current hand position
            viewModel.currentPoint.observe(this) { point ->
                try {
                    // Only update drawing if drawing is enabled (during recording or manual mode)
                    val shouldDraw = isDrawingEnabled || viewModel.drawingMode.value == true
                    mBinding.drawingView.updateDrawing(point, shouldDraw)

                    // Update converted hand position for debug
                    point?.let {
                        convertedHandX = it.x
                        convertedHandY = it.y

                        // Log the current point position
                        Log.d(TAG, "Hand position in VIEW coordinates: x=${it.x}, y=${it.y}")
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
            // Prevent duplicate setup causing multiple listeners and repeated callbacks
            if (isCameraSetup) {
                Log.d(TAG, "Camera already setup, skipping...")
                return
            }
            // Set initial camera facing
            mBinding.cameraView.facing = currentFacing
            
            // Configure video quality based on screen resolution
            configureVideoQuality()
            
            // Set up frame processor for hand detection
            // Clear any existing processors/listeners before adding new ones
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
                    if (!hasNavigatedToResult) {
                        hasNavigatedToResult = true
                        Routes.startResultActivity(this@HandDetectActivity, outputFile.absolutePath)
                    } else {
                        Log.d(TAG, "Result navigation already performed, skipping duplicate.")
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
                        Toast.makeText(this@HandDetectActivity, "Camera error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            
            // Start the camera
            mBinding.cameraView.open()
            isCameraSetup = true
            
            Log.d(TAG, "Camera setup completed with ${if (currentFacing == Facing.FRONT) "FRONT" else "BACK"} camera")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera", e)
        }
    }

    private fun configureVideoQuality() {
        try {
            // Determine video quality based on screen resolution
            val maxDimension = maxOf(screenHeight, screenWidth)

            when {
                maxDimension <= 1280 -> {
                    // HD (720p) for smaller screens
                    mBinding.cameraView.setVideoSize(SizeSelectors.maxArea(1280 * 720))
                    Log.d(TAG, "Using HD video quality (1280x720)")
                }
                maxDimension <= 1920 -> {
                    // Full HD (1080p) for medium screens
                    mBinding.cameraView.setVideoSize(SizeSelectors.maxArea(1920 * 1080))
                    Log.d(TAG, "Using Full HD video quality (1920x1080)")
                }
                else -> {
                    // 4K for large screens (but limit to 1080p for performance)
                    mBinding.cameraView.setVideoSize(SizeSelectors.maxArea(1920 * 1080))
                    Log.d(TAG, "Using Full HD video quality (1920x1080) for large screen")
                }
            }

            // Note: CameraView automatically handles preview size based on video size
            // No need to set previewSize separately

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring video quality", e)
        }
    }

    private fun processFrame(frame: Frame) {
        try {
            frameSkipCounter++
            if (frameSkipCounter % FRAME_SKIP_RATE != 0) return

            // Check memory usage periodically
            if (frameSkipCounter % 30 == 0) {
                checkMemoryUsage()
            }

            // Get frame data synchronously within the frame processor
            val data = frame.getData<ByteArray>()
            frameWidth = frame.size.width
            frameHeight = frame.size.height
            cameraRotation = frame.rotationToUser

            // Only process if we have valid dimensions
            if (frameWidth <= 0 || frameHeight <= 0) return

            // Create a smaller copy of the data to reduce memory usage
            val scaledWidth = frameWidth / 2
            val scaledHeight = frameHeight / 2
            val dataCopy = scaleDownFrameData(data, frameWidth, frameHeight, scaledWidth, scaledHeight)
            val width = scaledWidth
            val height = scaledHeight
            val rotation = cameraRotation

            cameraExecutor.execute {
                try {
                    // Convert the scaled data to bitmap for MediaPipe
                    val bitmap = convertByteArrayToBitmap(dataCopy, width, height, rotation)
                    
                    if (bitmap != null && hands != null) {
                        // Send to MediaPipe Hands
                        hands?.send(bitmap, System.currentTimeMillis())
                        
                        // Debug: Log frame processing
                        if (frameSkipCounter % 10 == 0) {
                            Log.d(TAG, "Gửi frame tới MediaPipe: ${bitmap.width}x${bitmap.height}")
                        }
                        
                        bitmap.recycle() // Clean up the bitmap
                    } else if (bitmap != null) {
                        bitmap.recycle() // Clean up if MediaPipe not available
                    } else {
                        Log.w(TAG, "Không thể tạo bitmap từ frame data")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing frame data", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing frame", e)
        }
    }

    private fun scaleDownFrameData(data: ByteArray, originalWidth: Int, originalHeight: Int, 
                                 targetWidth: Int, targetHeight: Int): ByteArray {
        try {
            // Create YuvImage from original data
            val yuvImage = YuvImage(data, ImageFormat.NV21, originalWidth, originalHeight, null)
            val out = ByteArrayOutputStream()
            
            // Compress to JPEG with lower quality and smaller size
            yuvImage.compressToJpeg(Rect(0, 0, originalWidth, originalHeight), 70, out)
            val imageBytes = out.toByteArray()
            
            // Decode to bitmap
            val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            if (originalBitmap != null) {
                // Scale down the bitmap
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                originalBitmap.recycle()
                
                // Convert back to byte array
                val scaledOut = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, scaledOut)
                scaledBitmap.recycle()
                
                return scaledOut.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scaling frame data", e)
        }
        
        // Fallback: return original data if scaling fails
        return data
    }

    private fun convertByteArrayToBitmap(data: ByteArray, width: Int, height: Int, rotation: Int): Bitmap? {
        return try {
            // Use BitmapFactory.Options to reduce memory usage
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Reduce memory usage by sampling
                inPreferredConfig = Bitmap.Config.ARGB_8888 // Use less memory per pixel
            }
            
            // Decode directly from byte array
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            
            if (bitmap != null) {
                // Rotate if needed
                if (rotation != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat())
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    bitmap.recycle()
                    rotatedBitmap
                } else {
                    bitmap
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting byte array to bitmap", e)
            null
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
        // Disable keep screen on when app is not visible
        setKeepScreenOn(false)
    }

    override fun onResume() {
        super.onResume()
        // Open camera when app becomes visible
        if (allPermissionsGranted()) {
            mBinding.cameraView.open()
        }
        // Enable keep screen on when app is visible
        setKeepScreenOn(true)

        // Làm mới đường dẫn nhạc local và cập nhật tiêu đề nhạc đang chọn
        MusicManagerApp.refreshLocalMusicPaths(this)
        
        // Reset tất cả state về ban đầu khi quay về từ màn hình result
        resetToInitialState()
        
        mBinding.tvFilterNameTop.text = MusicManagerApp.getCurrentSong()?.title ?: "No song selected"
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
        isDrawingEnabled = true // Giữ mặc định là true cho HandDetect
        
        // Reset navigation guard
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
        
        // Close camera and destroy
        // Clear listeners/processors to avoid leaks or duplicate callbacks on next init
        mBinding.cameraView.clearFrameProcessors()
        mBinding.cameraView.clearCameraListeners()
        mBinding.cameraView.close()
        mBinding.cameraView.destroy()
        isCameraSetup = false
        
        // Shutdown executor
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        
        // Clean up MediaPipe Hands
        try {
            hands?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing MediaPipe Hands", e)
        }
        
        // Force garbage collection to free memory
        System.gc()
    }

    private fun checkMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory * 100 / maxMemory).toInt()
        
        Log.d(TAG, "Memory usage: $memoryUsagePercent% (${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB)")
        
        if (memoryUsagePercent > 80) {
            Log.w(TAG, "High memory usage detected, forcing garbage collection")
            System.gc()
        }
    }
}