package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Mode
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityWhoLookLikeBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.model.CelebrityMatch
import com.titanbbl.funny.face.filter.game.model.Resource
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.utils.ImageCaptureUtils
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.isVideoFileValid
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.getVideoFileSize
import com.titanbbl.funny.face.filter.game.utils.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class WhoLookLikeActivity : BaseActivity<ActivityWhoLookLikeBinding>() {

    private val viewModel: WhoViewModel by viewModels()
    private var countDownTimer: CountDownTimer? = null
    private var resultNavigationTimer: CountDownTimer? = null
    private var backupTimer: android.os.Handler? = null
    private var timeoutHandler: android.os.Handler? = null
    private var isCapturing = false
    private var isVideoRecording = false
    private var handler: Handler? = null
    private var imageLoopRunnable: Runnable? = null
    private var isLooping = false
    private val celebrityImageList = mutableListOf<String>()
    private var currentVideoFile: File? = null
    private var shouldStopVideoAfterCelebrityDisplay = false
    private var hasNavigatedToResult = false // Flag to prevent multiple navigation calls

    override fun getLayoutActivity(): Int {
        return R.layout.activity_who_look_like
    }

    override fun initViews() {
        super.initViews()

        // Set up camera view
        mBinding.cameraView.setLifecycleOwner(this)

        // Set up click listeners
        setupClickListeners()

        // Observe view model data
        observeData()

        // Set the ViewModel to the binding
        mBinding.viewModel = viewModel
        mBinding.lifecycleOwner = this

        // Initialize handler for image looping
        handler = Handler(Looper.getMainLooper())

        // Load celebrity image list from assets
        loadCelebrityImageList()
    }

    private fun loadCelebrityImageList() {
        try {
            val imageFiles = assets.list("imagecelebrity") ?: emptyArray()
            celebrityImageList.clear()

            for (file in imageFiles) {
                if (file.endsWith(".png") || file.endsWith(".jpg") || file.endsWith(".jpeg") || file.endsWith(
                        ".webp"
                    )
                ) {
                    celebrityImageList.add("file:///android_asset/imagecelebrity/$file")
                }
            }

            Timber.d("Loaded ${celebrityImageList.size} celebrity images from assets")
        } catch (e: IOException) {
            Timber.e(e, "Error loading celebrity images from assets")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mBinding.cameraView.isOpened) {
            mBinding.cameraView.open()
        }
        // Reset state so recording can start from the beginning after resume
        resetStateForRecording()
    }

    override fun onPause() {
        super.onPause()
        if (mBinding.cameraView.isOpened) {
            mBinding.cameraView.close()
        }

        // Cancel any ongoing timer
        countDownTimer?.cancel()
        resultNavigationTimer?.cancel()
        backupTimer?.removeCallbacksAndMessages(null)
        timeoutHandler?.removeCallbacksAndMessages(null)
        stopImageLooping()

        // Stop video recording if in progress
        if (mBinding.cameraView.isTakingVideo) {
            mBinding.cameraView.stopVideo()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        resultNavigationTimer?.cancel()
        backupTimer?.removeCallbacksAndMessages(null)
        timeoutHandler?.removeCallbacksAndMessages(null)
        stopImageLooping()

        // Stop video recording if in progress
        if (mBinding.cameraView.isTakingVideo) {
            mBinding.cameraView.stopVideo()
        }
    }

    private fun setupClickListeners() {
        // Back button
        mBinding.btnBack.setOnClickListener {
            finish()
        }

        // Record/Capture button
        mBinding.btnRecord.setOnClickListener {
            mBinding.cameraView.mode = Mode.PICTURE
            if (!isCapturing) {
                startCaptureCountdown()
            }
        }

        // Set up camera listener
        mBinding.cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                Timber.d("Picture taken successfully")
                processCapturedImage(result)
            }

            override fun onVideoTaken(result: VideoResult) {
                try {
                    isVideoRecording = false
                    Timber.d("Video recorded successfully: ${result.file.absolutePath}")
                    
                    // Cập nhật currentVideoFile với file thực tế được tạo
                    currentVideoFile = result.file
                    
                    // Kiểm tra xem file video có hợp lệ không
                    if (isVideoFileValid(result.file)) {
                        val fileSize = getVideoFileSize(result.file)
                        Timber.d("Video file is valid, size: $fileSize")
                        
                        // Navigate to result activity với video file
                        navigateToResultWithVideo(result.file.absolutePath)
                    } else {
                        Timber.e("Video file is invalid: exists=${result.file.exists()}, size=${result.file.length()}")
                        showError("Video recording failed. Please try again.")
                    }
                } catch (e: Exception) {
                    isVideoRecording = false
                    Timber.e(e, "Error processing video result")
                    showError("Error processing video. Please try again.")
                }
            }
            
            override fun onCameraError(exception: com.otaliastudios.cameraview.CameraException) {
                super.onCameraError(exception)
                Timber.e(exception, "Camera error occurred")
                isVideoRecording = false
                showError("Camera error: ${exception.message}")
            }
            
            override fun onVideoRecordingStart() {
                super.onVideoRecordingStart()
                Timber.d("Video recording started callback")
            }
            
            override fun onVideoRecordingEnd() {
                super.onVideoRecordingEnd()
                Timber.d("Video recording ended callback")
            }
        })
    }
    
    /**
     * Navigate to ResultActivity using Routes.startResultActivity
     * Prevents multiple navigation calls with hasNavigatedToResult flag
     */
    private fun navigateToResultWithVideo(videoPath: String) {
        if (hasNavigatedToResult) {
            Timber.w("Navigation already in progress, ignoring duplicate call")
            return
        }
        
        try {
            hasNavigatedToResult = true
            
            // Cancel all timers to prevent duplicate navigation
            backupTimer?.removeCallbacksAndMessages(null)
            timeoutHandler?.removeCallbacksAndMessages(null)
            
            Timber.d("Navigating to ResultActivity with video: $videoPath")
            Routes.startResultActivity(this, videoPath)
        } catch (e: Exception) {
            hasNavigatedToResult = false // Reset flag on error
            Timber.e(e, "Error navigating to ResultActivity")
            showError("Navigation error. Please try again.")
        }
    }

    private fun observeData() {
        viewModel.celebritySearchResult.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
//                    hideLoading()

                    val data = resource.data
                    if (data != null && data["success"] == true) {
                        @Suppress("UNCHECKED_CAST")
                        val matches = data["matches"] as? List<CelebrityMatch>
                        if (!matches.isNullOrEmpty()) {
                            val firstMatch = matches[0]
                            val name = firstMatch.name
                            val celebrityInfo = firstMatch.info

                            // Get the first image URL from the celebrity info
                            val imageUrl = viewModel.getFirstImageUrl(celebrityInfo)

                            if (imageUrl != null) {
                                loadCelebrityImage(imageUrl)
                                Timber.d("Celebrity match found: $name, loading image from $imageUrl")

                                // Schedule navigation to ResultActivity after 5 seconds

                            } else {
                                Timber.e("No image URL available for celebrity: $name")
                                showError("Image not available for the matched celebrity.")

                                // Stop video recording and navigate if in progress
                                if (mBinding.cameraView.isTakingVideo && isVideoRecording) {
                                    stopVideoAndNavigateToResult()
                                }
                            }

                            // Show success message

                        }
                    } else {
                        Timber.e("Celebrity search failed: ${data?.get("error")}")
                        showError("No celebrity match found. Try again.")

                        // Stop video recording and navigate if in progress
                        if (mBinding.cameraView.isTakingVideo && isVideoRecording) {
                            stopVideoAndNavigateToResult()
                        }
                    }
                }

                is Resource.Error -> {
                    hideLoading()
                    Timber.e("Celebrity search error: ${resource.message}")
                    showError("Error: ${resource.message}")

                    // Stop video recording and navigate if in progress
                    if (mBinding.cameraView.isTakingVideo && isVideoRecording) {
                        stopVideoAndNavigateToResult()
                    }
                }

                is Resource.Loading -> {
//
                    handleLoadingResult()
                }
            }
        }
    }

    private fun startCaptureCountdown() {
        isCapturing = true

        // Show countdown UI
        mBinding.tvTimeCount.visibility = View.VISIBLE

        // Start a 3-second countdown
        countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000) + 1
                mBinding.tvTimeCount.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                mBinding.tvTimeCount.visibility = View.GONE
                captureImage()
            }
        }.start()
    }

    private fun startVideoRecording() {
        try {
            // Tạo file video mới cho mỗi lần recording
            currentVideoFile = createVideoFile(this)
            
            if (currentVideoFile == null) {
                Timber.e("Failed to create video file")
                showError("Failed to create video file. Please try again.")
                return
            }

            Timber.d("Starting video recording to ${currentVideoFile!!.absolutePath}")
            
            isVideoRecording = true
            shouldStopVideoAfterCelebrityDisplay = false
            
            // Switch to VIDEO mode before starting recording
            mBinding.cameraView.mode = Mode.VIDEO
            
            // Add a small delay to ensure mode switch is complete
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    // Start video recording
                    mBinding.cameraView.takeVideoSnapshot(currentVideoFile!!)
                    Timber.d("Video recording started successfully to ${currentVideoFile!!.absolutePath}")
                } catch (e: Exception) {
                    Timber.e(e, "Error starting video snapshot")
                    isVideoRecording = false
                    showError("Failed to start video recording. Please try again.")
                }
            }, 100) // 100ms delay
            
        } catch (e: Exception) {
            Timber.e(e, "Error starting video recording")
            showError("Failed to start video recording. Please try again.")
            isVideoRecording = false
        }
    }

    private fun stopVideoAndNavigateToResult() {
        Timber.d("stopVideoAndNavigateToResult: isTakingVideo=${mBinding.cameraView.isTakingVideo}, isVideoRecording=$isVideoRecording")
        
        if (mBinding.cameraView.isTakingVideo && isVideoRecording) {
            Timber.d("Stopping video recording...")
            shouldStopVideoAfterCelebrityDisplay = true
            mBinding.cameraView.stopVideo()
            
            // Add a timeout fallback in case onVideoTaken is not called
            timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            timeoutHandler?.postDelayed({
                if (isVideoRecording && !hasNavigatedToResult) {
                    Timber.w("onVideoTaken callback not received within 3 seconds, forcing navigation")
                    isVideoRecording = false
                    currentVideoFile?.let { file ->
                        if (file.exists()) {
                            navigateToResultWithVideo(file.absolutePath)
                        } else {
                            Timber.e("Video file does not exist after timeout")
                            showError("Video recording failed. Please try again.")
                        }
                    } ?: run {
                        Timber.e("No video file available after timeout")
                        showError("Video recording failed. Please try again.")
                    }
                }
            }, 3000) // 3 second timeout
            
        } else if (isVideoRecording) {
            // Video recording flag is true but camera says it's not recording
            Timber.w("Video recording flag is true but camera is not taking video - forcing stop")
            isVideoRecording = false
            currentVideoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    navigateToResultWithVideo(file.absolutePath)
                } else {
                    Timber.w("Video file is empty or doesn't exist, creating new one")
                    val newFile = createVideoFile(this)
                    if (newFile != null) {
                        navigateToResultWithVideo(newFile.absolutePath)
                    } else {
                        showError("Failed to create video file. Please try again.")
                    }
                }
            } ?: run {
                Timber.w("No current video file, creating new one")
                val newFile = createVideoFile(this)
                if (newFile != null) {
                    navigateToResultWithVideo(newFile.absolutePath)
                } else {
                    showError("Failed to create video file. Please try again.")
                }
            }
        } else {
            Timber.d("No active video recording, checking current video file")
            
            // Kiểm tra xem currentVideoFile có tồn tại và hợp lệ không
            currentVideoFile?.let { file ->
                if (file.exists() && isVideoFileValid(file)) {
                    val fileSize = getVideoFileSize(file)
                    Timber.d("Current video file exists and is valid, size: $fileSize")
                    navigateToResultWithVideo(file.absolutePath)
                } else {
                    Timber.w("Current video file is invalid, creating placeholder")
                    val newFile = createVideoFile(this)
                    if (newFile != null) {
                        navigateToResultWithVideo(newFile.absolutePath)
                    } else {
                        showError("Failed to create video file. Please try again.")
                    }
                }
            } ?: run {
                Timber.w("No current video file, creating new one")
                val newFile = createVideoFile(this)
                if (newFile != null) {
                    navigateToResultWithVideo(newFile.absolutePath)
                } else {
                    showError("Failed to create video file. Please try again.")
                }
            }
        }
    }

    private fun captureImage() {
        try {
            mBinding.cameraView.takePicture()
        } catch (e: Exception) {
            Timber.e(e, "Error taking picture")
            showError("Failed to take picture. Please try again.")
            isCapturing = false
        }
    }

    private fun processCapturedImage(result: PictureResult) {
        try {
            // Save picture to file
            val filePath = ImageCaptureUtils.savePictureToFile(result, this)

            // Search for celebrity matches
            viewModel.searchCelebrity(filePath)

            // Start video recording after image capture
            startVideoRecording()

            // Add a backup timer to stop video after 10 seconds if nothing happens
            backupTimer = android.os.Handler(android.os.Looper.getMainLooper())
            backupTimer?.postDelayed({
                if (isVideoRecording && !hasNavigatedToResult) {
                    Timber.w("Backup timer triggered - stopping video after 10 seconds")
                    stopVideoAndNavigateToResult()
                }
            }, 10000) // 10 second backup timer

            Timber.d("Image captured and saved to $filePath")
        } catch (e: Exception) {
            Timber.e(e, "Error processing captured image")
            showError("Error processing image. Please try again.")

            // Stop video recording and navigate if in progress
            if (mBinding.cameraView.isTakingVideo && isVideoRecording) {
                stopVideoAndNavigateToResult()
            }
        } finally {
            isCapturing = false
        }
    }

    fun handleLoadingResult() {
        // Make the image visible
        mBinding.imgWhoLockLike.visibility = View.VISIBLE

        // Start looping through random celebrity images
        startImageLooping()
    }

    private fun startImageLooping() {
        // Stop any existing loop
        stopImageLooping()

        // Check if we have images to loop through
        if (celebrityImageList.isEmpty()) {
            Timber.w("No celebrity images found in assets")
            return
        }

        isLooping = true
        imageLoopRunnable = Runnable {
            if (isLooping) {
                // Get a random image from the list
                val randomIndex = Random.nextInt(celebrityImageList.size)
                val randomImageUrl = celebrityImageList[randomIndex]

                // Load the random image
                Glide.with(this)
                    .load(randomImageUrl)
                    .into(mBinding.imgWhoLockLike)

                Timber.d("Showing random celebrity image: $randomImageUrl")

                // Schedule the next image change
                handler?.postDelayed(imageLoopRunnable!!, 200)
            }
        }

        // Start the loop
        handler?.post(imageLoopRunnable!!)
    }

    private fun stopImageLooping() {
        isLooping = false
        imageLoopRunnable?.let {
            handler?.removeCallbacks(it)
        }
    }

    // build.gradle: implementation("com.squareup.okhttp3:okhttp:4.12.0")

    private val okHttp by lazy {
        OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    }

    /** Tải ảnh qua HTTPS về internal storage và hiển thị bằng Glide */
    @SuppressLint("SetWorldReadable") // bỏ nếu không dùng setReadable
    private fun loadCelebrityImage(imageUrl: String) {
        stopImageLooping()
        Timber.d("Downloading via HTTPS: $imageUrl")

        lifecycleScope.launch(Dispatchers.IO) {
            val req = Request.Builder().url(imageUrl) // nên là https://...
                .addHeader("accessKey", "03d01bc4-2cec-4d16-92ca5d0d2ca5-3eae-4cb8").build()

            try {
                okHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IOException("HTTP ${resp.code}")
                    }

                    // Lấy extension theo Content-Type (fallback .jpg)
                    val ct = resp.header("Content-Type").orEmpty()
                    val ext = when {
                        ct.contains("png", true) -> ".png"
                        ct.contains("webp", true) -> ".webp"
                        ct.contains("gif", true) -> ".gif"
                        else -> ".jpg"
                    }

                    // Thư mục lưu ảnh: /data/data/<pkg>/files/images/
                    val dir = File(filesDir, "images").apply { if (!exists()) mkdirs() }
                    val file = File(
                        dir, "celebrity_${
                            System.currentTimeMillis()
                        }$ext"
                    )

                    resp.body!!.byteStream().use { ins ->
                        FileOutputStream(file).use { outs ->
                            ins.copyTo(outs)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Timber.d("Saved image to ${file.absolutePath}")

                        // Hiển thị từ file (không cần header nữa)
                        Glide.with(this@WhoLookLikeActivity).load(file)
                            .apply(RequestOptions().timeout(30_000))
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirst: Boolean
                                ): Boolean {
                                    Timber.e(e, "Glide show failed")

                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirst: Boolean
                                ): Boolean {
                                    Log.d(
                                        "load_image",
                                        "onResourceReady (local file), dataSource=$dataSource"
                                    )

                                    return false
                                }
                            }).into(mBinding.imgWhoLockLike)


                    }

                    stopImageLooping()

                    // Đợi 3 giây để hiển thị ảnh celebrity, sau đó stop video và navigate
                    lifecycleScope.launch(Dispatchers.Main) {
                        delay(3000)
                        Timber.d("Celebrity image displayed for 3s, now stopping video and navigating to result")
                        stopVideoAndNavigateToResult()
                    }
                    // (Tuỳ chọn) cho phép đọc ngoài app nếu cần chia sẻ
                    // file.setReadable(true, false)


                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Timber.e(e, "Download failed")


                }
            }
        }
    }


    private fun showLoading() {
        mBinding.loadingContainer.visibility = View.VISIBLE
        mBinding.btnRecord.isEnabled = false
    }

    private fun hideLoading() {
        mBinding.loadingContainer.visibility = View.GONE
        mBinding.btnRecord.isEnabled = true
    }

    private fun showError(message: String) {
        // Show error message as toast
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun resetStateForRecording() {
        try {
            // Stop any ongoing video recording to start fresh
            if (mBinding.cameraView.isTakingVideo) {
                mBinding.cameraView.stopVideo()
            }

            // Ensure camera is back to initial flow (take picture first)
            mBinding.cameraView.mode = Mode.PICTURE

            // Cancel timers and looping UI
            countDownTimer?.cancel()
            resultNavigationTimer?.cancel()
            backupTimer?.removeCallbacksAndMessages(null)
            timeoutHandler?.removeCallbacksAndMessages(null)
            stopImageLooping()

            // Reset UI elements
            mBinding.tvTimeCount.visibility = View.GONE
            mBinding.imgWhoLockLike.visibility = View.GONE
            hideLoading()

            // Reset flags
            isCapturing = false
            isVideoRecording = false
            shouldStopVideoAfterCelebrityDisplay = false
            hasNavigatedToResult = false // Reset navigation flag

            // Clean up previous video file
            currentVideoFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            currentVideoFile = null
            
            Timber.d("State reset completed successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error resetting state on resume")
        }
    }
}