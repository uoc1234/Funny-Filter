package com.titanbbl.funny.face.filter.game.ui.component.main.fragments

import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.FragmentCollectionBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseFragment
import com.titanbbl.funny.face.filter.game.ui.bases.ext.goneView
import com.titanbbl.funny.face.filter.game.ui.bases.ext.visibleView
import com.titanbbl.funny.face.filter.game.ui.bases.ext.slideInUp
import com.titanbbl.funny.face.filter.game.ui.bases.ext.slideOutDown
import com.titanbbl.funny.face.filter.game.ui.component.main.MainActivity
import com.titanbbl.funny.face.filter.game.ui.component.main.adapter.VideoCollectionAdapter
import com.titanbbl.funny.face.filter.game.model.VideoItem
import com.titanbbl.funny.face.filter.game.utils.Routes
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.*

class CollectionFragment : BaseFragment<FragmentCollectionBinding>() {

    private lateinit var videoAdapter: VideoCollectionAdapter
    private val videoList = mutableListOf<VideoItem>()

    // Background executor for file operations
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Coroutine scope for background operations
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Loading state
    private var isLoading = false

    // Delete mode state
    private var isDeleteMode = false

    override fun getLayoutFragment(): Int {
        return R.layout.fragment_collection
    }

    override fun initViews() {
        super.initViews()
        setupRecyclerView()
        setupDeleteButton()
        loadVideosFromDirectory()
    }

    override fun onResume() {
        super.onResume()
        // Refresh video list when fragment becomes visible
        loadVideosFromDirectory()
    }

    /**
     * Handle back press when in delete mode
     */
    fun onBackPressed(): Boolean {
        return if (isDeleteMode) {
            toggleDeleteMode()
            true // Consume the back press
        } else {
            false // Let the system handle it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up resources
        backgroundScope.cancel()
        backgroundExecutor.shutdown()
        videoAdapter.cleanup()
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoCollectionAdapter(onItemClick = { videoItem ->
            // Handle video item click
            playVideo(videoItem)
        }, onSelectionChanged = { selectedCount ->
            // Update the count display
            updateSelectedCount(selectedCount)
        })

        mBinding.rcvVideoCollection.apply {
            adapter = videoAdapter
        }
    }

    private fun setupDeleteButton() {
        mBinding.imvDelete.setOnClickListener {
            toggleDeleteMode()
        }

        // Long press to select all videos when in delete mode
        mBinding.imvDelete.setOnLongClickListener {
            if (isDeleteMode) {
                videoAdapter.selectAll()
                true
            } else {
                false
            }
        }

        // Initially hide the count view
        mBinding.tvCountVideoDelete.visibility = android.view.View.GONE

        // Set click listener for count view to trigger deletion
        mBinding.tvCountVideoDelete.setOnClickListener {
            if (isDeleteMode) {
                deleteSelectedVideos()
            }
        }
        mBinding.tvCountVideoDelete.setBackgroundResource(R.drawable.bg_count_video_delete_inactive)
        mBinding.tvCountVideoDelete.setTextColor("#D7D4D4".toColorInt())
    }

    private fun toggleDeleteMode() {
        isDeleteMode = !isDeleteMode

        if (isDeleteMode) {
            // Enter delete mode
            mBinding.imvDelete.setImageResource(R.drawable.ic_close_collection)
            // Show delete counter with slide in
            mBinding.tvCountVideoDelete.slideInUp()
            videoAdapter.setSelectionMode(true)
            updateSelectedCount(0)
            // Hide bottom nav with slide out
//            (activity as MainActivity).mBinding.bottomNavigation.slideOutDown()
        } else {
            // Exit delete mode
            mBinding.imvDelete.setImageResource(R.drawable.ic_delete_24)
            // Hide delete counter with slide out and then gone
            mBinding.tvCountVideoDelete.slideOutDown()
            videoAdapter.setSelectionMode(false)
            // Clear any existing selections
            videoAdapter.deselectAll()
            // Show bottom nav with slide in
//            (activity as MainActivity).mBinding.bottomNavigation.slideInUp()
        }
    }

    private fun updateSelectedCount(count: Int) {
        mBinding.tvCountVideoDelete.text = "Selected $count"

        when (count) {
            0 -> {
                mBinding.tvCountVideoDelete.setBackgroundResource(R.drawable.bg_count_video_delete_inactive)
                mBinding.tvCountVideoDelete.setTextColor("#D7D4D4".toColorInt())
            }

            else -> {
                mBinding.tvCountVideoDelete.setBackgroundResource(R.drawable.bg_count_video_delete_active)
                mBinding.tvCountVideoDelete.setTextColor("#FFFFFF".toColorInt())
            }
        }
    }

    private fun deleteSelectedVideos() {
        val selectedItems = videoAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(context, getString(R.string.no_videos_selected), Toast.LENGTH_SHORT).show()
            return
        }

        // Show Figma-styled confirmation dialog
        com.titanbbl.funny.face.filter.game.ui.component.dialog.DialogConfirmDelete.show(
            requireContext(),
            title = "Delete Videos",
            message = getString(R.string.delete_file, selectedItems.size),
            onConfirm = { performDelete(selectedItems) },
            onCancel = { /* no-op */ }
        )
    }

