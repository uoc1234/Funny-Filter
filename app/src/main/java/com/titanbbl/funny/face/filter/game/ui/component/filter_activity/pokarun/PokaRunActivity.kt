package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.pokarun

import android.Manifest
import android.animation.Animator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieImageAsset
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityPokaRunBinding

import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.bases.ext.click
import com.titanbbl.funny.face.filter.game.ui.bases.ext.goneView
import com.titanbbl.funny.face.filter.game.ui.bases.ext.visibleView
import com.titanbbl.funny.face.filter.game.ui.component.music.SelectMusicActivity
import com.titanbbl.funny.face.filter.game.ui.component.result.ResultActivity
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.titanbbl.funny.face.filter.game.utils.Routes
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.java
import kotlin.random.Random

class PokaRunActivity : BaseActivity<ActivityPokaRunBinding>() {
    
    companion object {
        private const val TAG = "PokaRunActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        private const val DEFAULT_RECORDING_DURATION = 30 // 30 seconds default
    }

    // Pose detection
    private lateinit var poseDetectionExecutor: ExecutorService
    private var isProcessingFrame = false
    private var currentScore = 1000
    
    private var isGameRunning = false
    private var isAnimationPlaying = false
    
    // Timer variables
    private var recordingTimer: CountDownTimer? = null
    private var recordingStartTime = 0L
    private var recordingElapsedTime = 0L
    private var selectedRecordingDuration = DEFAULT_RECORDING_DURATION // Default recording duration
    
    // UI elements for tracking
    private var scoreLayoutOriginalParams: ConstraintLayout.LayoutParams? = null
    private var scoreLayoutInitialized = false
    
