package com.titanbbl.funny.face.filter.game.ui.component.main.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.*
import java.io.File
import android.media.MediaMetadataRetriever
import java.util.concurrent.ConcurrentHashMap

class BitmapCache private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: BitmapCache? = null
        
        fun getInstance(): BitmapCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BitmapCache().also { INSTANCE = it }
            }
        }
    }
    
    // Cache cho Bitmap với LRU strategy
    private val bitmapCache: LruCache<String, Bitmap> = LruCache(50) // Cache tối đa 50 Bitmap
    
    // Cache cho các request đang xử lý để tránh duplicate requests
    private val pendingRequests = ConcurrentHashMap<String, Deferred<Bitmap?>>()
    
    // Coroutine scope cho background operations
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Lấy Bitmap từ cache hoặc load từ video file
     */
    suspend fun getBitmap(context: Context, videoPath: String, isLastFrame: Boolean = true): Bitmap? {
        val cacheKey = "${videoPath}_${if (isLastFrame) "last" else "first"}"
        
        // Kiểm tra cache trước
        bitmapCache.get(cacheKey)?.let { return it }
        
        // Kiểm tra xem có request đang xử lý không
        pendingRequests[cacheKey]?.let { deferred ->
            return deferred.await()
        }
        
        // Tạo request mới
        val deferred = backgroundScope.async {
            try {
                val bitmap = if (isLastFrame) {
                    extractLastFrameBitmap(videoPath)
                } else {
                    extractFirstFrameBitmap(videoPath)
                }
                
                // Cache bitmap nếu thành công
                bitmap?.let { bitmapCache.put(cacheKey, it) }
                
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                // Xóa request khỏi pending list
                pendingRequests.remove(cacheKey)
            }
        }
        
        pendingRequests[cacheKey] = deferred
        return deferred.await()
    }
    
    /**
     * Lấy frame cuối cùng từ video
     */
    private fun extractLastFrameBitmap(videoPath: String): Bitmap? {
        return try {
            val file = File(videoPath)
            if (!file.exists()) return null
            
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            // Lấy duration của video
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            
            // Lấy frame cuối cùng (trừ đi 100ms để tránh lấy frame cuối cùng có thể bị đen)
            val lastFrameTime = if (durationMs > 100) durationMs - 100 else 0L
            
            // Lấy frame tại thời điểm cuối
            val bitmap = retriever.getFrameAtTime(lastFrameTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Lấy frame đầu tiên từ video
     */
    private fun extractFirstFrameBitmap(videoPath: String): Bitmap? {
        return try {
            val file = File(videoPath)
            if (!file.exists()) return null
            
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            // Lấy frame đầu tiên
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Clear cache khi cần thiết
     */
    fun clearCache() {
        bitmapCache.evictAll()
        pendingRequests.clear()
    }
    
    /**
     * Remove specific item from cache
     */
    fun removeFromCache(videoPath: String) {
        val lastFrameKey = "${videoPath}_last"
        val firstFrameKey = "${videoPath}_first"
        bitmapCache.remove(lastFrameKey)
        bitmapCache.remove(firstFrameKey)
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        backgroundScope.cancel()
        clearCache()
    }
}
