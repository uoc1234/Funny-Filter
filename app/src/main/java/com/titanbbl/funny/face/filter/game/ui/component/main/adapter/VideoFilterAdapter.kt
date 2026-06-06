package com.titanbbl.funny.face.filter.game.ui.component.main.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.viewpager2.widget.ViewPager2
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ItemVideoFilterBinding
import com.titanbbl.funny.face.filter.game.model.VideoFilterItem
import com.titanbbl.funny.face.filter.game.ui.bases.BaseRecyclerView
import com.titanbbl.funny.face.filter.game.ui.component.main.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

private const val TAG = "VideoFilterAdapter"

class VideoFilterAdapter(
    private val onTryNowClick: (VideoFilterItem) -> Unit
) : BaseRecyclerView<VideoFilterItem>() {

    private var viewPager: ViewPager2? = null
    private val visibleItems = mutableSetOf<Int>()
    private val videoHolders = mutableMapOf<Int, VideoHolder>()
    private val handler = Handler(Looper.getMainLooper())

    // Flag to track if data has been shuffled
    private var isDataShuffled = false

    // Store the original shuffled order
    private var shuffledData: List<VideoFilterItem> = emptyList()

    override fun getItemLayout(): Int = R.layout.item_video_filter

    override fun submitData(newData: List<VideoFilterItem>) {
        Log.d(
            TAG,
            "submitData: newData.size=${newData.size}, currentPosition=$currentPosition, isDataShuffled=$isDataShuffled"
        )

        // Store current position before clearing
        val previousPosition = currentPosition

        if (!isDataShuffled) {
            // First time: shuffle the data and store the order
            shuffledData = newData.shuffled()
            isDataShuffled = true
            Log.d(TAG, "First time: Data shuffled and stored")
        }

        // Use the stored shuffled order
        list.clear()
        list.addAll(shuffledData)
        notifyDataSetChanged()

        // Update currentPosition to maintain consistency
        if (previousPosition >= 0 && previousPosition < list.size) {
            currentPosition = previousPosition
            Log.d(TAG, "submitData: Maintained currentPosition=$currentPosition")
        } else if (list.isNotEmpty()) {
            currentPosition = 0
            Log.d(TAG, "submitData: Reset currentPosition to 0")
        }

        Log.d(TAG, "submitData: Final currentPosition=$currentPosition, list.size=${list.size}")
    }

    fun shuffleData() {
        Log.d(TAG, "shuffData: Shuffling data")
        shuffledData = list.shuffled()
        isDataShuffled = true
        submitData(shuffledData)
    }

    override fun setData(binding: ViewDataBinding, item: VideoFilterItem, layoutPosition: Int) {
        if (binding is ItemVideoFilterBinding) {
            binding.apply {
                tvFilterName.text = item.filterName
                tvLikes.text = item.likes
                tvViews.text = item.views
                tvShares.text = item.shares

                // Initialize player with headers
                val dataSourceFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(
                    mapOf(
                        "accessKey" to item.accessKey
                    )
                )

                val player = ExoPlayer.Builder(root.context).setMediaSourceFactory(
                    DefaultMediaSourceFactory(dataSourceFactory)
                ).build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = false // Start paused by default

                    setMediaItem(MediaItem.fromUri(item.videoUrl))
                    prepare()
                }

                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        // If media becomes ready while not on Home, force pause
                        if (playbackState == Player.STATE_READY && !isHomeTabActive()) {
                            player.playWhenReady = false
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        // If playback starts while not on Home, stop immediately
                        if (isPlaying && !isHomeTabActive()) {
                            player.playWhenReady = false
                        }
                    }
                })

                playerView.player = player

                // Set up play/pause button
                btnPlayPause.setImageResource(R.drawable.ic_play_home)
                btnPlayPause.visibility = View.GONE

                // Store player in holder
                videoHolders[layoutPosition] = VideoHolder(binding, player)
            }
        }
    }

    override fun onClickViews(binding: ViewDataBinding, obj: VideoFilterItem, layoutPosition: Int) {
        super.onClickViews(binding, obj, layoutPosition)
        if (binding is ItemVideoFilterBinding) {
            binding.btnTryNow.setOnClickListener {
                onTryNowClick(obj)
            }

            // Set up click listener for video area
            binding.playerView.setOnClickListener {
                videoHolders[layoutPosition]?.togglePlayPause()
                videoHolders[layoutPosition]?.showControls()
            }

            // Track visibility
            binding.root.addOnAttachToWindowListener {
                visibleItems.add(layoutPosition)
                if (viewPager?.currentItem == layoutPosition && isHomeTabActive()) {
                    videoHolders[layoutPosition]?.playVideo()
                } else {
                    // Ensure video stays paused when not on Home tab
                    videoHolders[layoutPosition]?.pauseVideo()
                }
            }

            binding.root.addOnDetachFromWindowListener {
                visibleItems.remove(layoutPosition)
                videoHolders[layoutPosition]?.pauseVideo()
            }
        }
    }

    var currentPosition = 0
    fun attachToViewPager(viewPager: ViewPager2) {
        this.viewPager = viewPager
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Log.d(
                    TAG,
                    "onPageSelected: position=$position, previous currentPosition=$currentPosition"
                )

                // Validate position before proceeding
                if (position < 0 || !videoHolders.containsKey(position)) {
                    Log.w(TAG, "Invalid position $position or no video holder available")
                    return
                }

                // Update current position first
                currentPosition = position

                // Always pause all videos first
                pauseAllVideos()

                // Play the current video only if Home tab is active
                val currentHolder = videoHolders[position]
                if (currentHolder != null) {
                    if (isHomeTabActive()) {
                        Log.d(TAG, "Playing video at position $position (Home tab active)")
                        currentHolder.playVideo()
                    } else {
                        Log.d(TAG, "Skip playing at position $position (Home tab not active)")
                    }
                } else {
                    Log.e(TAG, "Failed to play video at position $position - holder is null")
                }

                Log.d(TAG, "onPageSelected: currentPosition updated to $currentPosition")
            }

            override fun onPageScrollStateChanged(state: Int) {
                Log.d(
                    TAG, "onPageScrollStateChanged: state=$state, currentPosition=$currentPosition"
                )
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        // User is dragging, pause all videos
                        Log.d(TAG, "User dragging - pausing all videos")
                        pauseAllVideos()
                    }

                    ViewPager2.SCROLL_STATE_SETTLING -> {
                        // Page is settling, keep videos paused
                        // They will be resumed in onPageSelected
                        Log.d(TAG, "Page settling - keeping videos paused")
                    }

                    ViewPager2.SCROLL_STATE_IDLE -> {
                        // Page is stable, resume current video
                        val currentPos = viewPager.currentItem
                        Log.d(TAG, "Page stable at position $currentPos - resuming video")

                        // Validate position before resuming
                        if (currentPos >= 0 && videoHolders.containsKey(currentPos)) {
                            currentPosition = currentPos
                            val holder = videoHolders[currentPos]
                            if (holder != null) {
                                if (isHomeTabActive()) {
                                    Log.d(
                                        TAG,
                                        "Resuming video at stable position $currentPos (Home tab active)"
                                    )
                                    holder.playVideo()
                                } else {
                                    Log.d(
                                        TAG,
                                        "Skip resuming at position $currentPos (Home tab not active)"
                                    )
                                    holder.pauseVideo()
                                }
                            } else {
                                Log.e(TAG, "Holder is null for position $currentPos")
                            }
                        } else {
                            Log.w(TAG, "Invalid position $currentPos for resuming video")
                        }
                    }
                }
            }
        })
    }

    fun onPause() {
        // Pause ALL videos, not just visible ones
        pauseAllVideos()
    }

    fun onResume() {
        viewPager?.let {
            // Use currentPosition instead of depending on visibleItems
            Log.d(
                TAG,
                "onResume: currentPosition=$currentPosition, viewPager.currentItem=${it.currentItem}"
            )

            // Get the actual current position from ViewPager
            val actualCurrentPosition = it.currentItem
            currentPosition = actualCurrentPosition

            // Validate that the current position has a valid video holder
            val currentHolder = videoHolders[actualCurrentPosition]
            if (currentHolder != null) {
                if (isHomeTabActive()) {
                    Log.d(
                        TAG, "Resuming video at position $actualCurrentPosition (Home tab active)"
                    )
                    currentHolder.playVideo()
                } else {
                    Log.d(
                        TAG,
                        "Skip resuming at position $actualCurrentPosition (Home tab not active)"
                    )
                    currentHolder.pauseVideo()
                }
            } else {
                Log.w(TAG, "No video holder found at position $actualCurrentPosition")
                // Try to find the first available holder
                val firstAvailablePosition = videoHolders.keys.firstOrNull()
                if (firstAvailablePosition != null) {
                    Log.d(TAG, "Falling back to first available position: $firstAvailablePosition")
                    currentPosition = firstAvailablePosition
                    if (isHomeTabActive()) {
                        videoHolders[firstAvailablePosition]?.playVideo()
                    } else {
                        videoHolders[firstAvailablePosition]?.pauseVideo()
                    }
                }
            }
        }
    }

    // Debug method to check current state
    fun debugCurrentState() {
        Log.d(TAG, "=== DEBUG CURRENT STATE ===")
        Log.d(TAG, "currentPosition: $currentPosition")
        Log.d(TAG, "viewPager.currentItem: ${viewPager?.currentItem}")
        Log.d(TAG, "visibleItems: $visibleItems")
        Log.d(TAG, "videoHolders.keys: ${videoHolders.keys.toList()}")
        Log.d(TAG, "list.size: ${list.size}")
        Log.d(TAG, "isDataShuffled: $isDataShuffled")
        Log.d(TAG, "shuffledData.size: ${shuffledData.size}")

        // Check for inconsistencies
        val viewPagerCurrent = viewPager?.currentItem ?: -1
        if (currentPosition != viewPagerCurrent) {
            Log.w(
                TAG,
                "⚠️ INCONSISTENCY: currentPosition ($currentPosition) != viewPager.currentItem ($viewPagerCurrent)"
            )
        }

        if (!videoHolders.containsKey(currentPosition)) {
            Log.w(
                TAG,
                "⚠️ INCONSISTENCY: currentPosition ($currentPosition) not found in videoHolders"
            )
        }

        // Check data consistency
        if (currentPosition >= 0 && currentPosition < list.size) {
            val currentItem = list[currentPosition]
            Log.d(TAG, "Current item: ${currentItem.filterName} (${currentItem.videoUrl})")
        } else {
            Log.w(
                TAG,
                "⚠️ INCONSISTENCY: currentPosition ($currentPosition) out of list bounds (0..${list.size - 1})"
            )
        }

        Log.d(TAG, "========================")
    }

    // Debug method to check data consistency
    fun debugDataConsistency() {
        Log.d(TAG, "=== DEBUG DATA CONSISTENCY ===")
        Log.d(TAG, "List items:")
        list.forEachIndexed { index, item ->
            Log.d(TAG, "  [$index] ${item.filterName} - ${item.videoUrl}")
        }
        Log.d(TAG, "Video holders:")
        videoHolders.forEach { (position, holder) ->
            Log.d(TAG, "  [$position] Holder exists: ${holder != null}")
        }
        Log.d(TAG, "========================")
    }

    // Method to fix inconsistent states
    fun fixInconsistentState() {
        Log.d(TAG, "Fixing inconsistent state...")
        viewPager?.let { vp ->
            val viewPagerCurrent = vp.currentItem
            if (currentPosition != viewPagerCurrent) {
                Log.d(TAG, "Fixing currentPosition: $currentPosition -> $viewPagerCurrent")
                currentPosition = viewPagerCurrent
            }

            // Ensure we have a valid video holder
            if (!videoHolders.containsKey(currentPosition)) {
                val firstAvailable = videoHolders.keys.firstOrNull()
                if (firstAvailable != null) {
                    Log.d(TAG, "Fixing currentPosition to first available: $firstAvailable")
                    currentPosition = firstAvailable
                }
            }
        }
    }

    // Helper method to pause all videos
    private fun pauseAllVideos() {
        Log.d(TAG, "pauseAllVideos: currentPosition=$currentPosition, visibleItems=$visibleItems")
        videoHolders.values.forEach { holder ->
            holder.pauseVideo()
        }
    }

    // Helper method to pause all videos except specified position
    private fun pauseAllVideosExcept(exceptPosition: Int) {
        Log.d(
            TAG,
            "pauseAllVideosExcept: exceptPosition=$exceptPosition, totalHolders=${videoHolders.size}"
        )
        videoHolders.forEach { (position, holder) ->
            if (position != exceptPosition) {
                Log.d(TAG, "Pausing video at position $position")
                holder.pauseVideo()
            } else {
                Log.d(TAG, "Keeping video at position $position playing")
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.layoutPosition
        if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
            videoHolders[position]?.release()
            videoHolders.remove(position)
        }
    }

    inner class VideoHolder(
        private val binding: ItemVideoFilterBinding, private var player: ExoPlayer?
    ) {
        private var controlsVisible = false
        private var fadeRunnable: Runnable? = null
        private var wasPlayingBeforePause = false

        fun playVideo() {
           if (isHomeTabActive()){
               player?.let {
                   if (!it.isPlaying) {
                       it.playWhenReady = true
                       binding.btnPlayPause.setImageResource(R.drawable.ic_pause_home)
                   }
               }
           }
        }

        fun pauseVideo() {
            player?.let {
                wasPlayingBeforePause = it.isPlaying
                if (it.isPlaying) {
                    it.playWhenReady = false
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play_home)
                }
            }
        }

        fun resumeVideo() {
            player?.let {
                if (wasPlayingBeforePause) {
                    it.playWhenReady = true
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause_home)
                }
            }
        }

        fun togglePlayPause() {
            player?.let {
                if (it.isPlaying) {
                    pauseVideo()
                } else {
                    playVideo()
                }
            }
        }

        fun showControls() {
            // Cancel any pending fade out
            fadeRunnable?.let { handler.removeCallbacks(it) }

            // Show controls
            binding.btnPlayPause.visibility = View.VISIBLE
            binding.btnPlayPause.alpha = 1f
            controlsVisible = true

            // Schedule fade out after 3 seconds
            fadeRunnable = Runnable {
                fadeOutControls()
            }
            handler.postDelayed(fadeRunnable!!, 3000)
        }

        private fun fadeOutControls() {
            if (!controlsVisible) return

            ObjectAnimator.ofFloat(binding.btnPlayPause, "alpha", 1f, 0f).apply {
                duration = 500
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.btnPlayPause.visibility = View.GONE
                        controlsVisible = false
                    }
                })
                start()
            }
        }

        fun release() {
            fadeRunnable?.let { handler.removeCallbacks(it) }
            player?.release()
            player = null
        }
    }

    companion object {
        const val ORIENTATION_VERTICAL = ViewPager2.ORIENTATION_VERTICAL
    }

    // Method to reset shuffle and force new shuffle
    fun resetShuffle() {
        Log.d(TAG, "Resetting shuffle - will shuffle again on next submitData")
        isDataShuffled = false
        shuffledData = emptyList()
    }

    // Method to force new shuffle with current data
    fun forceNewShuffle() {
        Log.d(TAG, "Forcing new shuffle with current data")
        if (list.isNotEmpty()) {
            shuffledData = list.shuffled()
            isDataShuffled = true
            Log.d(TAG, "New shuffle applied to ${shuffledData.size} items")
        }
    }

    // Method to check if data has changed
    fun hasDataChanged(newData: List<VideoFilterItem>): Boolean {
        if (list.size != newData.size) return true

        return list.zip(newData).any { (old, new) ->
            old.videoUrl != new.videoUrl || old.filterName != new.filterName || old.accessKey != new.accessKey
        }
    }

    // Method to submit data only if it has changed
    fun submitDataIfChanged(newData: List<VideoFilterItem>) {
        if (hasDataChanged(newData)) {
            Log.d(TAG, "Data changed, submitting new data")
            submitData(newData)
        } else {
            Log.d(TAG, "Data unchanged, skipping submitData")
        }
    }

    // Check whether Home tab is active before allowing playback
    private fun isHomeTabActive(): Boolean {
        return if (context is MainActivity) {
            (context as MainActivity).tabSelection == 0
        } else {
            false
        }

    }

    // Extension functions for view visibility tracking
    private fun View.addOnAttachToWindowListener(callback: () -> Unit) {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                callback()
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
    }

    private fun View.addOnDetachFromWindowListener(callback: () -> Unit) {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                callback()
            }
        })
    }
}

