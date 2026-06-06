package com.titanbbl.funny.face.filter.game.ui.component.main.fragments

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.FragmentHomeBinding
import com.titanbbl.funny.face.filter.game.model.VideoFilterItem
import com.titanbbl.funny.face.filter.game.ui.bases.BaseFragment
import com.titanbbl.funny.face.filter.game.ui.component.main.MainActivity
import com.titanbbl.funny.face.filter.game.ui.component.main.adapter.VideoFilterAdapter
import com.titanbbl.funny.face.filter.game.ui.component.main.viewmodel.HomeViewModel
import com.titanbbl.funny.face.filter.game.utils.PermissionUtils
import com.titanbbl.funny.face.filter.game.utils.Routes
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.provideFactory() }
    private lateinit var videoAdapter: VideoFilterAdapter

    override fun getLayoutFragment(): Int = R.layout.fragment_home

    override fun initViews() {
        initViewPager()
        initSwipeRefresh()
    }

    override fun onResizeViews() {
        // Handle view resizing if needed
    }

    override fun onClickViews() {
        mBinding.apply {
            tvDirection.setOnClickListener {
                tvDirection.setBackgroundResource(R.drawable.bg_underline)
                tvSuggestion.background = null
                videoAdapter.shuffleData()
            }

            tvSuggestion.setOnClickListener {
                tvSuggestion.setBackgroundResource(R.drawable.bg_underline)
                tvDirection.background = null
                videoAdapter.shuffleData()
            }
        }
    }

    // "LIP_FALL_CHALLENGE_FACE_PUZZLE" -> {
//                // TODO: Replace with your actual Face Puzzle Activity
//                Intent().apply {
//                    putExtra("filter_name", "Face puzzle")
//                    putExtra("category", type)
//                }
//            }
//
//            "LIP_FALL_CHALLENGE_ZOOM_PUZZLE" -> {
//                // TODO: Replace with your actual Zoom Puzzle Activity
//                Intent().apply {
//                    putExtra("filter_name", "Zoom puzzle")
//                    putExtra("category", type)
//                }
//            }
    override fun observerData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.videos.collect { videos ->
                        if (videos.isNotEmpty()) {
                            val filteredVideos = videos.filterNot {
                                it.category == "LIP_FALL_CHALLENGE_FACE_PUZZLE" || it.category == "LIP_FALL_CHALLENGE_ZOOM_PUZZLE"
                            }
                            // Use submitDataIfChanged to prevent unnecessary reloading
                            videoAdapter.submitDataIfChanged(filteredVideos)
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        mBinding.apply {
                            progressBarLoading.visibility =
                                if (isLoading) View.VISIBLE else View.GONE
                            swipeRefresh.isRefreshing = isLoading
                        }
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            mBinding.apply {
                                errorLayout.visibility = View.VISIBLE
                                contentLayout.visibility = View.GONE
                            }
                        } ?: run {
                            mBinding.apply {
                                errorLayout.visibility = View.GONE
                                contentLayout.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initViewPager() {
        videoAdapter = VideoFilterAdapter { videoFilterItem ->
            handleTryNowClick(videoFilterItem)
        }
        mBinding.viewPager.apply {
            adapter = videoAdapter
            orientation = VideoFilterAdapter.ORIENTATION_VERTICAL
            offscreenPageLimit = 1
        }

        // Connect adapter to ViewPager2 for auto-play/pause functionality
        videoAdapter.attachToViewPager(mBinding.viewPager)
    }

    private fun initSwipeRefresh() {
        mBinding.swipeRefresh.setOnRefreshListener {
            viewModel.loadVideos()
        }
    }

    private fun handleTryNowClick(videoFilterItem: VideoFilterItem) {
        activity?.let { activity ->
            // Check for required permissions before starting the activity
            if (PermissionUtils.hasRequiredPermissions(activity)) {
                // Permissions already granted, start activity
                Routes.startFilterActivity(videoFilterItem.category, activity)
            } else {
                // Request required permissions
                PermissionUtils.requestRequiredPermissions(this)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionUtils.handlePermissionsResult(requestCode, grantResults)) {
            // Permissions granted, start activity

        } else {
            // Permissions denied, show message
            activity?.let { PermissionUtils.showPermissionsDeniedMessage(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        // Debug current state before pausing
        videoAdapter.debugCurrentState()
        // Pause all videos when fragment is paused
        videoAdapter.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Fix any inconsistent state before resuming
        videoAdapter.fixInconsistentState()
        // Debug current state before resuming
        videoAdapter.debugCurrentState()
        // Resume current video when fragment is resumed
        if (activity is MainActivity) {
            if ((activity as MainActivity).tabSelection == 0) {
                videoAdapter.onResume()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Additional safety: pause all videos when fragment stops
        videoAdapter.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up adapter when view is destroyed
        videoAdapter.onPause()
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
} 