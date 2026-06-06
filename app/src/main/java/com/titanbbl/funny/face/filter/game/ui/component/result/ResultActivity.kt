package com.titanbbl.funny.face.filter.game.ui.component.result

import android.content.ContentValues
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import androidx.core.content.FileProvider
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityResultBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.utils.Routes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class ResultActivity : BaseActivity<ActivityResultBinding>() {
    
    private var resultVideoUri: Uri? = null
    private var mediaController: MediaController? = null
    private lateinit var templateAdapter: TemplateAdapter
    
    override fun getLayoutActivity(): Int = R.layout.activity_result

    override fun initViews() {
        super.initViews()
        
        // Get video path from intent directly since videos are in app's internal storage
        intent.getStringExtra("video_path")?.let { path ->
            Log.d("ResultActivity", "Received video path: $path")
            setupVideoPlayer(path)
            // Store original path for saving
            originalVideoPath = path
        } ?: run {
            Log.e("ResultActivity", "No video path received in intent")
            Toast.makeText(this, "No video path provided", Toast.LENGTH_SHORT).show()
        }

        // Setup templates recycler view
        setupTemplatesRecyclerView()
    }
    
    private fun setupTemplatesRecyclerView() {
        templateAdapter = TemplateAdapter()
        mBinding.templatesContainer.adapter = templateAdapter

        // Submit template data
        val templates = listOf(
            TemplateItem(R.drawable.template_1, "Which animal are you?"),
            TemplateItem(R.drawable.template_2, "Which country is this?"),
            TemplateItem(R.drawable.template_3, "Which animal are you?")
        )
        templateAdapter.submitData(templates)
    }

    private fun setupVideoPlayer(videoPath: String) {
        try {
            Log.d("ResultActivity", "Setting up video player with internal path: $videoPath")
            
            val file = File(videoPath)
            if (!file.exists()) {
                Log.e("ResultActivity", "Video file does not exist: ${file.absolutePath}")
                Toast.makeText(this, "Video file not found in app storage", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d("ResultActivity", "Video file exists, size: ${file.length()} bytes")
            
            // Debug file information for internal storage
//            debugVideoFile(file)
            
            // For internal app files, we can use FileProvider or direct file access
            // Try FileProvider first (recommended for sharing)
            try {
                resultVideoUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                Log.d("ResultActivity", "FileProvider URI created: $resultVideoUri")
            } catch (e: Exception) {
                Log.w("ResultActivity", "FileProvider failed, trying direct file access", e)
                // Fallback to direct file URI for internal files
                resultVideoUri = Uri.fromFile(file)
                Log.d("ResultActivity", "Direct file URI created: $resultVideoUri")
            }
            
            // Setup video player
            resultVideoUri?.let { uri ->
                mBinding.resultVideo.apply {
                    // Clear any previous state
                    stopPlayback()
                    
                    // Set video URI
                    setVideoURI(uri)
                    
                    // Setup media controller
                    mediaController = MediaController(this@ResultActivity).apply {
                        setAnchorView(this@apply)
                    }
                    setMediaController(mediaController)
                    
                    // Start playing when prepared
                    setOnPreparedListener { mp ->
                        Log.d("ResultActivity", "Video prepared successfully, duration: ${mp.duration}ms")
                        mp.isLooping = true
                        start()
                    }
                    
                    // Handle errors
                    setOnErrorListener { _, what, extra ->
                        Log.e("ResultActivity", "Video playback error: what=$what, extra=$extra")
                        Toast.makeText(
                            this@ResultActivity,
                            "Video playback error: $what, $extra",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Try alternative loading method for internal files
                        tryAlternativeVideoLoading(videoPath)
                        
                        true
                    }
                    
                    // Handle completion
                    setOnCompletionListener { mp ->
                        Log.d("ResultActivity", "Video playback completed")
                        // Restart video since it's looping
                        mp.start()
                    }
                    
                    // Handle info
                    setOnInfoListener { _, what, extra ->
                        Log.d("ResultActivity", "Video info: what=$what, extra=$extra")
                        true
                    }
                }
                
                Log.d("ResultActivity", "Video player setup completed")
                
            } ?: run {
                Log.e("ResultActivity", "Failed to create video URI")
                Toast.makeText(this, "Failed to create video URI", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error setting up video player", e)
            e.printStackTrace()
            Toast.makeText(this, "Failed to load video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Debug method to help troubleshoot video loading issues for internal app files
     */
    private fun debugVideoFile(file: File) {
        try {
            Log.d("ResultActivity", "=== INTERNAL VIDEO FILE DEBUG INFO ===")
            Log.d("ResultActivity", "File path: ${file.absolutePath}")
            Log.d("ResultActivity", "File exists: ${file.exists()}")
            Log.d("ResultActivity", "File size: ${file.length()} bytes")
            Log.d("ResultActivity", "File readable: ${file.canRead()}")
            Log.d("ResultActivity", "File name: ${file.name}")
            Log.d("ResultActivity", "File extension: ${file.extension}")
            Log.d("ResultActivity", "Parent directory: ${file.parent}")
            Log.d("ResultActivity", "Parent exists: ${file.parentFile?.exists()}")
            Log.d("ResultActivity", "Parent readable: ${file.parentFile?.canRead()}")
            
            // Check if it's in app's internal storage
            val isInternal = file.absolutePath.contains(filesDir.absolutePath) || 
                           file.absolutePath.contains(cacheDir.absolutePath)
            Log.d("ResultActivity", "Is internal app file: $isInternal")
            Log.d("ResultActivity", "App files dir: ${filesDir.absolutePath}")
            Log.d("ResultActivity", "App cache dir: ${cacheDir.absolutePath}")
            Log.d("ResultActivity", "================================================")
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error in debugVideoFile", e)
        }
    }
    
    /**
     * Validate video file before opening preview dialog
     */
    private fun validateVideoFile(videoPath: String): Boolean {
        try {
            val file = File(videoPath)
            if (!file.exists()) {
                Log.e("ResultActivity", "Video file does not exist: ${file.absolutePath}")
                return false
            }
            
            if (!file.canRead()) {
                Log.e("ResultActivity", "Video file is not readable: ${file.absolutePath}")
                return false
            }
            
            if (file.length() == 0L) {
                Log.e("ResultActivity", "Video file is empty: ${file.absolutePath}")
                return false
            }
            
            // Check if it's a valid video file
            val fileName = file.name.lowercase()
            val validExtensions = listOf(".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm", ".3gp", ".m4v")
            val hasValidExtension = validExtensions.any { fileName.endsWith(it) }
            
            if (!hasValidExtension) {
                Log.w("ResultActivity", "File does not have valid video extension: $fileName")
                return false
            }
            
            Log.d("ResultActivity", "Video file validation passed: ${file.absolutePath}, size: ${file.length()} bytes, extension: ${file.extension}")
            return true
            
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error validating video file: $videoPath", e)
            return false
        }
    }

    /**
     * Try alternative video loading methods if primary method fails
     */
    private fun tryAlternativeVideoLoading(videoPath: String) {
        try {
            Log.d("ResultActivity", "Trying alternative video loading methods for internal file")
            
            val file = File(videoPath)
            if (!file.exists()) {
                Log.e("ResultActivity", "Video file still doesn't exist")
                return
            }
            
            // For internal app files, try different approaches
            Log.d("ResultActivity", "Trying direct file URI for internal file")
            
            mBinding.resultVideo.apply {
                stopPlayback()
                
                // Try with direct file URI (works for internal app files)
                val directUri = Uri.fromFile(file)
                setVideoURI(directUri)
                
                setOnPreparedListener { mp ->
                    Log.d("ResultActivity", "Alternative loading successful, duration: ${mp.duration}ms")
                    mp.isLooping = true
                    start()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("ResultActivity", "Alternative loading failed: what=$what, extra=$extra")
                    
                    // Last resort: try with absolute path
                    try {
                        Log.d("ResultActivity", "Trying with absolute file path")
                        setVideoPath(file.absolutePath)
                        
                        setOnPreparedListener { mp ->
                            Log.d("ResultActivity", "Absolute path loading successful")
                            mp.isLooping = true
                            start()
                        }
                        
                        setOnErrorListener { _, what2, extra2 ->
                            Log.e("ResultActivity", "All loading methods failed: what=$what2, extra=$extra2")
                            Toast.makeText(
                                this@ResultActivity,
                                "Unable to load video. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            true
                        }
                        
                    } catch (e: Exception) {
                        Log.e("ResultActivity", "Absolute path loading also failed", e)
                        Toast.makeText(
                            this@ResultActivity,
                            "Video loading failed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error in alternative video loading", e)
        }
    }

    override fun onClickViews() {
        // Back button click
        mBinding.btnBack.setOnClickListener {
            finish()
        }


        mBinding.btnHome.setOnClickListener {
            Routes.startHomeActivity(this)
        }
        
        // Share button overlay click
        mBinding.shareButtonOverlay.setOnClickListener {
            // Show video preview dialog with proper error handling
            intent.getStringExtra("video_path")?.let { path ->
                if (validateVideoFile(path)) {
                    try {
                        Log.d("ResultActivity", "Opening video preview dialog for path: $path")
                        
                        // Pause the main video before showing dialog
                        pauseMainVideo()
                        
                        val dialog = VideoPreviewDialog(this, path) { 
                            // Callback when dialog is dismissed - resume main video
                            resumeMainVideo()
                        }
                        dialog.show()
                    } catch (e: Exception) {
                        Log.e("ResultActivity", "Error opening video preview dialog", e)
                        Toast.makeText(this, "Error opening video preview", Toast.LENGTH_SHORT).show()
                        // Resume main video if dialog creation failed
                        resumeMainVideo()
                    }
                } else {
                    Log.e("ResultActivity", "Video file validation failed for path: $path")
                    Toast.makeText(this, "Video file is not accessible", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.e("ResultActivity", "No video path available for sharing")
                Toast.makeText(this, "No video available for preview", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Save option click
        mBinding.saveOption.setOnClickListener {
            saveVideoToGallery()
        }
        
        // TikTok option click
        mBinding.tiktokOption.setOnClickListener {
            shareToApp("com.zhiliaoapp.musically")
        }
        
        // Instagram option click
        mBinding.instagramOption.setOnClickListener {
            shareToApp("com.instagram.android")
        }
        
        // Facebook option click
        mBinding.facebookOption.setOnClickListener {
            shareToApp("com.facebook.katana")
        }
        
        // Other option click
        mBinding.otherOption.setOnClickListener {
            shareVideo()
        }
    }
    
    private fun shareVideo() {
        try {
            resultVideoUri?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share video via"))
            } ?: Toast.makeText(this, "No video to share", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share video", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun shareToApp(packageName: String) {
        try {
            resultVideoUri?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage(packageName)
                }
                
                if (shareIntent.resolveActivity(packageManager) != null) {
                    startActivity(shareIntent)
                } else {
                    Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "No video to share", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share to app", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private var originalVideoPath: String? = null
    private var wasVideoPlayingBeforeDialog = false

    /**
     * Pause the main video when dialog opens
     */
    private fun pauseMainVideo() {
        try {
            wasVideoPlayingBeforeDialog = mBinding.resultVideo.isPlaying
            if (wasVideoPlayingBeforeDialog) {
                mBinding.resultVideo.pause()
                Log.d("ResultActivity", "Main video paused for dialog")
            }
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error pausing main video", e)
            wasVideoPlayingBeforeDialog = false
        }
    }
    
    /**
     * Resume the main video when dialog closes
     */
    private fun resumeMainVideo() {
        try {
            if (wasVideoPlayingBeforeDialog && resultVideoUri != null) {
                mBinding.resultVideo.start()
                Log.d("ResultActivity", "Main video resumed after dialog")
            }
            wasVideoPlayingBeforeDialog = false
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error resuming main video", e)
            wasVideoPlayingBeforeDialog = false
        }
    }

    private fun saveVideoToGallery() {
        try {
            val srcPath = originalVideoPath
            if (srcPath.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.no_video_to_save), Toast.LENGTH_SHORT).show()
                return
            }
            val srcFile = File(srcPath)
            if (!srcFile.exists()) {
                Toast.makeText(this, getString(R.string.source_video_not_found), Toast.LENGTH_SHORT).show()
                return
            }

            val displayName = srcFile.name
            val mimeType = "video/mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = contentResolver.insert(collection, values)
                if (itemUri == null) {
                    Toast.makeText(this, "Failed to create media item", Toast.LENGTH_SHORT).show()
                    return
                }

                contentResolver.openOutputStream(itemUri).use { outStream ->
                    if (outStream == null) throw IllegalStateException("Output stream is null")
                    FileInputStream(srcFile).use { input ->
                        input.copyTo(outStream)
                    }
                }

                // Mark as not pending so it becomes visible
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, values, null, null)

                Toast.makeText(this, getString(R.string.saved_to_gallery_success), Toast.LENGTH_SHORT).show()
            } else {
                // Legacy: write to Movies directory
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                if (moviesDir?.exists() == false) moviesDir.mkdirs()
                val dstFile = File(moviesDir, displayName)
                FileInputStream(srcFile).use { input ->
                    FileOutputStream(dstFile).use { output ->
                        input.copyTo(output)
                    }
                }
                // Trigger media scan
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dstFile)))
                Toast.makeText(this, getString(R.string.saved_to_gallery_success), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error saving video", e)
            Toast.makeText(this, getString(R.string.saved_to_gallery_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (mBinding.resultVideo.isPlaying) {
                mBinding.resultVideo.pause()
                Log.d("ResultActivity", "Video paused due to activity pause")
            }
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error pausing video", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Resume video if it was playing before
            if (resultVideoUri != null && !mBinding.resultVideo.isPlaying) {
                Log.d("ResultActivity", "Resuming video playback")
                mBinding.resultVideo.start()
            }
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error resuming video", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaController = null
            if (mBinding.resultVideo.isPlaying) {
                mBinding.resultVideo.stopPlayback()
                Log.d("ResultActivity", "Video playback stopped")
            }
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error stopping video", e)
        }
    }
}