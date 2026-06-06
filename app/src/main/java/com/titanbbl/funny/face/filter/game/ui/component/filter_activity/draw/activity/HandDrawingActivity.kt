package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.CountDownTimer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityHandDrawingBinding
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.component.music.SelectMusicActivity
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.titanbbl.funny.face.filter.game.utils.Routes
import com.bumptech.glide.Glide
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HandDrawingActivity : BaseActivity<ActivityHandDrawingBinding>() {

    companion object {
        private const val TAG = "HandDrawingActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val DEFAULT_RECORDING_DURATION = 30 // 30 seconds default
    }

    // Camera and recording
    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false
    private var isFrontCamera = true
    
    // Timer variables
    private var countDownTimer: CountDownTimer? = null
    private var recordingTimer: CountDownTimer? = null
    private var recordingStartTime = 0L
    private var recordingElapsedTime = 0L
    private var selectedTimerDuration = 3 // Default 3 seconds timer
    private var selectedRecordingDuration = DEFAULT_RECORDING_DURATION // Default recording duration
    
    // Drawing state
    private var isDrawing = false
    private var isDrawingEnabled = false
    private var lastX = 0f
    private var lastY = 0f

    // Output file for video
    private val outputFile: File by lazy { createVideoFile(this) }

    // Guards to prevent duplicate navigation and repeated listener registration
    private var hasNavigatedToResult = false
    private var isCameraSetup = false

    // ActivityResultLauncher để nhận kết quả chọn nhạc
    private val musicSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Cập nhật tiêu đề nhạc ngay lập tức khi chọn xong
            updateMusicTitle()
            Log.d(TAG, "Music selection completed, updated title")
        }
    }

    private val cameraListener = object : CameraListener() {
        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            Log.e(TAG, "Camera error: ${exception.message}")
            Toast.makeText(this@HandDrawingActivity, "Camera error occurred", Toast.LENGTH_SHORT).show()
        }

        override fun onCameraOpened(options: CameraOptions) {
            super.onCameraOpened(options)
            Log.d(TAG, "Camera opened successfully")
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            if (!hasNavigatedToResult) {
                hasNavigatedToResult = true
                Routes.startResultActivity(this@HandDrawingActivity, outputFile.absolutePath)
            }
            // Reset UI after video is saved
            resetRecordingState()
        }
    }

    override fun getLayoutActivity(): Int = R.layout.activity_hand_drawing

    override fun initViews() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initialize timer text
        mBinding.tvTimer.text = "${selectedTimerDuration}s"
        mBinding.tvTimeRec.text = "00:00"
        
        // Khởi tạo MusicManagerApp
        MusicManagerApp.init(this)
        
        // Request permissions and start camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        
        setupDrawingView()
    }

    private fun startCamera() {
        try {
            if (isCameraSetup) {
                Log.d(TAG, "Camera already setup, skipping addListener")
            }
            mBinding.cameraView.previewFrameRate = 40.0f
            mBinding.cameraView.setLifecycleOwner(this)
            mBinding.cameraView.facing = if (isFrontCamera) Facing.FRONT else Facing.BACK
            mBinding.cameraView.mode = Mode.VIDEO
            mBinding.cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)
            if (!isCameraSetup) {
                mBinding.cameraView.clearCameraListeners()
                mBinding.cameraView.addCameraListener(cameraListener)
                isCameraSetup = true
            }
            
            if (!mBinding.cameraView.isOpened) {
                mBinding.cameraView.open()
            }
            
            Log.d(TAG, "Camera started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera", e)
            Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDrawingView() {
        // Set up touch listener for drawing
        mBinding.cameraView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isDrawingEnabled) {
                        isDrawing = true
                        lastX = event.x
                        lastY = event.y
                        mBinding.drawingView.updateDrawing(PointF(event.x, event.y), true)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDrawing && isDrawingEnabled) {
                        lastX = event.x
                        lastY = event.y
                        mBinding.drawingView.updateDrawing(PointF(event.x, event.y), true)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDrawing = false
                    mBinding.drawingView.updateDrawing(PointF(lastX, lastY), false)
                }
            }
            true
        }
    }

    override fun onClickViews() {
        // Back button
        mBinding.btnBack.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
            finish()
        }

        // Close filter button (if exists)
        mBinding.btnCloseFilterTop?.setOnClickListener {
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
                musicSelectionLauncher.launch(Intent(this, SelectMusicActivity::class.java))
            }
        }
    }

    private fun flipCamera() {
        isFrontCamera = !isFrontCamera
        
        // Close current camera
        if (mBinding.cameraView.isOpened) {
            mBinding.cameraView.close()
        }
        
        // Restart camera with new facing
        mBinding.cameraView.facing = if (isFrontCamera) Facing.FRONT else Facing.BACK
        mBinding.cameraView.open()
        
        Toast.makeText(this, "Camera switched", Toast.LENGTH_SHORT).show()
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
        mBinding.tvCountdown.visibility = View.VISIBLE
        Glide.with(this).load(R.drawable.ic_record_stop).into(mBinding.btnRecord)
        mBinding.tvCountdown.text = selectedTimerDuration.toString()
        
        countDownTimer = object : CountDownTimer((selectedTimerDuration * 1000 +1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                mBinding.tvCountdown.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                // Hide countdown timer when recording starts
                mBinding.tvCountdown.visibility = View.GONE
                startRecording()
            }
        }.start()
    }

    private fun startRecording() {
        try {
            // Reset tất cả state trước khi bắt đầu recording mới
            resetRecordingState()
            
            // Start music playback if a song is selected
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
                
                MusicManagerApp.playMusic(this) {}
            } else {
                Log.w(TAG, "No song selected for music playback")
            }
            isRecording = true
            hasNavigatedToResult = false
            isDrawingEnabled = true
            recordingStartTime = System.currentTimeMillis()
            mBinding.tvTimeRec.text = "00:00"
            
            updateRecordingUI(true)
            mBinding.cameraView.takeVideoSnapshot(outputFile)
            startRecordingTimer()
            
            Toast.makeText(this, "Recording started - Press stop to finish", Toast.LENGTH_SHORT).show()
            
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
            
            countDownTimer?.cancel()
            recordingTimer?.cancel()
            
            if (mBinding.cameraView.isTakingVideo) {
                mBinding.cameraView.stopVideo()
            }
            
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
            mBinding.btnRecord.alpha = 0.8f
            mBinding.filterHeaderTop?.alpha = 0.9f
        } else {
            mBinding.btnRecord.alpha = 1.0f
            mBinding.filterHeaderTop?.alpha = 1.0f
        }
    }

    private fun resetRecordingState() {
        isRecording = false
        // Re-enable manual drawing after recording stops
        isDrawingEnabled = true
        
        countDownTimer?.cancel()
        recordingTimer?.cancel()
        
        // Reset timer variables
        recordingStartTime = 0L
        recordingElapsedTime = 0L
        
        updateRecordingUI(false)
        mBinding.tvTimer.text = "${selectedTimerDuration}s"
        mBinding.tvTimeRec.text = "00:00"
        mBinding.tvCountdown.visibility = View.GONE
        mBinding.drawingView.clearDrawing()
        
        // Reset navigation guard
        hasNavigatedToResult = false
        
        Log.d(TAG, "Recording state reset completed")
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Camera lifecycle is managed automatically
        // Refresh local music paths and update current song title
        MusicManagerApp.refreshLocalMusicPaths(this)
        
        // Reset tất cả state về ban đầu khi quay về từ màn hình result
        resetToInitialState()
        
        // Cập nhật tiêu đề nhạc đang chọn
        updateMusicTitle()
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        countDownTimer?.cancel()
        recordingTimer?.cancel()
        
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        
        if (mBinding.cameraView.isOpened) {
            mBinding.cameraView.clearCameraListeners()
            mBinding.cameraView.close()
        }
    }
    
    /**
     * Cập nhật tiêu đề nhạc và refresh local paths
     */
    private fun updateMusicTitle() {
        try {
            // Refresh local music paths để đảm bảo có đường dẫn mới nhất
            MusicManagerApp.refreshLocalMusicPaths(this)
            
            // Cập nhật tiêu đề nhạc
            val currentSong = MusicManagerApp.getCurrentSong()
            mBinding.tvFilterNameTop.text = currentSong?.title ?: "No song selected"
            
            Log.d(TAG, "Music title updated to: ${currentSong?.title ?: "No song selected"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating music title", e)
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
        
        // Reset UI alpha
        updateRecordingUI(false)
        
        Log.d(TAG, "Reset to initial state completed")
    }
}