    private fun performDelete(selectedItems: List<VideoItem>) {
        backgroundScope.launch {
            try {
                var deletedCount = 0

                selectedItems.forEach { videoItem ->
                    val file = File(videoItem.filePath)
                    if (file.exists() && file.delete()) {
                        deletedCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    if (deletedCount > 0) {
                        Toast.makeText(
                            context, "$deletedCount video(s) deleted", Toast.LENGTH_SHORT
                        ).show()
                        // Exit delete mode and refresh the list
                        toggleDeleteMode()
                        loadVideosFromDirectory()
                    } else {
                        Toast.makeText(context, "Failed to delete videos", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error deleting videos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadVideosFromDirectory() {
        // Prevent multiple simultaneous loads
        if (isLoading) return

        isLoading = true
        showLoadingState()

        // Use background scope for file operations
        backgroundScope.launch {
            try {
                val videos = loadVideosInBackground()

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    updateUIWithVideos(videos)
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error on main thread
                withContext(Dispatchers.Main) {
                    showErrorState()
                    isLoading = false
                }
            }
        }
    }

    private suspend fun loadVideosInBackground(): List<VideoItem> {
        return withContext(Dispatchers.IO) {
            val videos = mutableListOf<VideoItem>()

            try {
                val videoDir = File(context?.filesDir, "Videos")
                if (videoDir.exists() && videoDir.isDirectory) {
                    val videoFiles = videoDir.listFiles { file ->
                        file.isFile && (file.extension.equals(
                            "mp4", ignoreCase = true
                        ) || file.extension.equals(
                            "mov", ignoreCase = true
                        ) || file.extension.equals("avi", ignoreCase = true))
                    }

                    videoFiles?.forEach { file ->
                        val videoItem = VideoItem(
                            id = file.absolutePath.hashCode().toLong(),
                            filePath = file.absolutePath,
                            fileName = file.name,
                            fileSize = file.length(),
                            dateCreated = file.lastModified(),
                            duration = 0L // You can extract duration if needed
                        )
                        videos.add(videoItem)
                    }

                    // Sort by date created (newest first)
                    videos.sortByDescending { it.dateCreated }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            videos
        }
    }

    private fun updateUIWithVideos(videos: List<VideoItem>) {
        videoList.clear()
        videoList.addAll(videos)

        // Update adapter
        videoAdapter.submitList(videoList.toList())

        if (videoList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showLoadingState() {
        // Show loading indicator if you have one
        // For now, we'll just ensure empty state is hidden
        hideEmptyState()
    }

    private fun showErrorState() {
        // Show error state
        Toast.makeText(context, "Error loading videos", Toast.LENGTH_SHORT).show()
        showEmptyState()
    }

    private fun showEmptyState() {
        mBinding.imvDelete.goneView()
        mBinding.llNoCollection.visibleView()
    }

    private fun hideEmptyState() {
        mBinding.imvDelete.visibleView()
        mBinding.llNoCollection.visibility = android.view.View.GONE
    }

    private fun playVideo(videoItem: VideoItem) {
        try {
            activity?.let { Routes.startResultActivity(it, videoItem.filePath) }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot play video", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance() = CollectionFragment()
    }
} 