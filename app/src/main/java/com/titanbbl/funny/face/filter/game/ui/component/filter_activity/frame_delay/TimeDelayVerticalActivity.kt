package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.frame_delay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityTimeDelayVerticalBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.bases.ext.click
import com.titanbbl.funny.face.filter.game.ui.component.result.ResultActivity
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.ui.component.music.SelectMusicActivity
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TimeDelayVerticalActivity : BaseActivity<ActivityTimeDelayVerticalBinding>() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val CAPTURE_DELAY_MS = 3000L // 3 seconds between captures
    }

    private lateinit var countdownText: TextView
    private lateinit var recordingTimeText: TextView

    private var isRecording = false
    private var countdownTimer: CountDownTimer? = null
    private var captureTimer: CountDownTimer? = null
    private var isFrontCamera = true
    private var isSettingsOpen = false
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    
    // Settings
    private var selectedDuration = 3 // Default 3 seconds delay between photos
    private var selectedTimerDuration = 3 // Default 3 seconds timer
    private var selectedRecordingDuration = 60 // Default 60 seconds recording duration
    
    // Auto capture variables
    private var currentFrameIndex = 0
    private var isAutoCaptureRunning = false
    private var isCapturingCompleted = false
    
    // Recording time variables
    private var recordingTimer: CountDownTimer? = null
    private var recordingStartTime = 0L
    private var recordingElapsedTime = 0L
    private var totalRecordingTimer: CountDownTimer? = null
    private val outputFile: File by lazy { createVideoFile(this) }
    
    private var cameraListener = object : CameraListener() {
        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
        }

        override fun onCameraOpened(options: CameraOptions) {
            super.onCameraOpened(options)
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            // Handle the video result and navigate to ResultActivity
            val file = result.file
            if (file != null && file.exists()) {
                navigateToResultActivity(file.absolutePath)
            }
        }

        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            takePhoto(result)
        }
    }

    override fun getLayoutActivity(): Int = R.layout.activity_time_delay_vertical

    override fun initViews() {
        countdownText = mBinding.tvCountdown
        recordingTimeText = mBinding.tvTimeRec
        
        // Initialize the recording duration display
        mBinding.flTimeControl1m.text = formatDurationText(selectedRecordingDuration)
        
        // Initialize selected music title
        mBinding.tvNameMusic.text = MusicManagerApp.getCurrentSong()?.title ?: "No song selected"

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request permissions
        if (allPermissionsGranted()) {
            startCamera()
            setupClickListeners()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        mBinding.cameraView.setLifecycleOwner(this)
        mBinding.cameraView.facing = Facing.FRONT
        mBinding.cameraView.mode = Mode.VIDEO
        mBinding.cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)
        mBinding.cameraView.addCameraListener(cameraListener)
        if (!mBinding.cameraView.isOpened) {
            mBinding.cameraView.open()
        }
    }

    private fun setupClickListeners() {
        // Back button
        mBinding.ivBack.click {
            finish()
        }
        
        // Close filter button
        mBinding.ivClose.click {
            MusicManagerApp.stopMusic()

            // Xóa bài hát đã chọn
            MusicManagerApp.clearSelectedSong()

            // Đổi text thành "thêm âm thanh"
            mBinding.tvNameMusic.text = getString(R.string.add_song)
        }

        // Start/stop auto capture when screen is clicked
        mBinding.flRecord.click {
            handleCaptureToggle()
        }
        
        // Open music selection screen when tapping the music title
        mBinding.tvNameMusic.click {
            if (isAutoCaptureRunning) {
                Toast.makeText(this, "Cannot change music while recording", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, SelectMusicActivity::class.java))
            }
        }
        
        // Apply settings button
        mBinding.btnApplySettings.click {
            applySettings()
        }
        
        // (Deprecated UI) Duration radio buttons no longer used; we toggle via the icon
        
        // (Deprecated) Recording duration radios kept for compatibility but not used for auto-stop
    }

    private fun handleCaptureToggle() {
        when {
            isAutoCaptureRunning -> {
                stopAutoCapture()
                Toast.makeText(this, "Stopped taking photos", Toast.LENGTH_SHORT).show()
            }

            isCapturingCompleted -> {
                resetCapture()
                startAutoCapture()
                Toast.makeText(this, "Start a new photo shoot", Toast.LENGTH_SHORT).show()
            }

            else -> {
                startAutoCapture()
                Toast.makeText(this, "Start Photography", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetCapture() {
        currentFrameIndex = 0
        isAutoCaptureRunning = false
        isCapturingCompleted = false

        enableSettingsButtons()

        // Sử dụng method mới để reset ảnh
        resetAllImages()

        countdownText.visibility = View.GONE
        recordingTimeText.visibility = View.GONE
    }

    private fun startAutoCapture() {
        isAutoCaptureRunning = true
        isCapturingCompleted = false
        currentFrameIndex = 0
        mBinding.flRecord.setImageResource(R.drawable.ic_record_stop)

        disableSettingsButtons()
        startRecordingTimer()
        startInitialCountdown()
        // Start recording without fixed duration; user will stop manually
        mBinding.cameraView.takeVideoSnapshot(outputFile)
        
        // Start background music if a song is selected
        MusicManagerApp.getCurrentSong()?.let {
            MusicManagerApp.playMusic(this) {}
        }
    }
    
    private fun stopAutoCapture() {
        isAutoCaptureRunning = false

        // Cancel all timers
        captureTimer?.cancel()
        countdownTimer?.cancel()
        recordingTimer?.cancel()
        totalRecordingTimer?.cancel()
        captureTimer = null
        countdownTimer = null
        recordingTimer = null
        totalRecordingTimer = null
        mBinding.flRecord.setImageResource(R.drawable.ic_record)
        
        enableSettingsButtons()
        stopRecordingTimer()
        countdownText.visibility = View.GONE
        countdownText.removeCallbacks(null)
        mBinding.cameraView.stopVideo()
        
        // Stop background music
        MusicManagerApp.stopMusic()
    }
    
    private fun startInitialCountdown() {
        countdownText.visibility = View.VISIBLE
        countdownText.text = "3"

        countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                countdownText.text = secondsRemaining.toString()
            }
            
            override fun onFinish() {
                countdownText.visibility = View.GONE
                mBinding.cameraView.takePictureSnapshot()
            }
        }.start()
    }
    


    private fun takePhoto(result: PictureResult) {
        val currentFrame = when (currentFrameIndex) {
            0 -> mBinding.imageView1
            1 -> mBinding.imageView2
            2 -> mBinding.imageView3
            3 -> mBinding.imageView4
            else -> return
        }
        
        result.toBitmap { bitmap ->
            bitmap?.let {
                val croppedBitmap = getCroppedBitmapForView(it, currentFrame)
                currentFrame.setImageBitmap(croppedBitmap)
                currentFrameIndex++
                
                if (currentFrameIndex >= 4) {
                    Toast.makeText(this, "All 4 photos captured! Recording continues...", Toast.LENGTH_SHORT).show()
                    isCapturingCompleted = true
                } else if (isAutoCaptureRunning) {
                    scheduleNextCapture()
                }
            }
        }
    }

    private fun getCroppedBitmapForView(sourceBitmap: Bitmap, targetView: View): Bitmap {
        // Get the coordinates of the frame in the screen
        val frameRect = Rect()
        targetView.getGlobalVisibleRect(frameRect)

        // Get the coordinates of the camera preview in the screen
        val previewRect = Rect()
        mBinding.cameraView.getGlobalVisibleRect(previewRect)

        // Calculate the relative position of the frame within the preview
        val relativeLeft = frameRect.left - previewRect.left
        val relativeTop = frameRect.top - previewRect.top

        // Calculate the scaling factors between the preview view and the actual bitmap
        val scaleX = sourceBitmap.width.toFloat() / mBinding.cameraView.width
        val scaleY = sourceBitmap.height.toFloat() / mBinding.cameraView.height

        // Calculate the coordinates in the bitmap
        val left = (relativeLeft * scaleX).toInt()
        val top = (relativeTop * scaleY).toInt()
        val width = (frameRect.width() * scaleX).toInt()
        val height = (frameRect.height() * scaleY).toInt()

        // Ensure the coordinates are within the bitmap bounds
        val safeLeft = left.coerceIn(0, sourceBitmap.width - 1)
        val safeTop = top.coerceIn(0, sourceBitmap.height - 1)
        val safeWidth = width.coerceAtMost(sourceBitmap.width - safeLeft)
        val safeHeight = height.coerceAtMost(sourceBitmap.height - safeTop)

        // Create a cropped bitmap
        return try {
            Bitmap.createBitmap(
                sourceBitmap, safeLeft, safeTop, safeWidth, safeHeight
            )
        } catch (e: Exception) {
            // If cropping fails, return the original bitmap
            sourceBitmap
        }
    }
    

    
    private fun scheduleNextCapture() {
        if (!isAutoCaptureRunning) return
        
        countdownText.visibility = View.VISIBLE
        countdownText.text = selectedDuration.toString()
        
        val delayMs = (selectedDuration * 1000).toLong()
        captureTimer = object : CountDownTimer(delayMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                countdownText.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                countdownText.visibility = View.GONE
                mBinding.cameraView.takePictureSnapshot()
            }
        }.start()
    }
    
    private fun startTotalRecordingTimer() {
        val totalDurationMs = selectedRecordingDuration * 1000L
        
        totalRecordingTimer = object : CountDownTimer(totalDurationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Just keep track of time, no UI updates needed here
            }
            
            override fun onFinish() {
                if (isAutoCaptureRunning) {
                    isCapturingCompleted = true
                    stopAutoCapture()
                    Toast.makeText(this@TimeDelayVerticalActivity, 
                        "Recording completed (${formatDurationText(selectedRecordingDuration)})", 
                        Toast.LENGTH_LONG).show()
                    showCompletionDialog()
                }
            }
        }.start()
    }
    
    private fun startRecordingTimer() {
        recordingStartTime = System.currentTimeMillis()
        recordingElapsedTime = 0L
        recordingTimeText.visibility = View.VISIBLE
        recordingTimeText.text = "00:00"
        
        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 100) {
            override fun onTick(millisUntilFinished: Long) {
                recordingElapsedTime = System.currentTimeMillis() - recordingStartTime
                updateRecordingTimeDisplay()
            }
            
            override fun onFinish() {
            }
        }.start()
    }
    
    private fun stopRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = null
        recordingTimeText.visibility = View.GONE
    }
    
    private fun updateRecordingTimeDisplay() {
        val totalSeconds = recordingElapsedTime / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        recordingTimeText.text = timeString
    }
    
    private fun formatDurationText(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds % 60 == 0 -> "${seconds / 60}m"
            else -> "${seconds / 60}m${seconds % 60}s"
        }
    }
    
    private fun disableSettingsButtons() {
        mBinding.flCameraSwitch.alpha = 0.3f
        mBinding.flTimeControl3sContainer.alpha = 0.3f
        mBinding.flTimerContainer.alpha = 0.3f
        mBinding.flCameraSwitch.isClickable = false
        mBinding.flTimeControl3sContainer.isClickable = false
        mBinding.flTimerContainer.isClickable = false
    }
    
    private fun enableSettingsButtons() {
        mBinding.flCameraSwitch.alpha = 1.0f
        mBinding.flTimeControl3sContainer.alpha = 1.0f
        mBinding.flTimerContainer.alpha = 1.0f
        mBinding.flCameraSwitch.isClickable = true
        mBinding.flTimeControl3sContainer.isClickable = true
        mBinding.flTimerContainer.isClickable = true
    }

    private fun flipCamera() {
        isFrontCamera = !isFrontCamera
        mBinding.cameraView.toggleFacing()
    }
    
    private fun toggleSettingsBottomSheet() {
        isSettingsOpen = !isSettingsOpen
        mBinding.clSettingsBottomSheet.visibility = if (isSettingsOpen) View.VISIBLE else View.GONE
    }
    
    private fun toggleDelaySettings() {
        isSettingsOpen = !isSettingsOpen
        mBinding.clSettingsBottomSheet.visibility = if (isSettingsOpen) View.VISIBLE else View.GONE
        
        when (selectedDuration) {
            3 -> mBinding.rb3s.isChecked = true
            5 -> mBinding.rb5s.isChecked = true
            10 -> mBinding.rb10s.isChecked = true
        }
    }
    
    private fun toggleTimerSettings() {
        isSettingsOpen = !isSettingsOpen
        mBinding.clSettingsBottomSheet.visibility = if (isSettingsOpen) View.VISIBLE else View.GONE
        
        when (selectedRecordingDuration) {
            60 -> mBinding.rb60s.isChecked = true
            90 -> mBinding.rb90s.isChecked = true
            120 -> mBinding.rb2m.isChecked = true
            300 -> mBinding.rb5m.isChecked = true
        }
    }
    
    private fun applySettings() {
        isSettingsOpen = false
        mBinding.clSettingsBottomSheet.visibility = View.GONE
        
        mBinding.flTimeControl3s.text = "${selectedDuration}s"
        mBinding.flTimeControl1m.text = formatDurationText(selectedRecordingDuration)
        
        Toast.makeText(this, 
            "Settings updated: Delay ${selectedDuration}s, Recording ${formatDurationText(selectedRecordingDuration)}", 
            Toast.LENGTH_SHORT).show()
    }
    private fun showCompletionDialog() {
        val message = if (currentFrameIndex >= 4) {
            "Đã chụp xong 4 ảnh và hoàn thành quay video! Đang chuyển sang màn hình kết quả."
        } else {
            "Đã hoàn thành quay video với ${currentFrameIndex} ảnh! Đang chuyển sang màn hình kết quả."
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Navigate to ResultActivity if the output file exists
        if (outputFile.exists()) {
            navigateToResultActivity(outputFile.absolutePath)
        }
    }
    
    private fun navigateToResultActivity(videoPath: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("video_path", videoPath)
        }
        startActivity(intent)
        finish() // Optional: finish this activity if you don't want to return to it
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
                setupClickListeners()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    override fun onResizeViews() {
        // No resize needed
    }

    override fun onClickViews() {
        mBinding.flCameraSwitch.click {
            if (!isAutoCaptureRunning) {
                flipCamera()
            } else {
                Toast.makeText(this, "Cannot switch camera while recording", Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.flTimeControl3sContainer.click {
            if (!isAutoCaptureRunning) {
                // Toggle delay 3s -> 5s -> 10s -> 3s
                selectedDuration = when (selectedDuration) {
                    3 -> 5
                    5 -> 10
                    10 -> 3
                    else -> 3
                }
                mBinding.flTimeControl3s.text = "${selectedDuration}s"
                Toast.makeText(this, "Delay set to ${selectedDuration}s", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot change settings while recording", Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.flTimerContainer.click {
            if (!isAutoCaptureRunning) {
                toggleTimerSettings()
            } else {
                Toast.makeText(this, "Cannot change settings while recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun observerData() {
    }

    override fun onResume() {
        super.onResume()
        // Refresh local music paths and update music title
        MusicManagerApp.refreshLocalMusicPaths(this)
        mBinding.tvNameMusic.text = MusicManagerApp.getCurrentSong()?.title ?: "No song selected"
        
        // Reset tất cả các ảnh khi quay lại từ màn Result
        resetAllImages()
        
        // Reset trạng thái capture
        resetCaptureState()
    }

    /**
     * Reset tất cả các ảnh 1,2,3,4 thành ảnh trong suốt
     */
    private fun resetAllImages() {
        try {
            mBinding.imageView1.setImageBitmap(null)
            mBinding.imageView2.setImageBitmap(null)
            mBinding.imageView3.setImageBitmap(null)
            mBinding.imageView4.setImageBitmap(null)
            
            // Đặt background trong suốt để đảm bảo không có ảnh cũ
            mBinding.imageView1.setBackgroundResource(android.R.color.transparent)
            mBinding.imageView2.setBackgroundResource(android.R.color.transparent)
            mBinding.imageView3.setBackgroundResource(android.R.color.transparent)
            mBinding.imageView4.setBackgroundResource(android.R.color.transparent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reset trạng thái capture về ban đầu
     */
    private fun resetCaptureState() {
        try {
            currentFrameIndex = 0
            isAutoCaptureRunning = false
            isCapturingCompleted = false
            
            // Reset UI elements
            mBinding.flRecord.setImageResource(R.drawable.ic_record)
            countdownText.visibility = View.GONE
            recordingTimeText.visibility = View.GONE
            
            // Enable settings buttons
            enableSettingsButtons()
            
            // Cancel all timers
            countdownTimer?.cancel()
            captureTimer?.cancel()
            recordingTimer?.cancel()
            totalRecordingTimer?.cancel()
            countdownTimer = null
            captureTimer = null
            recordingTimer = null
            totalRecordingTimer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAutoCaptureRunning) {
            stopAutoCapture()
        }
        MusicManagerApp.stopMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        captureTimer?.cancel()
        recordingTimer?.cancel()
        totalRecordingTimer?.cancel()
        cameraExecutor.shutdown()
        MusicManagerApp.stopMusic()
        
        // Reset ảnh khi destroy activity
        resetAllImages()
    }
}