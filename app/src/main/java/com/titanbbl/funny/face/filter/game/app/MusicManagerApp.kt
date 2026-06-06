package com.titanbbl.funny.face.filter.game.app

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.titanbbl.funny.face.filter.game.model.api.Song
import com.google.gson.Gson
import java.io.File
import com.titanbbl.funny.face.filter.game.BuildConfig

/**
 * MusicManager - Quản lý nhạc với SharedPreferences và MediaPlayer
 */
object MusicManagerApp {
    
    private const val PREF_NAME = "music_preferences"
    private const val KEY_SELECTED_SONG = "selected_song"
    private const val KEY_LAST_POSITION = "last_position"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_VOLUME = "volume"
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private var isPlaying = false
    private var currentPosition = 0
    private var volume = 1.0f
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gson: Gson
    
    /**
     * Khởi tạo MusicManager
     */
    fun init(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            gson = Gson()
            loadSavedState()
        }
    }
    
    /**
     * Lưu bài hát được chọn vào SharedPreferences
     */
    fun saveSelectedSong(song: Song) {
        try {
            // Kiểm tra xem có phải bài hát mới không
            val isNewSong = currentSong?.id != song.id
            
            currentSong = song
            val songJson = gson.toJson(song)
            sharedPreferences.edit()
                .putString(KEY_SELECTED_SONG, songJson)
                .putBoolean(KEY_IS_PLAYING, false)
                .putInt(KEY_LAST_POSITION, 0)
                .apply()
            
            // Nếu có bài hát mới, reset MediaPlayer và clear local paths
            if (isNewSong) {
                // Reset MediaPlayer
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer?.stop()
                        mediaPlayer?.reset()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                        currentPosition = 0
                        
                        Log.d("MusicManager", "Reset MediaPlayer for new song: ${song.title}")
                    } catch (e: Exception) {
                        Log.e("MusicManager", "Error resetting MediaPlayer", e)
                    }
                }
                
                // Clear local music paths để force tìm kiếm file mới
                clearAllLocalMusicPaths()
                Log.d("MusicManager", "Cleared local music paths for new song: ${song.title}")
            }
            
            Log.d("MusicManager", "Saved song: ${song.title}")
        } catch (e: Exception) {
            Log.e("MusicManager", "Error saving song", e)
        }
    }
    
    /**
     * Lưu đường dẫn local của nhạc
     */
    fun saveLocalMusicPath(songId: Int, localPath: String) {
        try {
            val key = "local_path_$songId"
            sharedPreferences.edit().putString(key, localPath).apply()
            Log.d("MusicManager", "Saved local music path for song $songId: $localPath")
        } catch (e: Exception) {
            Log.e("MusicManager", "Error saving local music path", e)
        }
    }
    
    /**
     * Lấy đường dẫn local của nhạc từ SharedPreferences
     */
    fun getLocalMusicPathFromPrefs(songId: Int): String? {
        try {
            val key = "local_path_$songId"
            return sharedPreferences.getString(key, null)
        } catch (e: Exception) {
            Log.e("MusicManager", "Error getting local music path from prefs", e)
            return null
        }
    }
    
    /**
     * Lấy bài hát đã lưu từ SharedPreferences
     */
    fun getSelectedSong(): Song? {
        if (currentSong == null) {
            try {
                val songJson = sharedPreferences.getString(KEY_SELECTED_SONG, null)
                if (songJson != null) {
                    currentSong = gson.fromJson(songJson, Song::class.java)
                    Log.d("MusicManager", "Loaded song: ${currentSong?.title}")
                }
            } catch (e: Exception) {
                Log.e("MusicManager", "Error loading song", e)
            }
        }
        return currentSong
    }
    
    /**
     * Phát nhạc
     */
    fun playMusic(context: Context, onPlaybackComplete: (() -> Unit)? = null) {
        val song = getSelectedSong() ?: return
        
        try {
            // Kiểm tra xem có phải bài hát mới không
            val isNewSong = currentSong?.id != song.id
            
            // Tạo MediaPlayer mới nếu cần hoặc nếu có bài hát mới
            if (mediaPlayer == null || isNewSong) {
                // Release MediaPlayer cũ nếu có
                mediaPlayer?.release()
                
                // Tạo MediaPlayer mới
                mediaPlayer = MediaPlayer()
                setupMediaPlayerListeners(onPlaybackComplete)
                
                Log.d("MusicManager", "Created new MediaPlayer for song: ${song.title}")
            }
            
            // Reset player nếu đang phát hoặc có bài hát mới
            if (isPlaying || isNewSong) {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                isPlaying = false
            }
            
            // Tìm đường dẫn local trước, nếu không có thì dùng URL gốc
            val localPath = getLocalMusicPath(context, song)
            val dataSource = localPath ?: song.url
            
            Log.d("MusicManager", "Playing music from: $dataSource")
            
            // Set data source và prepare (thêm header khi phát từ URL BunnyCDN)
            if (localPath == null && song.url.startsWith("http")) {
                try {
                    val headers = hashMapOf("accessKey" to BuildConfig.BUNNY_CDN_ACCESS_KEY)
                    mediaPlayer?.setDataSource(context, Uri.parse(song.url), headers)
                } catch (e: Exception) {
                    Log.w("MusicManager", "Header setDataSource failed, fallback to plain URL", e)
                    mediaPlayer?.setDataSource(song.url)
                }
            } else {
                mediaPlayer?.setDataSource(dataSource)
            }
            mediaPlayer?.prepareAsync()
            
            // Set volume
            mediaPlayer?.setVolume(volume, volume)
            
            // Set completion listener
            mediaPlayer?.setOnPreparedListener { mp ->
                mp.start()
                isPlaying = true
                currentPosition = 0
                
                // Lưu trạng thái
                savePlaybackState()
                
                Log.d("MusicManager", "Started playing: ${song.title} from $dataSource")
            }
            
        } catch (e: Exception) {
            Log.e("MusicManager", "Error playing music", e)
        }
    }
    
    /**
     * Pause nhạc
     */
    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                currentPosition = mediaPlayer?.currentPosition ?: 0
                mediaPlayer?.pause()
                isPlaying = false
                
                // Lưu trạng thái
                savePlaybackState()
                
                Log.d("MusicManager", "Paused music at position: $currentPosition")
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error pausing music", e)
        }
    }
    
    /**
     * Resume nhạc
     */
    fun resumeMusic() {
        try {
            if (mediaPlayer != null && !isPlaying) {
                mediaPlayer?.start()
                isPlaying = true
                
                // Lưu trạng thái
                savePlaybackState()
                
                Log.d("MusicManager", "Resumed music")
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error resuming music", e)
        }
    }
    
    /**
     * Stop nhạc
     */
    fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            isPlaying = false
            currentPosition = 0
            
            // Lưu trạng thái
            savePlaybackState()
            
            Log.d("MusicManager", "Stopped music")
        } catch (e: Exception) {
            Log.e("MusicManager", "Error stopping music", e)
        }
    }
    
    /**
     * Seek to position
     */
    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
            currentPosition = position
            savePlaybackState()
            
            Log.d("MusicManager", "Seeked to position: $position")
        } catch (e: Exception) {
            Log.e("MusicManager", "Error seeking", e)
        }
    }
    
    /**
     * Set volume
     */
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
        sharedPreferences.edit().putFloat(KEY_VOLUME, volume).apply()
        
        Log.d("MusicManager", "Volume set to: $volume")
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: currentPosition
    }
    
    /**
     * Get duration
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
    
    /**
     * Check if playing
     */
    fun isPlaying(): Boolean {
        return isPlaying && mediaPlayer?.isPlaying == true
    }
    
    /**
     * Get current song
     */
    fun getCurrentSong(): Song? = currentSong
    
    /**
     * Kiểm tra xem nhạc có sẵn local không
     */
    fun isLocalMusicAvailable(context: Context, song: Song): Boolean {
        return getLocalMusicPath(context, song) != null
    }
    
    /**
     * Lấy đường dẫn local của nhạc hiện tại
     */
    fun getCurrentLocalMusicPath(context: Context): String? {
        val song = getCurrentSong() ?: return null
        return getLocalMusicPath(context, song)
    }
    
    /**
     * Clear selected song
     */
    fun clearSelectedSong() {
        currentSong = null
        sharedPreferences.edit().remove(KEY_SELECTED_SONG).apply()
        Log.d("MusicManager", "Cleared selected song")
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            currentPosition = 0
            
            Log.d("MusicManager", "Released resources")
        } catch (e: Exception) {
            Log.e("MusicManager", "Error releasing resources", e)
        }
    }
    
    /**
     * Force refresh việc tìm kiếm file local
     */
    fun refreshLocalMusicPaths(context: Context) {
        try {
            val song = getCurrentSong() ?: return
            
            // Xóa path đã lưu để force tìm kiếm lại
            sharedPreferences.edit().remove("local_path_${song.id}").apply()
            
            // Tìm kiếm lại
            val newPath = getLocalMusicPath(context, song)
            if (newPath != null) {
                Log.d("MusicManager", "Refreshed local music path: $newPath")
            } else {
                Log.d("MusicManager", "No local music found after refresh")
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error refreshing local music paths", e)
        }
    }
    
    /**
     * Clear tất cả local music paths đã lưu (để force tìm kiếm lại)
     */
    fun clearAllLocalMusicPaths() {
        try {
            // Lấy tất cả keys trong SharedPreferences
            val allKeys = sharedPreferences.all.keys.toList()
            
            // Xóa tất cả local path keys
            val localPathKeys = allKeys.filter { it.startsWith("local_path_") }
            if (localPathKeys.isNotEmpty()) {
                sharedPreferences.edit().apply {
                    localPathKeys.forEach { key ->
                        remove(key)
                    }
                }.apply()
                
                Log.d("MusicManager", "Cleared ${localPathKeys.size} local music paths")
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error clearing all local music paths", e)
        }
    }
    
    /**
     * Setup MediaPlayer listeners
     */
    private fun setupMediaPlayerListeners(onPlaybackComplete: (() -> Unit)?) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
            currentPosition = 0
            savePlaybackState()
            
            Log.d("MusicManager", "Playback completed")
            onPlaybackComplete?.invoke()
        }
        
        mediaPlayer?.setOnErrorListener { mp, what, extra ->
            Log.e("MusicManager", "MediaPlayer error: what=$what, extra=$extra")
            isPlaying = false
            savePlaybackState()
            true
        }
    }
    
    /**
     * Lưu trạng thái phát nhạc
     */
    private fun savePlaybackState() {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_PLAYING, isPlaying)
            .putInt(KEY_LAST_POSITION, currentPosition)
            .apply()
    }
    
    /**
     * Tìm đường dẫn local của nhạc
     */
    private fun getLocalMusicPath(context: Context, song: Song): String? {
        try {
            // Đầu tiên, kiểm tra SharedPreferences
            val savedLocalPath = getLocalMusicPathFromPrefs(song.id)
            if (savedLocalPath != null) {
                val savedFile = File(savedLocalPath)
                if (savedFile.exists()) {
                    Log.d("MusicManager", "Using saved local path: $savedLocalPath")
                    return savedLocalPath
                } else {
                    // File không tồn tại, xóa path đã lưu
                    sharedPreferences.edit().remove("local_path_${song.id}").apply()
                }
            }
            
            // Tìm trong thư mục music của app - ƯU TIÊN TÌM THEO ID TRƯỚC
            val musicDir = File(context.filesDir, "music")
            if (musicDir.exists() && musicDir.isDirectory) {
                // Tìm file nhạc theo ID trước (chính xác nhất)
                val musicFilesById = musicDir.listFiles { file ->
                    file.isFile && file.name.startsWith("${song.id}_") && 
                    (file.name.endsWith(".mp3") || file.name.endsWith(".wav") || file.name.endsWith(".m4a"))
                }
                
                if (musicFilesById != null && musicFilesById.isNotEmpty()) {
                    val localPath = musicFilesById[0].absolutePath
                    Log.d("MusicManager", "Found local music file by ID: $localPath")
                    // Lưu đường dẫn này để lần sau sử dụng
                    saveLocalMusicPath(song.id, localPath)
                    return localPath
                }
                
                // Nếu không tìm thấy theo ID, tìm theo tên bài hát
                val musicFilesByName = musicDir.listFiles { file ->
                    file.isFile && file.name.contains(song.title, ignoreCase = true) && 
                    (file.name.endsWith(".mp3") || file.name.endsWith(".wav") || file.name.endsWith(".m4a"))
                }
                
                if (musicFilesByName != null && musicFilesByName.isNotEmpty()) {
                    val localPath = musicFilesByName[0].absolutePath
                    Log.d("MusicManager", "Found local music file by title: $localPath")
                    // Lưu đường dẫn này để lần sau sử dụng
                    saveLocalMusicPath(song.id, localPath)
                    return localPath
                }
            }
            
            // Nếu không tìm thấy trong thư mục music, thử tìm trong cache
            val cacheDir = context.cacheDir
            val cacheFilesById = cacheDir.listFiles { file ->
                file.isFile && file.name.startsWith("${song.id}_") && 
                (file.name.endsWith(".mp3") || file.name.endsWith(".wav") || file.name.endsWith(".m4a"))
            }
            
            if (cacheFilesById != null && cacheFilesById.isNotEmpty()) {
                val localPath = cacheFilesById[0].absolutePath
                Log.d("MusicManager", "Found local music file in cache by ID: $localPath")
                // Lưu đường dẫn này để lần sau sử dụng
                saveLocalMusicPath(song.id, localPath)
                return localPath
            }
            
            // Cuối cùng, tìm trong cache theo tên
            val cacheFilesByName = cacheDir.listFiles { file ->
                file.isFile && file.name.contains(song.title, ignoreCase = true) && 
                (file.name.endsWith(".mp3") || file.name.endsWith(".wav") || file.name.endsWith(".m4a"))
            }
            
            if (cacheFilesByName != null && cacheFilesByName.isNotEmpty()) {
                val localPath = cacheFilesByName[0].absolutePath
                Log.d("MusicManager", "Found local music file in cache by title: $localPath")
                // Lưu đường dẫn này để lần sau sử dụng
                saveLocalMusicPath(song.id, localPath)
                return localPath
            }
            
            Log.d("MusicManager", "No local music file found for song ID: ${song.id}, title: ${song.title}")
            return null
            
        } catch (e: Exception) {
            Log.e("MusicManager", "Error finding local music path", e)
            return null
        }
    }
    
    /**
     * Load trạng thái đã lưu
     */
    private fun loadSavedState() {
        try {
            isPlaying = sharedPreferences.getBoolean(KEY_IS_PLAYING, false)
            currentPosition = sharedPreferences.getInt(KEY_LAST_POSITION, 0)
            volume = sharedPreferences.getFloat(KEY_VOLUME, 1.0f)
            
            Log.d("MusicManager", "Loaded state: playing=$isPlaying, position=$currentPosition, volume=$volume")
        } catch (e: Exception) {
            Log.e("MusicManager", "Error loading saved state", e)
        }
    }
}