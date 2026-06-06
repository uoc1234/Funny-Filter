package com.titanbbl.funny.face.filter.game.data.repository

import android.util.Log
import com.titanbbl.funny.face.filter.game.data.api.PredictionApiService
import com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem

class PredictionRepository(
    private val apiService: PredictionApiService
) {
    suspend fun getPredictions(): MutableList<PredictionResponseItem> {
        return try {
            val response = apiService.getPredictions()
            Log.d(TAG, "Raw server response: ${response.size} items")

            response.toMutableList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching predictions", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "PredictionRepository"
        private var instance: PredictionRepository? = null

        fun getInstance(): PredictionRepository {
            return instance ?: synchronized(this) {
                instance ?: PredictionRepository(PredictionApiService.create()).also { instance = it }
            }
        }
    }
} 