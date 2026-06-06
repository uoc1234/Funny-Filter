package com.titanbbl.funny.face.filter.game.ui.component.main.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.titanbbl.funny.face.filter.game.data.repository.VideoRepository
import com.titanbbl.funny.face.filter.game.ui.bases.BaseViewModel
import com.titanbbl.funny.face.filter.game.model.VideoFilterItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoRepository: VideoRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(savedStateHandle) {

    private val _videos = MutableStateFlow<MutableList<VideoFilterItem>>(mutableListOf())
    val videos: StateFlow<MutableList<VideoFilterItem>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadVideos()
    }

    fun loadVideos() {
        Log.d(TAG, "Starting to load videos")
        bgScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                Log.d(TAG, "Loading state set to true")
                
                val result = videoRepository.getVideos()
                Log.d(TAG, "Successfully loaded ${result.size} videos")
                result.shuffled()
                result.shuffled()
                _videos.value = result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading videos", e)
                _error.value = e.message ?: "An error occurred while loading videos"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Loading state set to false")
            }
        }
    }

    fun retryLoading() {
        Log.d(TAG, "Retrying video loading")
        loadVideos()
    }
    
    companion object {
        private const val TAG = "HomeViewModel"
        
        fun provideFactory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    videoRepository = VideoRepository.getInstance(),
                    savedStateHandle = SavedStateHandle()
                )
            }
        }
    }
} 