    private val poseDetector by lazy {
        Log.d(TAG, "Initializing pose detector")
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "All permissions granted, setting up camera")
            setupCamera()
        } else {
            Log.e(TAG, "Camera permissions not granted")
            finish()
        }
    }
    
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

    override fun getLayoutActivity(): Int = R.layout.activity_poka_run

    override fun initViews() {
        super.initViews()

        Log.e(TAG, "POKEMON ACTIVITY INIT VIEWS STARTED")
        mBinding.tvScore.text = getString(R.string.dp_score, Random.nextInt(10000))

        // Initialize executors
        poseDetectionExecutor = Executors.newSingleThreadExecutor()
        
        // Force score layout to be visible
        mBinding.layoutTvScore.visibility = View.VISIBLE
        
        // Store original layout params
        scoreLayoutOriginalParams = mBinding.layoutTvScore.layoutParams as? ConstraintLayout.LayoutParams
        
        // Check permissions and setup camera
        checkAndRequestPermissions()
        
        // Setup camera listener with video result handling
        mBinding.cameraView.addCameraListener(object : CameraListener() {

            override fun onCameraError(exception: CameraException) {
                super.onCameraError(exception)
                Log.e(TAG, "Camera error: ${exception.message}")
            }

            override fun onCameraOpened(options: CameraOptions) {
                super.onCameraOpened(options)
                Log.d(TAG, "Camera opened successfully")
                
                // Ensure score layout is visible after camera is opened
                mBinding.layoutTvScore.visibility = View.VISIBLE
                Log.d(TAG, "onCameraOpened: Forced overlay and score layout to VISIBLE")
            }

            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)
                Log.d(TAG, "Picture taken successfully")

                try {
                    Log.d(TAG, "Ảnh đã được chụp, bắt đầu xử lý")

                    // Kiểm tra thư mục images có tồn tại không
                    if (!::imagesDir.isInitialized || !imagesDir.exists()) {
                        Log.e(TAG, "Thư mục images chưa được khởi tạo hoặc không tồn tại")
                        return
                    }

                    // Tạo file ảnh với tên img_1.png
                    val imageFile = File(imagesDir, "img_1.png")

                    if (imageFile.exists()) {
                        imageFile.delete()
                    }

                    // Lấy dữ liệu ảnh
                    val data = result.data
                    // Decode -> resize 358x638 -> save PNG
                    val decodedBitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    if (decodedBitmap != null) {
                        val targetWidth = 358
                        val targetHeight = 638
                        val resizedBitmap = Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
                        FileOutputStream(imageFile).use { output ->
                            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                            output.flush()
                        }
                        decodedBitmap.recycle()
                        if (!resizedBitmap.isRecycled) resizedBitmap.recycle()
                    } else {
                        // Fallback: save original bytes if decoding fails
                        FileOutputStream(imageFile).use { output ->
                            output.write(data)
                            output.flush()
                        }
                    }

                    Log.d(
                        TAG,
                        "Đã lưu ảnh thành công: ${imageFile.absolutePath}, kích thước: ${imageFile.length()} bytes"

                    )
                    mBinding.animationView.playAnimation()
                    mBinding.cameraView.takeVideoSnapshot(outputFile)

                    // Cập nhật JSON để sử dụng ảnh mới
//                        updateJsonWithNewImage(imageFile)

                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi xử lý ảnh đã chụp", e)
                }
            }
            
            override fun onVideoTaken(result: VideoResult) {
                super.onVideoTaken(result)
                Log.d(TAG, "Video recording completed")
                val videoFile = result.file
                if (videoFile.exists()) {
                    Routes.startResultActivity(this@PokaRunActivity, videoFile.absolutePath)
                }
            }
        })
        
        // Setup frame processor for pose detection
        mBinding.cameraView.addFrameProcessor(frameProcessor)

        // Khởi tạo MusicManagerApp
        MusicManagerApp.init(this)
        
        // Make music header visible for user interaction
        mBinding.filterHeaderTop.visibleView()
        
        // Setup animation files first
        Log.e(TAG, "POKEMON CALLING SETUP ANIMATION FILES")
        setupAnimationFiles()

        // Khởi tạo animation ngay sau khi setup files
        if (animationFilesExtracted) {
            initLottieAnimation()
        }
        mBinding.cameraView.facing = Facing.BACK
    }

    private lateinit var animationDir: File
    private lateinit var dataJsonFile: File
    private lateinit var imagesDir: File
    private var animationFilesExtracted = false

    // Add output file for video recording
    private val outputFile: File by lazy { createVideoFile(this) }

    /**
     * Setup animation files - chỉ đọc file đã được giải nén từ màn hình loading
     */
    private fun setupAnimationFiles() {
        try {
            // Thiết lập đường dẫn thư mục anim
            val animDir = File(filesDir, "anim")
            if (!animDir.exists()) {
                Log.e(TAG, "POKEMON ANIMATION DIRECTORY NOT FOUND - FILES NOT EXTRACTED")
                return
            }

            animationDir = animDir

            // Tìm cấu trúc thư mục đã giải nén
            val pokenmomDir = File(animationDir, "pokemom")
            if (pokenmomDir.exists() && pokenmomDir.isDirectory) {
                dataJsonFile = File(pokenmomDir, "data.json")
                imagesDir = File(pokenmomDir, "images")
            } else {
                // Fallback to original paths
                dataJsonFile = File(animationDir, "data.json")
                imagesDir = File(animationDir, "images")
            }

            // Kiểm tra xem các file đã giải nén có tồn tại không
            if (dataJsonFile.exists() && imagesDir.exists() && imagesDir.isDirectory) {
                animationFilesExtracted = true
            } else {
                // Nếu không tìm thấy file, thử tìm alternative paths
                if (!dataJsonFile.exists()) {
                    val possibleJsonFiles = animationDir.listFiles { file ->
                        file.name.endsWith(".json")
                    }

                    if (possibleJsonFiles != null && possibleJsonFiles.isNotEmpty()) {
                        dataJsonFile = possibleJsonFiles[0]
                    }
                }

                if (!imagesDir.exists()) {
                    val possibleImageDirs = animationDir.listFiles { file ->
                        file.isDirectory && (file.name == "images" || file.listFiles()
                            ?.any { it.name.endsWith(".png") } == true)
                    }

                    if (possibleImageDirs != null && possibleImageDirs.isNotEmpty()) {
                        imagesDir = possibleImageDirs[0]
                    }
                }

                // Kiểm tra lại sau khi tìm alternative paths
                if (dataJsonFile.exists() && imagesDir.exists() && imagesDir.isDirectory) {
                    animationFilesExtracted = true
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error reading pokemon animation files", e)
        }
    }


    /**
     * Initialize Lottie animation with extracted files
     */
    private fun initLottieAnimation() {
        try {
            if (!animationFilesExtracted) {
                Log.e(TAG, "CANNOT INITIALIZE POKEMON LOTTIE: FILES NOT EXTRACTED")
                return
            }

            val lottieView = mBinding.animationView

            if (dataJsonFile.exists()) {
                try {
                    // Đọc nội dung file JSON thành string
                    val jsonString = dataJsonFile.readText()

                    // Sử dụng setAnimationFromJson thay vì setAnimation
                    lottieView.setAnimationFromJson(jsonString, "")

                    // Thiết lập ImageAssetDelegate để load images từ thư mục đã giải nén
                    lottieView.setImageAssetDelegate(object : ImageAssetDelegate {
                        override fun fetchBitmap(asset: LottieImageAsset): Bitmap? {
                            try {
                                val imageFile = File(imagesDir, asset.fileName)
                                if (imageFile.exists()) {
                                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                                    if (bitmap != null) {
                                        return bitmap
                                    }
                                }
                                return null
                            } catch (e: Exception) {
                                Log.e(TAG, "ERROR LOADING POKEMON IMAGE: ${asset.fileName}", e)
                                return null
                            }
                        }
                    })

                    // Thiết lập animation listener để biết khi nào load xong
                    lottieView.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                            // Animation bắt đầu
                            isAnimationPlaying = true
                            Log.d(TAG, "Animation started, isAnimationPlaying: $isAnimationPlaying")
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            // Animation kết thúc
                            isAnimationPlaying = false
                            Log.d(TAG, "Animation ended, isAnimationPlaying: $isAnimationPlaying")
                            
                            // Chỉ dừng game khi animation kết thúc và game đang chạy
                            if (isGameRunning) {
                                Log.d(TAG, "Animation ended, stopping game automatically")
                                stopGame()
                            }
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            // Animation bị hủy
                            isAnimationPlaying = false
                            Log.d(TAG, "Animation cancelled, isAnimationPlaying: $isAnimationPlaying")
                        }

                        override fun onAnimationRepeat(animation: Animator) {
                            // Animation lặp lại
                            Log.d(TAG, "Animation repeated, isAnimationPlaying: $isAnimationPlaying")
                        }
                    })

                    // Thiết lập speed cho animation
                    lottieView.speed = 0.5f

                    // Cho phép click vào LottieView sau khi load xong
                    enableLottieViewClick()

                } catch (e: Exception) {
                    Log.e(TAG, "ERROR SETTING UP POKEMON ANIMATION: ${e.message}")
                    e.printStackTrace()
                }

            } else {
                Log.e(TAG, "POKEMON DATA.JSON FILE NOT FOUND")
            }

        } catch (e: Exception) {
            Log.e(TAG, "ERROR INITIALIZING POKEMON LOTTIE ANIMATION", e)
            e.printStackTrace()
        }
    }


    override fun onClickViews() {
        super.onClickViews()
        
        // LottieView click sẽ được enable sau khi animation load xong
        // Không set click listener ở đây
        
        mBinding.btnBack.click {
            Log.d(TAG, "Back button clicked")
            finish()
        }
        
        mBinding.btnRecord.click {
            Log.d(TAG, "Record button clicked, isGameRunning: $isGameRunning")
            if (!isGameRunning) {
                startGame()
            } else {
                stopGame()
            }
        }
        
        mBinding.btnCamera.click {
            // Switch camera facing
            Log.d(TAG, "Camera flip button clicked, current facing: ${mBinding.cameraView.facing}")
            mBinding.cameraView.facing = if (mBinding.cameraView.facing == Facing.FRONT) {
                Facing.BACK
            } else {
                Facing.FRONT
            }
        }
        
        // Debug button to test score layout visibility
        mBinding.filterHeaderTop.click {
            Log.d(TAG, "Filter header clicked - testing score layout")
            testScoreLayoutVisibility()
        }


        mBinding.btnCloseFilterTop.click {
            MusicManagerApp.stopMusic()

            // Xóa bài hát đã chọn
            MusicManagerApp.clearSelectedSong()

            // Đổi text thành "thêm âm thanh"
            mBinding.tvFilterNameTop.text = getString(R.string.add_song)
        }

        
        // Mở chọn nhạc khi nhấn tiêu đề
        mBinding.tvFilterNameTop.click {
            if (isGameRunning) {
                Toast.makeText(this, "Cannot change music while recording", Toast.LENGTH_SHORT).show()
            } else {
                musicSelectionLauncher.launch(Intent(this, SelectMusicActivity::class.java))
            }
        }
    }
    
    private fun testScoreLayoutVisibility() {
        Log.d(TAG, "testScoreLayoutVisibility: Current visibility = ${mBinding.layoutTvScore.visibility}")
        
        if (mBinding.layoutTvScore.visibility != View.VISIBLE) {
            mBinding.layoutTvScore.visibility = View.VISIBLE
            Log.d(TAG, "testScoreLayoutVisibility: Set to VISIBLE")
        } else {
            // Just for testing - toggle position
            val layoutParams = mBinding.layoutTvScore.layoutParams as ConstraintLayout.LayoutParams
            
            // Log current constraints
            Log.d(TAG, "testScoreLayoutVisibility: Current constraints - " +
                    "topToTop=${layoutParams.topToTop}, " +
                    "bottomToBottom=${layoutParams.bottomToBottom}, " +
                    "startToStart=${layoutParams.startToStart}, " +
                    "endToEnd=${layoutParams.endToEnd}, " +
                    "verticalBias=${layoutParams.verticalBias}")
            
            // Toggle vertical position between top, center, and bottom
            when {
                layoutParams.verticalBias < 0.3f -> {
                    layoutParams.verticalBias = 0.5f
                    Log.d(TAG, "testScoreLayoutVisibility: Moved to center")
                }
                layoutParams.verticalBias < 0.7f -> {
                    layoutParams.verticalBias = 0.9f
                    Log.d(TAG, "testScoreLayoutVisibility: Moved to bottom")
                }
                else -> {
                    layoutParams.verticalBias = 0.1f
                    Log.d(TAG, "testScoreLayoutVisibility: Moved to top")
                }
            }
            
            mBinding.layoutTvScore.layoutParams = layoutParams
            Log.d(TAG, "testScoreLayoutVisibility: Changed position constraints, new verticalBias=${layoutParams.verticalBias}")
        }
    }

    
    private val frameProcessor = FrameProcessor { frame ->
        if (!isProcessingFrame) {
            isProcessingFrame = true
            processFrame(frame)
        }
    }

    private fun processFrame(frame: Frame) {
        try {
            // Get frame data
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
                

                
                poseDetector.process(image)
                    .addOnSuccessListener { pose ->

                        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
                        
                        if (nose != null) {

                            // Get view dimensions for mapping
                            val viewWidth = mBinding.cameraView.width.toFloat()
                            val viewHeight = mBinding.cameraView.height.toFloat()
                            
                            if (viewWidth <= 0 || viewHeight <= 0) {

                                return@addOnSuccessListener
                            }
                            
                            // Using simpler coordinate mapping from DrawByNoseActivity
                            val rawNoseX = nose.position.x
                            val rawNoseY = nose.position.y
                            

                            
                            updateScorePosition(rawNoseX, rawNoseY, viewWidth, viewHeight)
                        } else {

                            // Always show score even when no pose detected
                            runOnUiThread {
                                mBinding.layoutTvScore.visibleView()
                            }
                        }
                        isProcessingFrame = false
                    }
                    .addOnFailureListener { e ->

                        isProcessingFrame = false
                    }
            } else {

                isProcessingFrame = false
            }
        } catch (e: Exception) {

            isProcessingFrame = false
        }
    }
    
    private fun updateScorePosition(rawNoseX: Float, rawNoseY: Float, viewWidth: Float, viewHeight: Float) {
        runOnUiThread {
            try {
                val scoreLayout = mBinding.layoutTvScore
                
                Log.d(TAG, "updateScorePosition: Raw nose coordinates: ($rawNoseX, $rawNoseY)")
                
                // Simple coordinate mapping similar to DrawByNoseActivity
                // For front camera, we need to mirror the X coordinate
                var finalX = rawNoseX
                if (mBinding.cameraView.facing == Facing.FRONT) {
                    finalX = viewWidth - rawNoseX
                }
                
                // Y coordinate remains the same
                val finalY = rawNoseY
                

                
                // Ensure score layout is visible
                scoreLayout.visibleView()
                
                // Get score layout dimensions
                val scoreWidth = if (scoreLayout.width > 0) scoreLayout.width else 200 // fallback width
                val scoreHeight = if (scoreLayout.height > 0) scoreLayout.height else 60 // fallback height
                

                
                // Position score layout with bottom center aligned to nose
                val layoutParams = scoreLayout.layoutParams as ConstraintLayout.LayoutParams
                
                // Clear all constraints first - proper way to reset constraints
                layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                layoutParams.startToEnd = ConstraintLayout.LayoutParams.UNSET
                layoutParams.endToStart = ConstraintLayout.LayoutParams.UNSET
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                layoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET
                layoutParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
                layoutParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                
                // Set horizontal center to match nose position
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                
                // Calculate horizontal bias (ensure it's within 0-1 range)
                val horizontalBias = (finalX / viewWidth).coerceIn(0f, 1f)
                layoutParams.horizontalBias = horizontalBias
                

                
                // Position score layout above the head
                // We'll position it 100dp above the nose for better visibility
                val offsetY = 100f // pixels above the nose
                val newY = (finalY - scoreHeight - offsetY).coerceAtLeast(0f)
                val verticalBias = (newY / viewHeight).coerceIn(0f, 1f)
                
                layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.verticalBias = verticalBias
                
                scoreLayout.layoutParams = layoutParams
                

                // Ensure we initialize the layout only once
                if (!scoreLayoutInitialized) {
                    scoreLayoutInitialized = true
                               }
                
            } catch (e: Exception) {
                      }
        }
    }

    private fun resetScorePosition() {
        runOnUiThread {
                 scoreLayoutOriginalParams?.let { originalParams ->
                mBinding.layoutTvScore.layoutParams = originalParams
               }
            mBinding.layoutTvScore.visibleView()
        }
    }
    
    private fun startGame() {

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
            
            MusicManagerApp.playMusic(this@PokaRunActivity, {})
        } else {
            Log.w(TAG, "No song selected for music playback")
        }
        
        isGameRunning = true
        currentScore = 1000
        
        // Bắt đầu animation nếu chưa đang phát
        if (!isAnimationPlaying) {
            Log.d(TAG, "Starting animation for game")
            mBinding.animationView.playAnimation()
        }

        
        // Ensure score layout is visible when game starts
        mBinding.layoutTvScore.visibleView()

        // Show countdown

        
        // Update UI
        mBinding.btnRecord.setImageResource(R.drawable.ic_stop)
        mBinding.tvTimeRec.visibleView()
        

        // Initialize recording time tracking
        recordingStartTime = System.currentTimeMillis()
        startRecordingTimer()
        

        // Set random score for display
        
        // Chụp ảnh và lưu vào thư mục images
        mBinding.cameraView.takePictureSnapshot()
        
        // Start video recording

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
    
    private fun stopGame() {

        isGameRunning = false
        
        // Cancel recording timer
        recordingTimer?.cancel()
        
        // Update UI
        mBinding.btnRecord.setImageResource(R.drawable.ic_record)
        mBinding.tvTimeRec.goneView()
        mBinding.tvTimeCount.goneView()

        MusicManagerApp.stopMusic()
        
        // Reset score position
        resetScorePosition()
        
        // Stop video recording
        mBinding.cameraView.stopVideo()
        
        // Dừng animation nếu đang phát
        if (isAnimationPlaying) {
            Log.d(TAG, "Stopping animation after game stop")
            mBinding.animationView.pauseAnimation()
            isAnimationPlaying = false
        }
    }
    

    

    

    
    private fun showCountdown() {

        
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val count = (millisUntilFinished / 1000 ).toInt()
                runOnUiThread {

                    mBinding.tvTimeCount.visibleView()
                    mBinding.tvTimeCount.text = count.toString()
                }
            }

            override fun onFinish() {
                runOnUiThread {
                    if (!isGameRunning) {
                        startGame()
                        mBinding.tvTimeCount.goneView()
                    } else {
                        // Không gọi stopGame() ở đây nữa vì game sẽ tự động dừng khi animation kết thúc
                        Log.d(TAG, "Countdown finished but game will stop when animation ends")
                    }
                }
            }
        }.start()
    }
    

    private fun setupCamera() {
        try {

            mBinding.cameraView.mode = Mode.VIDEO
            mBinding.cameraView.facing = Facing.BACK
            mBinding.cameraView.setLifecycleOwner(this)
            
            // Ensure overlay and score are visible after camera setup
            mBinding.facePartsOverlay.visibleView()
            mBinding.layoutTvScore.visibleView()
            

        } catch (e: Exception) {

        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isEmpty()) {

            setupCamera()
        } else {

            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    /**
     * Reset all game state and UI elements to initial state
     * Called when returning from result screen or other activities
     */
    private fun resetToInitialState() {
        Log.d(TAG, "Resetting to initial state")
        
        // Reset game state variables
        isGameRunning = false
        isAnimationPlaying = false
        currentScore = 1000
        
        // Cancel any running timers
        recordingTimer?.cancel()
        recordingTimer = null
        
        // Reset recording time tracking
        recordingStartTime = 0L
        recordingElapsedTime = 0L
        
        // Reset UI elements to initial state
        resetUIToInitialState()
        
        // Reset animation state
        resetAnimationState()
        
        // Reset score position to original
        resetScorePosition()
        
        // Stop any playing music
        MusicManagerApp.stopMusic()
        
        // Ensure score layout is visible
        mBinding.layoutTvScore.visibleView()
        
        Log.d(TAG, "Initial state reset completed")
    }
    
    /**
     * Reset UI elements to their initial state
     */
    private fun resetUIToInitialState() {
        // Reset record button to initial state
        mBinding.btnRecord.setImageResource(R.drawable.ic_record)
        
        // Hide time recording display
        mBinding.tvTimeRec.goneView()
        
        // Hide countdown display
        mBinding.tvTimeCount.goneView()
        
        // Reset camera to front facing (initial state)
        mBinding.cameraView.facing = Facing.BACK
        
        // Generate new random score for display
        mBinding.tvScore.text = getString(R.string.dp_score, Random.nextInt(10000))
        
        Log.d(TAG, "UI elements reset to initial state")
    }
    
    /**
     * Reset animation to initial state
     */
    private fun resetAnimationState() {
        try {
            // Stop animation if it's currently playing
            if (mBinding.animationView.isAnimating) {
                mBinding.animationView.pauseAnimation()
                Log.d(TAG, "Animation paused during reset")
            }
            
            // Reset animation progress to beginning
            mBinding.animationView.progress = 0f
            
            // Update animation playing flag
            isAnimationPlaying = false
            
            Log.d(TAG, "Animation state reset completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting animation state", e)
        }
    }
    
    override fun onResume() {
        super.onResume()

        // Reset to initial state when returning from result screen or other activities
        resetToInitialState()
        
        // Refresh local music paths khi resume
        MusicManagerApp.refreshLocalMusicPaths(this)
        
        // Cập nhật tiêu đề nhạc đang chọn
        updateMusicTitle()
        
        // Kiểm tra và cập nhật trạng thái animation
        updateAnimationState()
    }
    
    /**
     * Cập nhật trạng thái animation dựa trên trạng thái hiện tại
     */
    private fun updateAnimationState() {
        val isCurrentlyPlaying = mBinding.animationView.isAnimating
        if (isCurrentlyPlaying != isAnimationPlaying) {
            Log.d(TAG, "Animation state mismatch detected, updating: was=$isAnimationPlaying, actual=$isCurrentlyPlaying")
            isAnimationPlaying = isCurrentlyPlaying
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
          // Cancel timers
        recordingTimer?.cancel()
        
        poseDetectionExecutor.shutdown()
        poseDetector.close()
        MusicManagerApp.stopMusic()
    }
    
    override fun onPause() {
        super.onPause()

        if (isGameRunning) {
            stopGame()
        }
    }

    /**
     * Cho phép click vào LottieView sau khi animation load xong
     * Chỉ cho phép click khi game không chạy và animation không đang phát
     */
    private fun enableLottieViewClick() {
        mBinding.animationView.click {
            // Chặn click khi game đang chạy hoặc animation đang phát
            if (isGameRunning || isAnimationPlaying) {
                Log.d(TAG, "Click blocked: game running=$isGameRunning, animation playing=$isAnimationPlaying")
                return@click
            }

            if (it is LottieAnimationView) {
                Log.d(TAG, "LottieView clicked, starting countdown")
                showCountdown()
            }
        }
        
        Log.d(TAG, "LottieView click đã được enable với điều kiện kiểm tra")
    }
    
    /**
     * Test việc phát nhạc để debug
     */

    
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
}