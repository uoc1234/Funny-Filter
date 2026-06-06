package com.titanbbl.funny.face.filter.game.ui.component.custom

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import com.titanbbl.funny.face.filter.game.R
import kotlin.random.Random

class FlagScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val flagContainer: LinearLayout
    private val flagViews = mutableListOf<ImageView>()
    private var selectedPosition = 0
    private var isScrolling = false
    private var onFlagSelectedListener: ((String) -> Unit)? = null
    
    // Auto scroll variables
    private var autoScrollHandler: Handler? = null
    private var autoScrollRunnable: Runnable? = null
    private var isAutoScrolling = false

    init {
        // Create container layout
        flagContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        }
        addView(flagContainer)
        
        // Setup flags
        setupFlags()
        
        // Disable scroll bar
        isHorizontalScrollBarEnabled = false
        
        // Set initial position
        post {
            scrollTo(getItemPosition(5), 0)
        }
    }

    private fun setupFlags() {
        val availableFlags = mutableListOf<String>()
        
        // Get available flag files from assets
        try {
            val assetFiles = context.assets.list("flat_natitional") ?: emptyArray()
            for (file in assetFiles) {
                if (file.endsWith(".png")) {
                    availableFlags.add(file)
                }
            }
            availableFlags.sort() // Sort để đảm bảo thứ tự
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default list if assets reading fails
            for (i in 1..64) {
                availableFlags.add("flat_$i.png")
            }
        }

        // Add padding items at start (last 5 flags)
        val paddingStart = availableFlags.takeLast(5)
        for (flagName in paddingStart) {
            addFlagView(flagName, false)
        }
        
        // Add main flags
        for (flagName in availableFlags) {
            addFlagView(flagName, true)
        }
        
        // Add padding items at end (first 5 flags)
        val paddingEnd = availableFlags.take(5)
        for (flagName in paddingEnd) {
            addFlagView(flagName, false)
        }
    }

    private fun addFlagView(flagName: String, isMainFlag: Boolean) {
        val flagView = LayoutInflater.from(context).inflate(R.layout.item_flag_simple, flagContainer, false) as ImageView
        
        // Load flag image
        loadFlagImage(flagView, flagName)
        
        // Set click listener
        flagView.setOnClickListener {
            val position = flagViews.indexOf(flagView)
            stopAutoScroll()
            setSelectedPosition(position)
            centerOnPosition(position)
            
            // Get real flag index
            val realIndex = getRealPosition(position) + 1
            onFlagSelectedListener?.invoke("stop_scroll_${realIndex}:${position}")
        }
        
        flagContainer.addView(flagView)
        flagViews.add(flagView)
    }

    private fun loadFlagImage(imageView: ImageView, flagName: String) {
        try {
            val inputStream = context.assets.open("flat_natitional/$flagName")
            val drawable = Drawable.createFromStream(inputStream, null)
            imageView.setImageDrawable(drawable)
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            imageView.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    private fun getItemPosition(index: Int): Int {
        if (index >= flagViews.size || index < 0) return 0
        
        val itemWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._50sdp)
        val marginHorizontal = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
        return index * (itemWidth + marginHorizontal * 2)
    }

    private fun centerOnPosition(position: Int) {
        post {
            val itemPosition = getItemPosition(position)
            val screenWidth = width
            val itemWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._50sdp)
            val centerPosition = itemPosition - (screenWidth / 2) + (itemWidth / 2)
            smoothScrollTo(centerPosition, 0)
        }
    }

    private fun getRealPosition(position: Int): Int {
        // Do có 5 item padding ở đầu, trừ đi 5 để lấy vị trí thực
        val availableFlags = getAvailableFlagsCount()
        return ((position - 5 + availableFlags) % availableFlags)
    }

    private fun getAvailableFlagsCount(): Int {
        // Count actual flag files
        return try {
            val assetFiles = context.assets.list("flat_natitional") ?: emptyArray()
            assetFiles.count { it.endsWith(".png") }
        } catch (e: Exception) {
            64 // Default fallback
        }
    }

    fun setScrolling(scrolling: Boolean) {
        isScrolling = scrolling
        updateFlagAppearance()
    }

    fun setSelectedPosition(position: Int) {
        if (position >= 0 && position < flagViews.size) {
            selectedPosition = position
            updateFlagAppearance()
            val realIndex = getRealPosition(position) + 1
            onFlagSelectedListener?.invoke("flat_$realIndex")
        }
    }

    private fun updateFlagAppearance() {
        flagViews.forEachIndexed { index, flagView ->
            when {
                isScrolling -> {
                    // Khi đang scroll - tất cả đều sáng
                    flagView.alpha = 1.0f
                    flagView.scaleX = 1.0f
                    flagView.scaleY = 1.0f
                }
                index == selectedPosition -> {
                    // Khi dừng và được chọn - sáng và to hơn
                    flagView.alpha = 1.0f
                    flagView.scaleX = 1.2f
                    flagView.scaleY = 1.2f
                }
                else -> {
                    // Khi dừng và không được chọn - mờ
                    flagView.alpha = 0.4f
                    flagView.scaleX = 1.0f
                    flagView.scaleY = 1.0f
                }
            }
        }
    }

    fun startSingleAutoScroll() {
        if (isAutoScrolling) return
        
        isAutoScrolling = true
        setScrolling(true)
        
        // Random thời gian từ 5-7 giây
        val randomDuration = Random.nextLong(5000, 7000)
        
        // Dừng auto scroll sau thời gian random
        Handler(Looper.getMainLooper()).postDelayed({
            stopAutoScroll()
        }, randomDuration)
        
        autoScrollHandler = Handler(Looper.getMainLooper())
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isAutoScrolling) {
                    try {
                        val currentScrollX = scrollX
                        val maxScrollX = flagContainer.width - width
                        
                        if (currentScrollX >= maxScrollX - 200) {
                            // Gần cuối, nhảy về đầu
                            scrollTo(getItemPosition(5), 0)
                        } else if (currentScrollX <= getItemPosition(5)) {
                            // Gần đầu, nhảy về cuối
                            scrollTo(maxScrollX - 200, 0)
                        } else {
                            // Scroll bình thường
                            smoothScrollBy(300, 0)
                        }
                        
                        autoScrollHandler?.postDelayed(this, 50)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        stopAutoScroll()
                    }
                }
            }
        }
        autoScrollHandler?.postDelayed(autoScrollRunnable!!, 50)
    }

    fun stopAutoScroll() {
        isAutoScrolling = false
        setScrolling(false)
        autoScrollRunnable?.let { runnable ->
            autoScrollHandler?.removeCallbacks(runnable)
        }
        
        // Update selected position based on current scroll
        updateSelectedPositionFromScroll()
    }

    private fun updateSelectedPositionFromScroll() {
        post {
            val currentScrollX = scrollX
            val itemWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._50sdp)
            val marginHorizontal = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
            val itemTotalWidth = itemWidth + marginHorizontal * 2
            val screenCenter = currentScrollX + width / 2
            val centerPosition = screenCenter / itemTotalWidth
            
            val newSelectedPosition = centerPosition.coerceIn(0, flagViews.size - 1)
            setSelectedPosition(newSelectedPosition)
            centerOnPosition(newSelectedPosition)
        }
    }

    fun setOnFlagSelectedListener(listener: (String) -> Unit) {
        onFlagSelectedListener = listener
    }

    fun startRecordingScroll() {
        setScrolling(true)
        
        autoScrollHandler = Handler(Looper.getMainLooper())
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isScrolling) {
                    try {
                        val currentScrollX = scrollX
                        val maxScrollX = flagContainer.width - width
                        
                        if (currentScrollX >= maxScrollX - 200) {
                            // Gần cuối, nhảy về đầu
                            scrollTo(0, 0)
                        } else {
                            // Scroll nhanh hơn khi recording
                            smoothScrollBy(400, 0)
                        }
                        
                        autoScrollHandler?.postDelayed(this, 50)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        setScrolling(false)
                    }
                }
            }
        }
        autoScrollHandler?.postDelayed(autoScrollRunnable!!, 50)
    }

    fun stopRecordingScroll() {
        setScrolling(false)
        autoScrollRunnable?.let { runnable ->
            autoScrollHandler?.removeCallbacks(runnable)
        }
        updateSelectedPositionFromScroll()
    }

    fun cleanup() {
        stopAutoScroll()
        autoScrollHandler = null
        autoScrollRunnable = null
        onFlagSelectedListener = null
    }
} 