package com.titanbbl.funny.face.filter.game.data.repository

import com.titanbbl.funny.face.filter.game.data.api.MusicApiService
import com.titanbbl.funny.face.filter.game.model.api.Song

class MusicRepository(
    private val apiService: MusicApiService
) {
    suspend fun getMusic(): MutableList<Song> {
        return apiService.getMusic().songs.toMutableList()
    }

    companion object {
        private const val TAG = "PredictionRepository"
        private var instance: MusicRepository? = null

        fun getInstance(): MusicRepository {
            return instance ?: synchronized(this) {
                instance ?: MusicRepository(MusicApiService.create()).also { instance = it }
            }
        }
    }
} 