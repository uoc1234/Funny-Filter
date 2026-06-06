package com.titanbbl.funny.face.filter.game.data.repository

import android.util.Log
import com.titanbbl.funny.face.filter.game.BuildConfig
import com.titanbbl.funny.face.filter.game.data.api.VideoApiService
import com.titanbbl.funny.face.filter.game.model.api.VideoItem
import com.titanbbl.funny.face.filter.game.model.VideoFilterItem
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class VideoRepository(
    private val apiService: VideoApiService
) {
    suspend fun getVideos(): MutableList<VideoFilterItem> {
        return try {
            val response = apiService.getVideos()
            Log.d(TAG, "Raw server response: ${response.items}")

            val mappedItems = response.items.map { it.toVideoFilterItem() }
            Log.d(TAG, "Mapped video items: $mappedItems")

            mappedItems.toMutableList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching videos", e)
            throw e
        }
    }

    private fun VideoItem.toVideoFilterItem(): VideoFilterItem {
        return VideoFilterItem(
            id = id,
            rank = rank,
            videoUrl = BuildConfig.BUNNY_CDN_VIDEO_URL + videoName,
            accessKey = BuildConfig.BUNNY_CDN_ACCESS_KEY,
            likes = formatNumber(likes),
            views = formatNumber(views),
            shares = formatNumber(shares),
            category = category,
            filterName = filterName
        )
    }

    private fun formatNumber(number: Int): String {
        if (number < 1000) return number.toString()

        val exp = floor(log10(number.toDouble()) / 3).toInt()
        val value = number / 10.0.pow(exp * 3)
        val roundedValue = "%.1f".format(value).trimEnd('0').trimEnd('.')

        return when (exp) {
            1 -> "${roundedValue}K"
            2 -> "${roundedValue}M"
            3 -> "${roundedValue}B"
            else -> number.toString()
        }
    }

    companion object {
        private const val TAG = "VideoRepository"
        private var instance: VideoRepository? = null

        fun getInstance(): VideoRepository {
            return instance ?: synchronized(this) {
                instance ?: VideoRepository(VideoApiService.create()).also { instance = it }
            }
        }
    }
} 