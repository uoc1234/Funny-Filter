package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.prediction_national

import android.animation.Animator
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.databinding.ActivityLipNationalBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.bases.ext.click
import com.titanbbl.funny.face.filter.game.ui.bases.ext.goneView
import com.titanbbl.funny.face.filter.game.ui.bases.ext.invisibleView
import com.titanbbl.funny.face.filter.game.ui.bases.ext.parcelable
import com.titanbbl.funny.face.filter.game.ui.bases.ext.visibleView
import com.titanbbl.funny.face.filter.game.ui.component.custom.FlagScrollView
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureItem
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureType
import com.titanbbl.funny.face.filter.game.utils.CameraUtil.createVideoFile
import com.titanbbl.funny.face.filter.game.utils.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class HumanNationalActivity : BaseActivity<ActivityLipNationalBinding>() {

    private lateinit var flagScrollView: FlagScrollView
    private var isRecording = false
    private var hasAutoScrolledOnce = false // Biến để kiểm tra đã auto scroll một lần chưa

    private var countdownTime = 3 // default seconds
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null

    private var isVideoRecording = false
    private var recordingStartTime = 0L
    private var recordingTimerHandler: Handler? = null
    private var recordingTimerRunnable: Runnable? = null

    val outputFile by lazy { createVideoFile(this) }

    var listNumBabyTexts = listOf(
        "4 Babies",
        "6 Babies",
        "8 Babies",
        "3 Babies",
        "5 Babies",
        "7 Babies",
        "9 Babies",
        "2 Babies",
    )

    val random = Random(System.currentTimeMillis())

    val babyDescriptions = getListTextBabies()

    private fun getListTextBabies(): List<String> {
        return listNumBabyTexts.shuffled(random).map { text ->
            val number = text.trim().split(" ")[0].toInt()

            val boys = random.nextInt(0, number + 1)
            val girls = number - boys

            val boyText = "$boys ${if (boys == 1) "boy" else "boys"}"
            val girlText = "$girls ${if (girls == 1) "girl" else "girls"}"

            val randomOrder = if (random.nextBoolean()) {
                "$boyText, $girlText"
            } else {
                "$girlText, $boyText"
            }

            "$number Babies → $randomOrder"
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentTextIndex = 0
    private val TEXT_CHANGE_INTERVAL = 300L

    private val textRunnable by lazy {
        object : Runnable {
            override fun run() {
                // Check if we've reached 5 seconds of recording
                val currentRecordingTime = if (isRecording) {
                    (System.currentTimeMillis() - recordingStartTime) / 1000
                } else 0

                if (currentRecordingTime >= 5) {
                    // Stop text animation at 5 seconds
                    Log.d("TAG", "Text animation stopped at 5 seconds")
                    return
                }

                currentTextIndex = (currentTextIndex + 1) % babyDescriptions.size
                mBinding.overlayImage.setTextWithAnimation(babyDescriptions[currentTextIndex])
                handler.postDelayed(this, TEXT_CHANGE_INTERVAL)
            }
        }
    }

    private var cameraListener = object : CameraListener() {
        override fun onCameraOpened(options: CameraOptions) {
            super.onCameraOpened(options)
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            Routes.startResultActivity(this@HumanNationalActivity, outputFile.absolutePath)
        }
    }

    override fun getLayoutActivity() = R.layout.activity_lip_national

    override fun initViews() {
        super.initViews()

        // Ẩn các view countdown và recording ban đầu
        mBinding.tvTimeCount.goneView()
        mBinding.tvTimeRec.goneView()

        mBinding.overlayImage.setQuestion("How many babies will you get?")


        val dataIntent = intent.parcelable<PhysicalFeatureItem>("physicalFeature")

        mBinding.overlayImage.invisibleView()
        mBinding.flagScrollView.invisibleView()

        when (dataIntent?.type) {
            PhysicalFeatureType.LIPS -> {
                mBinding.ltvAction.setAnimation(R.raw.filter_lips)
                mBinding.ltvAction.speed = 0.4f
                mBinding.flagScrollView.visibleView()
            }

            PhysicalFeatureType.NOSE -> {
                mBinding.ltvAction.setAnimation(R.raw.filter_nose)
                mBinding.ltvAction.speed = 0.4f
                mBinding.flagScrollView.visibleView()
            }

            PhysicalFeatureType.HAND -> {
                mBinding.ltvAction.setAnimation(R.raw.filter_hand)
                mBinding.ltvAction.speed = 0.4f
                mBinding.overlayImage.visibleView()
            }

            else -> {

            }
        }

        setupFlagScrollView()
        setupCamera()

        // Không auto scroll khi mở app - chỉ scroll khi bắt đầu quay

        mBinding.ltvAction.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                // Animation bắt đầu
                Log.d("Lottie", "Animation started")
            }

            override fun onAnimationEnd(animation: Animator) {
                // Khi animation kết thúc
                Log.d("Lottie", "Animation ended")

                // Gọi stopRecordingScroll trước khi dừng quay
                if (::flagScrollView.isInitialized) {
                    flagScrollView.stopRecordingScroll()
                }

                // Dừng quay video
                stopVideoRecording()

            }

            override fun onAnimationCancel(animation: Animator) {
                // Animation bị cancel
                Log.d("Lottie", "Animation canceled")
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Animation lặp lại (nếu loop)
                Log.d("Lottie", "Animation repeated")
            }
        })
    }

    private fun setupCamera() {
        try {
            mBinding.cameraView.setLifecycleOwner(this)
            mBinding.cameraView.facing = Facing.FRONT // Mặc định camera trước
            mBinding.cameraView.mode = Mode.VIDEO
            mBinding.cameraView.addCameraListener(cameraListener)

            if (!mBinding.cameraView.isOpened) {
                mBinding.cameraView.open()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật tiêu đề bài hát đã chọn
        MusicManagerApp.refreshLocalMusicPaths(this)
        MusicManagerApp.getCurrentSong().apply {
            mBinding.tvFilterNameTop.text = this?.title ?: "No song selected"
        }
        listNumBabyTexts = getListTextBabies()

        // Reset toàn bộ trạng thái khi quay lại từ màn result
        resetStateOnResume()
    }

    override fun onClickViews() {
        super.onClickViews()


        mBinding.filterHeaderTop.click {
            Routes.startSelectMusicActivity(this)
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


        mBinding.btnRecord.setOnClickListener {
            if (!isVideoRecording) {
                startCountdownBeforeRecord()

            } else {
                stopVideoRecording()
            }
        }

        mBinding.btnCamera.setOnClickListener {
            // Toggle front/back camera (only when not recording)
            if (!isVideoRecording) {
                try {
                    mBinding.cameraView.toggleFacing()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        mBinding.ivTimer.setOnClickListener {
            if (!isVideoRecording) {
                // Toggle countdown time: 3 -> 5 -> 10 -> 3
                countdownTime = when (countdownTime) {
                    3 -> 5
                    5 -> 10
                    else -> 3
                }
                mBinding.tvTimer.text = "${countdownTime}s"
            }
        }

        mBinding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupFlagScrollView() {
        try {
            flagScrollView = mBinding.flagScrollView
            flagScrollView.setOnFlagSelectedListener { selectedFlag ->
                // Kiểm tra nếu là lệnh dừng scroll
                if (selectedFlag.startsWith("stop_scroll_")) {
                    // Dừng scroll ngay lập tức
                    stopAutoScroll()
                    stopRecording()

                    // Parse chuỗi: stop_scroll_<flagNumber>:<adapterPosition>
                    val parts = selectedFlag.replace("stop_scroll_", "").split(":")
                    val flagNumber = parts[0]

                    handleFlagSelected("flat_$flagNumber")
                } else {
                    // Xử lý bình thường khi cờ được chọn
                    handleFlagSelected(selectedFlag)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Auto scroll một lần duy nhất khi mở activity, chạy trong 5-7 giây
     */
    private fun startSingleAutoScroll() {
        if (hasAutoScrolledOnce || !::flagScrollView.isInitialized) return

        hasAutoScrolledOnce = true

        try {
            flagScrollView.startSingleAutoScroll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAutoScroll() {
        try {
            if (::flagScrollView.isInitialized) {
                flagScrollView.stopAutoScroll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startRecording() {
        if (isRecording) return

        isRecording = true

        // Dừng auto scroll hiện tại nếu có
        stopAutoScroll()

        // Bắt đầu scroll nhanh cho recording
        startRecordingScroll()

        // Không cần random duration nữa - việc dừng sẽ được điều khiển bởi onAnimationEnd
        // Recording sẽ chạy cho đến khi animation kết thúc
    }

    private fun startRecordingScroll() {
        try {
            if (::flagScrollView.isInitialized) {
                flagScrollView.startRecordingScroll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        isRecording = false

        try {
            if (::flagScrollView.isInitialized) {
                mBinding.ltvAction.pauseAnimation()
                flagScrollView.stopRecordingScroll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Không cần xử lý recordingRunnable nữa vì đã loại bỏ random duration
        // Recording sẽ dừng khi animation kết thúc
    }

    private fun handleFlagSelected(flagName: String) {
        // Xử lý khi cờ được chọn
        // Có thể thêm logic áp dụng filter cho camera ở đây
    }



    private fun startCountdownBeforeRecord() {
        try {
            // Show countdown overlay
            mBinding.tvTimeCount.visibleView()
            mBinding.tvTimeCount.text = countdownTime.toString()
            var timeLeft = countdownTime
            countdownHandler = Handler(Looper.getMainLooper())
            countdownRunnable = object : Runnable {
                override fun run() {
                    timeLeft--
                    if (timeLeft <= 0) {
                        mBinding.tvTimeCount.goneView()
                        startVideoRecording()
                        mBinding.ltvAction.playAnimation()
                    } else {
                        mBinding.tvTimeCount.text = timeLeft.toString()
                        countdownHandler?.postDelayed(this, 1000)
                    }
                }
            }
            countdownHandler?.postDelayed(countdownRunnable!!, 1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun startVideoRecording() {
        // Bắt đầu auto scroll khi bắt đầu quay video
        if (!hasAutoScrolledOnce && ::flagScrollView.isInitialized) {
            startSingleAutoScroll()
        }

        // Start list scroll recording behaviour too
        if (!isRecording) {
            startRecording()
        }

        isVideoRecording = true
        recordingStartTime = System.currentTimeMillis()
        mBinding.tvTimeRec.visibleView()
        mBinding.tvTimeRec.text = "00:00"

        // Vô hiệu hóa các button chọn nhạc khi đang quay
        disableMusicSelectionButtons()

        // Bắt đầu phát nhạc nếu có bài đã chọn
        try {
            val currentSong = MusicManagerApp.getCurrentSong()
            if (currentSong != null) {
                MusicManagerApp.playMusic(this) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start recording timer
        recordingTimerHandler = Handler(Looper.getMainLooper())
        recordingTimerRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                mBinding.tvTimeRec.text = String.format("%02d:%02d", minutes, seconds)
                if (isVideoRecording) {
                    recordingTimerHandler?.postDelayed(this, 1000)
                }
            }
        }
        recordingTimerHandler?.post(recordingTimerRunnable!!)

        // Start text animation for HAND mode when recording starts
        val dataIntent = intent.parcelable<PhysicalFeatureItem>("physicalFeature")
        if (dataIntent?.type == PhysicalFeatureType.HAND) {
            // Reset text index and start text animation
            currentTextIndex = 0
            handler.post(textRunnable)
        }

        // Start video recording with CameraView
        try {

            mBinding.cameraView.takeVideoSnapshot(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVideoRecording() {
        isVideoRecording = false
        mBinding.tvTimeRec.goneView()
        
        // Kích hoạt lại các button chọn nhạc khi dừng quay
        enableMusicSelectionButtons()
        
        // Dừng phát nhạc khi dừng quay
        try {
            MusicManagerApp.stopMusic()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recordingTimerRunnable?.let { runnable ->
            recordingTimerHandler?.removeCallbacks(runnable)
        }
        recordingTimerHandler = null
        recordingTimerRunnable = null

        // Stop text animation for HAND mode when recording stops
        val dataIntent = intent.parcelable<PhysicalFeatureItem>("physicalFeature")
        if (dataIntent?.type == PhysicalFeatureType.HAND) {
            handler.removeCallbacks(textRunnable)
        }

        try {
            lifecycleScope.launch(Dispatchers.IO) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    mBinding.cameraView.stopVideo()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Stop list scroll recording behaviour
        stopRecording()
    }

    /**
     * Vô hiệu hóa các button chọn nhạc khi đang quay
     */
    private fun disableMusicSelectionButtons() {
        try {
            mBinding.filterHeaderTop.isClickable = false
            mBinding.filterHeaderTop.isFocusable = false
            mBinding.filterHeaderTop.alpha = 0.5f
            
            mBinding.tvFilterNameTop.isClickable = false
            mBinding.tvFilterNameTop.isFocusable = false
            mBinding.tvFilterNameTop.alpha = 0.5f
            
            mBinding.btnCloseFilterTop?.isClickable = false
            mBinding.btnCloseFilterTop?.isFocusable = false
            mBinding.btnCloseFilterTop?.alpha = 0.5f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Kích hoạt lại các button chọn nhạc khi dừng quay
     */
    private fun enableMusicSelectionButtons() {
        try {
            mBinding.filterHeaderTop.isClickable = true
            mBinding.filterHeaderTop.isFocusable = true
            mBinding.filterHeaderTop.alpha = 1.0f
            
            mBinding.tvFilterNameTop.isClickable = true
            mBinding.tvFilterNameTop.isFocusable = true
            mBinding.tvFilterNameTop.alpha = 1.0f
            
            mBinding.btnCloseFilterTop?.isClickable = true
            mBinding.btnCloseFilterTop?.isFocusable = true
            mBinding.btnCloseFilterTop?.alpha = 1.0f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reset UI và state khi quay lại màn hình này (ví dụ từ màn Result).
     */
    private fun resetStateOnResume() {
        try {
            // Dừng tất cả scrolling/animation
            stopAutoScroll()
            if (::flagScrollView.isInitialized) {
                flagScrollView.stopRecordingScroll()
            }

            // Dừng nhạc nếu còn phát
            try {
                MusicManagerApp.stopMusic()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Hủy countdown nếu còn
            countdownRunnable?.let { runnable ->
                countdownHandler?.removeCallbacks(runnable)
            }
            countdownHandler = null
            countdownRunnable = null

            // Hủy timer ghi hình nếu còn
            recordingTimerRunnable?.let { runnable ->
                recordingTimerHandler?.removeCallbacks(runnable)
            }
            recordingTimerHandler = null
            recordingTimerRunnable = null

            // Hủy text animation HAND
            handler.removeCallbacks(textRunnable)
            mBinding.overlayImage.setTextWithAnimation("???")

            // Reset cờ trạng thái
            isRecording = false
            isVideoRecording = false
            hasAutoScrolledOnce = false
            currentTextIndex = 0

            // Reset UI
            mBinding.tvTimeCount.goneView()
            mBinding.tvTimeRec.goneView()

            // Kích hoạt lại các button chọn nhạc
            enableMusicSelectionButtons()

            // Reset Lottie về đầu và dừng
            try {
                mBinding.ltvAction.pauseAnimation()
                mBinding.ltvAction.progress = 0f
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        // Clean up all handlers and runnables
        try {
            stopAutoScroll()

            countdownRunnable?.let { runnable ->
                countdownHandler?.removeCallbacks(runnable)
            }
            recordingTimerRunnable?.let { runnable ->
                recordingTimerHandler?.removeCallbacks(runnable)
            }

            // Stop text animation for HAND mode
            handler.removeCallbacks(textRunnable)

            // Stop camera
            if (mBinding.cameraView.isOpened) {
                mBinding.cameraView.close()
            }

            // Clean up flag scroll view
            if (::flagScrollView.isInitialized) {
                flagScrollView.cleanup()
            }

            // Kích hoạt lại các button chọn nhạc
            enableMusicSelectionButtons()

            // Clean up handlers
            countdownHandler = null
            countdownRunnable = null
            recordingTimerHandler = null
            recordingTimerRunnable = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }


}