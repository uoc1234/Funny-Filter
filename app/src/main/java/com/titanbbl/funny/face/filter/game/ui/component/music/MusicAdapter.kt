package com.titanbbl.funny.face.filter.game.ui.component.music

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.BuildConfig
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ItemMusicBinding
import com.titanbbl.funny.face.filter.game.model.api.Song
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.ui.bases.ext.click
import com.titanbbl.funny.face.filter.game.ui.bases.ext.goneView
import com.titanbbl.funny.face.filter.game.ui.bases.ext.visibleView
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.experimental.and

class MusicAdapter(
    private val context: Context,
    private val musicList: MutableList<Song>,
    private val onItemClick: (Song, Int) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var selectedPosition = -1
    private var currentPlayer: ExoPlayer? = null
    private var currentPlayingPosition = -1
    private var recyclerView: RecyclerView? = null
    private var progressUpdateJob: Job? = null

    private var selectPosition = -1

    inner class MusicViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, position: Int) {
            binding.tvMusicTitle.text = song.title
            // Hiển thị thời gian ban đầu: "00:00 / duration"
            binding.tvMusicDuration.text = "00:00 / ${song.duration}"

            // Check if song is already downloaded
            val isDownloaded = isSongDownloaded(song)
            updateDownloadState(position, isDownloaded)

            // Set play/pause button state
            updatePlayButtonState(position)

            // Set play button click listener
            binding.btnPlay.click {
                if (currentPlayingPosition == position) {
                    // Nếu đang phát bài hát này thì pause
                    pauseMusic()
                } else {
                    if (isSongDownloaded(song)) {
                        // Nếu đã download thì phát (sẽ tự động dừng bài hát khác)
                        playMusic(song, position)
                    } else {
                        // Nếu chưa download thì download
                        downloadSong(song, position)
                    }
                }
            }

			if (song.isSelected == true) {
				binding.bgSelected.setBackgroundResource(R.drawable.bg_music_item_selected)
			} else {
				binding.bgSelected.setBackgroundResource(R.drawable.bg_music_item)
			}


			binding.bgSelected.click {
				val songAtPos = musicList[position]
				// Nếu chưa download thì tiến hành download và thoát
				if (!isSongDownloaded(songAtPos)) {
					downloadSong(songAtPos, position)
					return@click
				}
				// Nếu đã download, thực hiện chọn duy nhất
				val oldPosition = selectedPosition
				if (position == oldPosition) {
					return@click
				}
				if (oldPosition >= 0 && oldPosition < musicList.size) {
					musicList[oldPosition].isSelected = false
					notifyItemChanged(oldPosition)
				}
				selectedPosition = position
				musicList[selectedPosition].isSelected = true
				notifyItemChanged(selectedPosition)
				onItemClick(musicList[selectedPosition], selectedPosition)
			}
        }
        private fun updatePlayButtonState(position: Int) {
            if (position == currentPlayingPosition) {
                // Bài hát đang phát - hiển thị pause button
                binding.btnPlay.setImageResource(R.drawable.ic_pause)
            } else {
                // Bài hát khác - kiểm tra trạng thái download
                val song = musicList[position]
                if (isSongDownloaded(song)) {
                    binding.btnPlay.setImageResource(R.drawable.ic_play_item)
                } else {
                    binding.btnPlay.setImageResource(R.drawable.ic_download)
                }
            }
        }

        private fun updateDownloadState(position: Int, isDownloaded: Boolean) {
            if (isDownloaded) {
                // File đã download - hiển thị play button
                binding.btnPlay.setImageResource(R.drawable.ic_play_item)
            } else {
                // Chưa download - hiển thị download button
                binding.btnPlay.setImageResource(R.drawable.ic_download)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(musicList[position], position)
    }

    override fun getItemCount(): Int = musicList.size

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(position)
    }

    fun getSelectedMusic(): Song? {
        return if (selectedPosition >= 0 && selectedPosition < musicList.size) {
            musicList[selectedPosition]
        } else null
    }

    private fun playMusic(song: Song, position: Int) {
        try {
            // Release any existing player
            releaseMediaPlayer()

            // Get file path - using internal storage to avoid permissions
            val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
            val downloadDir = File(context.filesDir, "music")
            val songFile = File(downloadDir, fileName)




            try {
                // Initialize ExoPlayer with configuration
                currentPlayer = ExoPlayer.Builder(context).build().apply {
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_OFF
                    volume = 1f
                }

                Log.d("MusicAdapter", "ExoPlayer created successfully")

                // Create media source with better error handling
                val dataSourceFactory = DefaultDataSourceFactory(context, "FunnyFilter.Music")
                val fileUri = songFile.toUri()

                Log.d("MusicAdapter", "File URI: $fileUri")
                Log.d("MusicAdapter", "URI scheme: ${fileUri.scheme}")
                Log.d("MusicAdapter", "URI path: ${fileUri.path}")

                val mediaItem = MediaItem.fromUri(fileUri)
                val mediaSource =
                    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

                Log.d("MusicAdapter", "MediaSource created successfully")

                currentPlayer?.apply {
                    setMediaSource(mediaSource)
                    Log.d("MusicAdapter", "MediaSource set to player")

                    prepare()
                    Log.d("MusicAdapter", "Player prepare() called")

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            Log.d("MusicAdapter", "Playback state changed: $state")
                            when (state) {
                                Player.STATE_READY -> {
                                    Log.d("MusicAdapter", "Ready to play: ${song.title}")

                                    currentPlayingPosition = position
                                    notifyItemChanged(position)
                                    
                                    // Cập nhật thời gian ngay lập tức
                                    try {
                                        val currentPosition = currentPlayer?.currentPosition ?: 0
                                        val duration = currentPlayer?.duration ?: 0
                                        if (duration > 0) {
                                            updateTimeDisplay(position, currentPosition, duration)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MusicAdapter", "Error updating time in STATE_READY", e)
                                    }
                                    
                                    startProgressTracking(position)
                                }

                                Player.STATE_ENDED -> {
                                    Log.d("MusicAdapter", "Playback completed: ${song.title}")
                                    releaseMediaPlayer()
                                    notifyItemChanged(position)
                                }

                                Player.STATE_BUFFERING -> {
                                    Log.d("MusicAdapter", "Buffering: ${song.title}")
                                }

                                Player.STATE_IDLE -> {
                                    Log.d("MusicAdapter", "Player idle")
                                }
                            }
                        }

                        override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                            Log.e(
                                "MusicAdapter", "Playback error for ${song.title}: ${error.message}"
                            )
                            Log.e("MusicAdapter", "Error cause: ${error.cause}")
                            Log.e("MusicAdapter", "Error code: ${error.errorCode}")
                            handlePlaybackError(error, song, position)
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Log.d("MusicAdapter", "Playing state changed: $isPlaying")
                            if (isPlaying) {
                                currentPlayingPosition = position
                                Log.d("MusicAdapter", "Music is now playing: ${song.title}")
                                
                                // Cập nhật thời gian ngay khi bắt đầu phát
                                try {
                                    val currentPosition = currentPlayer?.currentPosition ?: 0
                                    val duration = currentPlayer?.duration ?: 0
                                    if (duration > 0) {
                                        updateTimeDisplay(position, currentPosition, duration)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MusicAdapter", "Error updating time in isPlaying", e)
                                }
                            }
                            notifyItemChanged(position)
                        }
                    })
                }

            } catch (e: Exception) {
                Log.e("MusicAdapter", "Error setting up media source for ${song.title}", e)
                e.printStackTrace()
                handlePlaybackError(e, song, position)
            }

        } catch (e: Exception) {
            Log.e("MusicAdapter", "Fatal error playing ${song.title}", e)
            e.printStackTrace()
            handlePlaybackError(e, song, position)
        }
    }

    private fun handlePlaybackError(error: Exception, song: Song, position: Int) {
        Log.e("MusicAdapter", "=== HANDLE PLAYBACK ERROR ===")
        Log.e("MusicAdapter", "Error type: ${error.javaClass.simpleName}")
        Log.e("MusicAdapter", "Error message: ${error.message}")
        Log.e("MusicAdapter", "Error cause: ${error.cause}")

        releaseMediaPlayer()
        currentPlayingPosition = -1
        notifyItemChanged(position)

        // If it's a file error, try re-downloading
        if (error.message?.contains("FileNotFoundException") == true || error.message?.contains("IOException") == true || error.message?.contains(
                "SecurityException"
            ) == true
        ) {
            Log.d("MusicAdapter", "File error detected, attempting re-download")
            val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
            val downloadDir = File(context.filesDir, "music")
            val songFile = File(downloadDir, fileName)

            if (songFile.exists()) {
                songFile.delete()
            }
            downloadSong(song, position)
        } else {
            // Other type of error - try to refresh file status
            Log.d("MusicAdapter", "Non-file error, refreshing file status")
            refreshFileStatusImmediately(song, position)
        }
    }


    fun getCurrentPlayingSong(): Song? {
        return if (currentPlayingPosition >= 0 && currentPlayingPosition < musicList.size) {
            musicList[currentPlayingPosition]
        } else null
    }

    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    fun updateMusicList(newMusicList: List<Song>) {
        val oldSize = musicList.size
        // Update the list
        musicList.clear()
        musicList.addAll(newMusicList)

        // Notify adapter of changes
        if (oldSize == 0) {
            notifyDataSetChanged()
        } else {
            notifyItemRangeChanged(0, oldSize)
            if (newMusicList.size > oldSize) {
                notifyItemRangeInserted(oldSize, newMusicList.size - oldSize)
            }
        }
    }

    /**
     * Kiểm tra xem bài hát đã được download chưa
     */
    private fun isSongDownloaded(song: Song): Boolean {
        val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
        val downloadDir = File(context.filesDir, "music")
        val songFile = File(downloadDir, fileName)
        return songFile.exists() && songFile.length() > 0
    }

    /**
     * Download bài hát về máy
     */
     fun downloadSong(song: Song, position: Int) {
        val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
        val downloadDir = File(context.filesDir, "music")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        // Create headers map for BunnyCDN access key
        val headers = HashMap<String, String>()
        headers["accessKey"] = BuildConfig.BUNNY_CDN_ACCESS_KEY

        PRDownloader.download(song.url, downloadDir.absolutePath, fileName)
            .setHeader("accessKey", BuildConfig.BUNNY_CDN_ACCESS_KEY).build()
            .setOnStartOrResumeListener {
                Log.d("MusicAdapter", "Download started for ${song.title}")
                // Update UI to show download started
                updateDownloadProgress(position, 0)
            }.setOnPauseListener {
                Log.d("MusicAdapter", "Download paused for ${song.title}")
            }.setOnCancelListener {
                Log.d("MusicAdapter", "Download cancelled for ${song.title}")
                updateDownloadError(position)
            }.setOnProgressListener { progress ->
                val progressPercent = ((progress.currentBytes * 100) / progress.totalBytes).toInt()
                updateDownloadProgress(position, progressPercent)

            }.start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    Log.d("MusicAdapter", "Download completed for ${song.title}")
                    onDownloadComplete(song, position)
                }

                override fun onError(error: Error) {
                    Log.e(
                        "MusicAdapter",
                        "Download error for ${song.title}: ${error.connectionException}"
                    )
                    updateDownloadError(position)
                }
            })
    }

    /**
     * Cập nhật progress download
     */
    private fun updateDownloadProgress(position: Int, progress: Int) {
        try {
            val holder = recyclerView?.findViewHolderForAdapterPosition(position)
            if (holder is MusicViewHolder) {
                holder.binding.btnPlay.goneView()

                if (progress == 0) {
                    holder.binding.ltvDownloading.apply {
                        visibleView()
                        playAnimation()
                    }
                }

            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating progress", e)
        }
    }

    /**
     * Xử lý khi download hoàn tất
     */
    private fun onDownloadComplete(song: Song, position: Int) {
        try {
            val holder = recyclerView?.findViewHolderForAdapterPosition(position)
            if (holder is MusicViewHolder) {

                holder.binding.ltvDownloading.apply {
                    goneView()
                    pauseAnimation()
                }

                holder.binding.btnPlay.visibleView()

                holder.binding.btnPlay.setImageResource(R.drawable.ic_play_item)

                // Giữ nguyên format thời gian "00:00 / duration"
                holder.binding.tvMusicDuration.text = "00:00 / ${song.duration}"

                // Lưu local path sau khi tải xong để phát lại về sau
                try {
                    val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
                    val downloadDir = File(context.filesDir, "music")
                    val songFile = File(downloadDir, fileName)
                    if (songFile.exists() && songFile.length() > 0) {
                        MusicManagerApp.saveLocalMusicPath(song.id, songFile.absolutePath)
                        Log.d("MusicAdapter", "Saved local path for ${song.title}: ${songFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e("MusicAdapter", "Failed to save local path after download", e)
                }

                // Sử dụng MediaScanner để scan file ngay lập tức
                scanFileWithMediaScanner(song, position)

                // Sau đó sử dụng retry system để kiểm tra file nhanh hơn
                checkFileStatusWithRetry(song, position)

                // Cuối cùng scan chi tiết hơn để đảm bảo (với delay ngắn hơn)
                scanAndUpdateFileStatus(song, position)
            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating download complete", e)
        }
    }

    /**
     * Xử lý khi download lỗi
     */
    private fun updateDownloadError(position: Int) {
        try {
            val holder = recyclerView?.findViewHolderForAdapterPosition(position)
            if (holder is MusicViewHolder) {

            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating download error", e)
        }
    }

        /**
     * Bắt đầu tracking progress của bài hát đang phát
     */
    private fun startProgressTracking(position: Int) {
        // Cập nhật thời gian ngay lập tức khi bắt đầu
        try {
            val currentPosition = currentPlayer?.currentPosition ?: 0
            val duration = currentPlayer?.duration ?: 0
            
            if (duration > 0) {
                updateTimeDisplay(position, currentPosition, duration)
            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating time immediately", e)
        }
        
        // Sau đó update mỗi 500ms thay vì 1000ms để mượt hơn
        progressUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (currentPlayer?.isPlaying == true && currentPlayingPosition == position) {
                try {
                    val currentPosition = currentPlayer?.currentPosition ?: 0
                    val duration = currentPlayer?.duration ?: 0
                    
                    if (duration > 0) {
                        updateTimeDisplay(position, currentPosition, duration)
                    }
                    
                    delay(500) // Update every 500ms for smoother experience
                } catch (e: Exception) {
                    Log.e("MusicAdapter", "Error updating time", e)
                    break
                }
            }
        }
    }

    /**
     * Cập nhật hiển thị thời gian
     */
    private fun updateTimeDisplay(position: Int, currentPosition: Long, duration: Long) {
        try {
            // Chỉ update thời gian khi đang phát bài hát này
            if (position == currentPlayingPosition) {
                val holder = recyclerView?.findViewHolderForAdapterPosition(position)
                if (holder is MusicViewHolder) {
                    val currentTime = formatTime(currentPosition)
                    val totalTime = formatTime(duration)
                    holder.binding.tvMusicDuration.text = "$currentTime / $totalTime"
                }
            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating time display", e)
        }
    }

    /**
     * Format thời gian từ milliseconds sang mm:ss
     */
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Pause music và dừng progress tracking
     */
    private fun pauseMusic() {
        currentPlayer?.pause()
        val oldPosition = currentPlayingPosition
        currentPlayingPosition = -1
        progressUpdateJob?.cancel()
        
        // Reset thời gian về trạng thái ban đầu
        if (oldPosition >= 0 && oldPosition < musicList.size) {
            val song = musicList[oldPosition]
            try {
                val holder = recyclerView?.findViewHolderForAdapterPosition(oldPosition)
                if (holder is MusicViewHolder) {
                    holder.binding.tvMusicDuration.text = "00:00 / ${song.duration}"
                }
            } catch (e: Exception) {
                Log.e("MusicAdapter", "Error resetting time display", e)
            }
        }
        
        // Cập nhật UI cho cả bài hát cũ và bài hát được chọn
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition)
        }
        if (selectedPosition >= 0) {
            notifyItemChanged(selectedPosition)
        }
    }

    /**
     * Release media player và cleanup
     */
    fun releaseMediaPlayer() {
        val oldPosition = currentPlayingPosition
        
        currentPlayer?.let { player ->
            player.stop()
            player.release()
            currentPlayer = null
            currentPlayingPosition = -1
        }
        progressUpdateJob?.cancel()
        
        // Reset thời gian về trạng thái ban đầu
        if (oldPosition >= 0 && oldPosition < musicList.size) {
            val song = musicList[oldPosition]
            try {
                val holder = recyclerView?.findViewHolderForAdapterPosition(oldPosition)
                if (holder is MusicViewHolder) {
                    holder.binding.tvMusicDuration.text = "00:00 / ${song.duration}"
                }
            } catch (e: Exception) {
                Log.e("MusicAdapter", "Error resetting time display in release", e)
            }
        }
    }

    /**
     * Scan file sau khi download và cập nhật UI
     */
    private fun scanAndUpdateFileStatus(song: Song, position: Int) {
        // Tạo coroutine scope để scan file
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // Giảm delay xuống để file có thể sử dụng nhanh hơn
                delay(200)

                val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
                val downloadDir = File(context.filesDir, "music")
                val songFile = File(downloadDir, fileName)

                // Kiểm tra file chi tiết hơn
                val fileExists = songFile.exists()
                val fileSize = songFile.length()
                val isValidFile = fileExists && fileSize > 1024 // Ít nhất 1KB
                val canRead = songFile.canRead()

                Log.d("MusicAdapter", "Scanning file: ${songFile.absolutePath}")
                Log.d(
                    "MusicAdapter",
                    "File exists: $fileExists, Size: $fileSize bytes, CanRead: $canRead"
                )

                // Update UI trên main thread
                withContext(Dispatchers.Main) {
                    try {
                        val holder = recyclerView?.findViewHolderForAdapterPosition(position)
                        if (holder is MusicViewHolder) {
                            if (isValidFile && canRead) {
                                // File hợp lệ - cập nhật UI để sẵn sàng play


                                Log.d("MusicAdapter", "File scan completed - Ready to play")
                            } else {
                                // File không hợp lệ - hiển thị lỗi và thử download lại

                                Log.e(
                                    "MusicAdapter", "File scan failed - Invalid file or cannot read"
                                )

                                // Tự động thử download lại nếu file không hợp lệ
                                delay(1000) // Giảm delay xuống 1 giây
                                downloadSong(song, position)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicAdapter", "Error updating UI after scan", e)
                    }
                }

            } catch (e: Exception) {
                Log.e("MusicAdapter", "Error scanning file", e)
                withContext(Dispatchers.Main) {
                    try {
                        val holder = recyclerView?.findViewHolderForAdapterPosition(position)
                        if (holder is MusicViewHolder) {

                        }
                    } catch (e: Exception) {
                        Log.e("MusicAdapter", "Error updating error state", e)
                    }
                }
            }
        }
    }

    /**
     * Force refresh file status cho một bài hát cụ thể
     */
    fun refreshFileStatus(song: Song, position: Int) {
        // Kiểm tra ngay lập tức trước
        checkFileStatusImmediately(song, position)
        // Sau đó scan chi tiết
        scanAndUpdateFileStatus(song, position)
    }

    /**
     * Force refresh file status ngay lập tức (không delay)
     */
    fun refreshFileStatusImmediately(song: Song, position: Int) {
        checkFileStatusImmediately(song, position)
    }

    /**
     * Force scan file với MediaScanner
     */
    fun forceMediaScan(song: Song, position: Int) {
        scanFileWithMediaScanner(song, position)
    }

    /**
     * Scan tất cả file đã download với MediaScanner
     */
    fun scanAllDownloadedFiles() {
        musicList.forEachIndexed { index, song ->
            if (isSongDownloaded(song)) {
                scanFileWithMediaScanner(song, index)
            }
        }
    }

    /**
     * Force update thời gian ngay lập tức
     */
    fun forceUpdateTimeNow(position: Int) {
        try {
            if (position == currentPlayingPosition && currentPlayer != null) {
                val currentPosition = currentPlayer?.currentPosition ?: 0
                val duration = currentPlayer?.duration ?: 0
                if (duration > 0) {
                    updateTimeDisplay(position, currentPosition, duration)
                    Log.d("MusicAdapter", "Force updated time: ${formatTime(currentPosition)} / ${formatTime(duration)}")
                }
            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error force updating time", e)
        }
    }

    /**
     * Test file trước khi phát để debug
     */
    fun testFileBeforePlay(song: Song, position: Int) {
        val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
        val downloadDir = File(context.filesDir, "music")
        val songFile = File(downloadDir, fileName)

        Log.d("MusicAdapter", "=== TEST FILE BEFORE PLAY ===")
        Log.d("MusicAdapter", "Song: ${song.title}")
        Log.d("MusicAdapter", "File path: ${songFile.absolutePath}")
        Log.d("MusicAdapter", "File exists: ${songFile.exists()}")
        Log.d("MusicAdapter", "File size: ${songFile.length()} bytes")
        Log.d("MusicAdapter", "File can read: ${songFile.canRead()}")
        Log.d("MusicAdapter", "File can write: ${songFile.canWrite()}")
        Log.d("MusicAdapter", "File can execute: ${songFile.canExecute()}")
        Log.d("MusicAdapter", "File is file: ${songFile.isFile}")
        Log.d("MusicAdapter", "File is directory: ${songFile.isDirectory}")
        Log.d("MusicAdapter", "File is hidden: ${songFile.isHidden}")
        Log.d("MusicAdapter", "File last modified: ${songFile.lastModified()}")
        Log.d("MusicAdapter", "File absolute path: ${songFile.absolutePath}")
        Log.d(
            "MusicAdapter", "File canonical path: ${
                try {
                    songFile.canonicalPath
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }"
        )

        // Test file content
        try {
            val inputStream = songFile.inputStream()
            val firstBytes = ByteArray(10)
            val bytesRead = inputStream.read(firstBytes)
            inputStream.close()

            Log.d(
                "MusicAdapter", "First 10 bytes: ${
                firstBytes.take(bytesRead).joinToString(", ") { "0x%02X".format(it) }
            }")

            // Check if it's a valid MP3 file (should start with ID3 or 0xFF)
            val isValidMp3 =
                bytesRead > 0 && (firstBytes[0] == 0x49.toByte() && firstBytes[1] == 0x44.toByte() && firstBytes[2] == 0x33.toByte()) || (firstBytes[0] == 0xFF.toByte() && (firstBytes[1] and 0xE0.toByte()) == 0xE0.toByte())

            Log.d("MusicAdapter", "Valid MP3 header: $isValidMp3")

        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error reading file content: ${e.message}")
        }

        // Test directory permissions
        Log.d("MusicAdapter", "Directory exists: ${downloadDir.exists()}")
        Log.d("MusicAdapter", "Directory can read: ${downloadDir.canRead()}")
        Log.d("MusicAdapter", "Directory can write: ${downloadDir.canWrite()}")
        Log.d("MusicAdapter", "Directory can execute: ${downloadDir.canExecute()}")
    }

    /**
     * Refresh tất cả file status
     */
    fun refreshAllFileStatus() {
        musicList.forEachIndexed { index, song ->
            scanAndUpdateFileStatus(song, index)
        }
    }

    /**
     * Kiểm tra và cập nhật file status cho tất cả bài hát
     */
    fun checkAndUpdateAllFiles() {
        musicList.forEachIndexed { index, song ->
            val isDownloaded = isSongDownloaded(song)
            if (isDownloaded) {
                // Nếu đã download, scan lại để đảm bảo file hợp lệ
                scanAndUpdateFileStatus(song, index)
            } else {
                // Nếu chưa download, hiển thị trạng thái "Not Downloaded"
                updateNotDownloadedState(index)
            }
        }
    }

    /**
     * Cập nhật trạng thái cho bài hát chưa download
     */
    private fun updateNotDownloadedState(position: Int) {
        try {
            val holder = recyclerView?.findViewHolderForAdapterPosition(position)
            if (holder is MusicViewHolder) {

                holder.binding.btnPlay.setImageResource(R.drawable.ic_play_item)
            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating not downloaded state", e)
        }
    }

    /**
     * Kiểm tra file status và cập nhật UI ngay lập tức
     */
    fun checkFileStatusImmediately(song: Song, position: Int) {
        val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
        val downloadDir = File(context.filesDir, "music")
        val songFile = File(downloadDir, fileName)

        val fileExists = songFile.exists()
        val fileSize = songFile.length()
        val isValidFile = fileExists && fileSize > 1024
        val canRead = songFile.canRead()

        if (isValidFile && canRead) {
            // File hợp lệ - cập nhật UI ngay lập tức
            try {
                val holder = recyclerView?.findViewHolderForAdapterPosition(position)
                if (holder is MusicViewHolder) {

                    holder.binding.btnPlay.setImageResource(R.drawable.ic_play_item)

                    Log.d("MusicAdapter", "File ready immediately for ${song.title}")
                }
            } catch (e: Exception) {
                Log.e("MusicAdapter", "Error updating immediate file status", e)
            }
        } else {
            // File không hợp lệ - hiển thị lỗi
            try {
                val holder = recyclerView?.findViewHolderForAdapterPosition(position)
                if (holder is MusicViewHolder) {


                    Log.d("MusicAdapter", "File not ready yet for ${song.title}")
                }
            } catch (e: Exception) {
                Log.e("MusicAdapter", "Error updating immediate error state", e)
            }
        }
    }

    /**
     * Kiểm tra file status với retry nhanh
     */
    fun checkFileStatusWithRetry(song: Song, position: Int, maxRetries: Int = 5) {
        var retryCount = 0

        fun checkFile() {
            val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
            val downloadDir = File(context.filesDir, "music")
            val songFile = File(downloadDir, fileName)

            val fileExists = songFile.exists()
            val fileSize = songFile.length()
            val isValidFile = fileExists && fileSize > 1024
            val canRead = songFile.canRead()

            if (isValidFile && canRead) {
                // File sẵn sàng - cập nhật UI
                checkFileStatusImmediately(song, position)
                Log.d(
                    "MusicAdapter", "File ready after ${retryCount + 1} retries for ${song.title}"
                )
            } else if (retryCount < maxRetries) {
                // Thử lại sau 100ms
                retryCount++
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    checkFile()
                }
            } else {
                // Hết số lần thử - hiển thị lỗi
                Log.e("MusicAdapter", "File not ready after $maxRetries retries for ${song.title}")
                try {
                    val holder = recyclerView?.findViewHolderForAdapterPosition(position)
                    if (holder is MusicViewHolder) {

                    }
                } catch (e: Exception) {
                    Log.e("MusicAdapter", "Error updating retry failed state", e)
                }
            }
        }

        checkFile()
    }

    /**
     * Scan file với MediaScanner để hệ thống nhận diện
     */
    private fun scanFileWithMediaScanner(song: Song, position: Int) {
        val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
        val downloadDir = File(context.filesDir, "music")
        val songFile = File(downloadDir, fileName)

        if (songFile.exists() && songFile.length() > 1024) {
            try {
                // Sử dụng MediaScanner để scan file
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(songFile.absolutePath),
                    arrayOf("audio/mpeg"), // MIME type cho MP3
                    object : MediaScannerConnection.OnScanCompletedListener {
                        override fun onScanCompleted(path: String?, uri: android.net.Uri?) {
                            Log.d("MusicAdapter", "MediaScanner completed for: $path")
                            if (uri != null) {
                                Log.d("MusicAdapter", "Media URI: $uri")
                                // File đã được scan thành công
                                CoroutineScope(Dispatchers.Main).launch {
                                    updateMediaScanSuccess(song, position)
                                }
                            } else {
                                Log.e("MusicAdapter", "MediaScanner failed for: $path")
                                CoroutineScope(Dispatchers.Main).launch {
                                    updateMediaScanFailed(song, position)
                                }
                            }
                        }
                    })

                Log.d("MusicAdapter", "MediaScanner started for: ${songFile.absolutePath}")

            } catch (e: Exception) {
                Log.e("MusicAdapter", "Error starting MediaScanner", e)
                // Fallback to normal file check
                checkFileStatusImmediately(song, position)
            }
        } else {
            Log.d("MusicAdapter", "File not ready for MediaScanner yet")
            // File chưa sẵn sàng, kiểm tra lại sau
            checkFileStatusImmediately(song, position)
        }
    }

    /**
     * Cập nhật UI khi MediaScanner thành công
     */
    private fun updateMediaScanSuccess(song: Song, position: Int) {
        try {
            val holder = recyclerView?.findViewHolderForAdapterPosition(position)
            if (holder is MusicViewHolder) {

                holder.binding.btnPlay.setImageResource(R.drawable.ic_play_item)

                Log.d("MusicAdapter", "MediaScanner success - File ready for ${song.title}")
            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating MediaScanner success", e)
        }
    }

    /**
     * Cập nhật UI khi MediaScanner thất bại
     */
    private fun updateMediaScanFailed(song: Song, position: Int) {
        try {
            val holder = recyclerView?.findViewHolderForAdapterPosition(position)
            if (holder is MusicViewHolder) {


                Log.e("MusicAdapter", "MediaScanner failed for ${song.title}")
            }
        } catch (e: Exception) {
            Log.e("MusicAdapter", "Error updating MediaScanner failed", e)
        }
    }
}
