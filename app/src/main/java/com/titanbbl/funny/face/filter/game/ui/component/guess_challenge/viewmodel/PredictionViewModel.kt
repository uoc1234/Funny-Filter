package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem
import com.titanbbl.funny.face.filter.game.data.repository.PredictionRepository
import com.titanbbl.funny.face.filter.game.ui.bases.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PredictionViewModel(
    private val predictionRepository: PredictionRepository,
) : BaseViewModel() {

    private val _predictions = MutableStateFlow<MutableList<PredictionResponseItem>>(mutableListOf())
    val predictions: StateFlow<MutableList<PredictionResponseItem>> = _predictions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPredictions()
    }

    fun loadPredictions() {
        Log.d(TAG, "Starting to load predictions")
        bgScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                Log.d(TAG, "Loading state set to true")
                
                val result = predictionRepository.getPredictions()
                Log.d(TAG, "Successfully loaded ${result.size} predictions")
                _predictions.value = result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading predictions", e)
                _error.value = e.message ?: "An error occurred while loading predictions"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Loading state set to false")
            }
        }
    }

    fun retryLoading() {
        Log.d(TAG, "Retrying prediction loading")
        loadPredictions()
    }

    companion object {
        private const val TAG = "PredictionViewModel"

        fun provideFactory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PredictionViewModel(
                    predictionRepository = PredictionRepository.getInstance()
                )
            }
        }
    }
} 