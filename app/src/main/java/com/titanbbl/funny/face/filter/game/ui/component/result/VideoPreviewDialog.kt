package com.titanbbl.funny.face.filter.game.ui.component.result

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import androidx.core.content.FileProvider
import com.titanbbl.funny.face.filter.game.databinding.DialogVideoPreviewBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseDialog
import java.io.File

class VideoPreviewDialog(
    context: Context,
    private val videoPath: String,
    private val onDismissCallback: (() -> Unit)? = null
) : BaseDialog<DialogVideoPreviewBinding>(context) {

    private var mediaController: MediaController? = null

    override fun getLayoutDialog(): Int = com.titanbbl.funny.face.filter.game.R.layout.dialog_video_preview

    override fun initViews() {
        super.initViews()
        
        try {
            Log.d("VideoPreviewDialog", "Initializing video preview with path: $videoPath")
            
            val file = File(videoPath)
            if (!file.exists()) {
                Log.e("VideoPreviewDialog", "Video file does not exist: ${file.absolutePath}")
                Toast.makeText(context, "Video file not found", Toast.LENGTH_SHORT).show()
                dismiss()
                return
            }
            
            Log.d("VideoPreviewDialog", "Video file exists, size: ${file.length()} bytes")
            
            // Validate that it's actually a video file
            if (!isValidVideoFile(file)) {
                Log.e("VideoPreviewDialog", "File is not a valid video: ${file.absolutePath}")
                Toast.makeText(context, "File is not a valid video", Toast.LENGTH_SHORT).show()
                dismiss()
                return
            }
            
            // Try multiple methods to load the video for internal app files
            val uri = createVideoUri(file)
            if (uri != null) {
                setupVideoPlayer(uri)
            } else {
                Log.e("VideoPreviewDialog", "Failed to create video URI")
                Toast.makeText(context, "Failed to load video", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            
        } catch (e: Exception) {
            Log.e("VideoPreviewDialog", "Error initializing video preview", e)
            e.printStackTrace()
            Toast.makeText(context, "Error loading video: ${e.message}", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    /**
     * Check if the file is a valid video file
     */
    private fun isValidVideoFile(file: File): Boolean {
        try {
            val fileName = file.name.lowercase()
            val validExtensions = listOf(".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm", ".3gp", ".m4v")
            
            val hasValidExtension = validExtensions.any { fileName.endsWith(it) }
            if (!hasValidExtension) {
                Log.w("VideoPreviewDialog", "File does not have valid video extension: $fileName")
                return false
            }
            
            // Check if file size is reasonable (at least 1KB)
            if (file.length() < 1024) {
                Log.w("VideoPreviewDialog", "File size too small to be a valid video: ${file.length()} bytes")
                return false
            }
            
            Log.d("VideoPreviewDialog", "File validation passed: $fileName, size: ${file.length()} bytes")
            return true
            
        } catch (e: Exception) {
            Log.e("VideoPreviewDialog", "Error validating video file", e)
            return false
        }
    }

    /**
     * Create video URI using multiple methods for internal app files
     */
    private fun createVideoUri(file: File): Uri? {
        try {
            Log.d("VideoPreviewDialog", "=== CREATING VIDEO URI ===")
            Log.d("VideoPreviewDialog", "File path: ${file.absolutePath}")
            Log.d("VideoPreviewDialog", "File exists: ${file.exists()}")
            Log.d("VideoPreviewDialog", "File size: ${file.length()} bytes")
            Log.d("VideoPreviewDialog", "File readable: ${file.canRead()}")
            Log.d("VideoPreviewDialog", "Package name: ${context.packageName}")
            
            // Method 1: Try FileProvider first (recommended for sharing)
            try {
                val authority = "${context.packageName}.fileprovider"
                Log.d("VideoPreviewDialog", "Trying FileProvider with authority: $authority")
                
                val uri = FileProvider.getUriForFile(context, authority, file)
                Log.d("VideoPreviewDialog", "FileProvider URI created successfully: $uri")
                return uri
            } catch (e: Exception) {
                Log.w("VideoPreviewDialog", "FileProvider failed, trying direct file access", e)
            }
            
            // Method 2: Try direct file URI for internal app files
            try {
                Log.d("VideoPreviewDialog", "Trying direct file URI")
                val uri = Uri.fromFile(file)
                Log.d("VideoPreviewDialog", "Direct file URI created: $uri")
                return uri
            } catch (e: Exception) {
                Log.w("VideoPreviewDialog", "Direct file URI failed", e)
            }
            
            // Method 3: Try with file:// scheme
            try {
                Log.d("VideoPreviewDialog", "Trying file:// scheme")
                val uri = Uri.parse("file://${file.absolutePath}")
                Log.d("VideoPreviewDialog", "File scheme URI created: $uri")
                return uri
            } catch (e: Exception) {
                Log.w("VideoPreviewDialog", "File scheme URI failed", e)
            }
            
            Log.e("VideoPreviewDialog", "All URI creation methods failed")
            return null
            
        } catch (e: Exception) {
            Log.e("VideoPreviewDialog", "Error creating video URI", e)
            return null
        }
    }

    private fun setupVideoPlayer(uri: Uri) {
        mBinding.previewVideo.apply {
            // Clear any previous state
            stopPlayback()
            
            // Set video URI
            setVideoURI(uri)
            
            // Setup media controller
            mediaController = MediaController(context).apply {
                setAnchorView(this@apply)
            }
            setMediaController(mediaController)
            
            // Start playing when prepared
            setOnPreparedListener { mp ->
                Log.d("VideoPreviewDialog", "Video prepared successfully, duration: ${mp.duration}ms")
                mp.isLooping = true
                start()
            }
            
            // Handle errors with fallback methods
            setOnErrorListener { _, what, extra ->
                Log.e("VideoPreviewDialog", "Video playback error: what=$what, extra=$extra")
                
                // Try alternative loading method
                tryAlternativeVideoLoading()
                
                true
            }
            
            // Handle completion
            setOnCompletionListener { mp ->
                Log.d("VideoPreviewDialog", "Video playback completed")
                // Restart video since it's looping
                mp.start()
            }
            
            // Handle info
            setOnInfoListener { _, what, extra ->
                Log.d("VideoPreviewDialog", "Video info: what=$what, extra=$extra")
                true
            }
            
            // Add timeout for video loading
            postDelayed({
                if (!isPlaying) {
                    Log.w("VideoPreviewDialog", "Video loading timeout, trying alternative method")
                    tryAlternativeVideoLoading()
                }
            }, 5000) // 5 second timeout
        }
    }
    
    /**
     * Try alternative video loading methods if primary method fails
     */
    private fun tryAlternativeVideoLoading() {
        try {
            Log.d("VideoPreviewDialog", "Trying alternative video loading methods")
            
            val file = File(videoPath)
            if (!file.exists()) {
                Log.e("VideoPreviewDialog", "Video file still doesn't exist")
                return
            }
            
            mBinding.previewVideo.apply {
                stopPlayback()
                
                // Try with direct file path
                try {
                    Log.d("VideoPreviewDialog", "Trying with setVideoPath")
                    setVideoPath(file.absolutePath)
                    
                    setOnPreparedListener { mp ->
                        Log.d("VideoPreviewDialog", "Alternative loading successful with setVideoPath")
                        mp.isLooping = true
                        start()
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e("VideoPreviewDialog", "Alternative loading also failed: what=$what, extra=$extra")
                        
                        // Last resort: try copying to temp file
                        tryCopyToTempAndPlay()
                        
                        true
                    }
                    
                } catch (e: Exception) {
                    Log.e("VideoPreviewDialog", "Alternative loading failed", e)
                    
                    // Try copying to temp file
                    tryCopyToTempAndPlay()
                }
            }
            
        } catch (e: Exception) {
            Log.e("VideoPreviewDialog", "Error in alternative video loading", e)
        }
    }
    
    /**
     * Last resort: copy video to temp file and try to play
     */
    private fun tryCopyToTempAndPlay() {
        try {
            Log.d("VideoPreviewDialog", "Trying to copy video to temp file for better compatibility")
            
            val originalFile = File(videoPath)
            if (!originalFile.exists()) {
                Log.e("VideoPreviewDialog", "Original file doesn't exist for copying")
                return
            }
            
            // Create temp file in app's cache directory
            val tempFile = File.createTempFile("video_preview_", ".mp4", context.cacheDir)
            Log.d("VideoPreviewDialog", "Created temp file: ${tempFile.absolutePath}")
            
            // Copy the video file
            originalFile.copyTo(tempFile, overwrite = true)
            Log.d("VideoPreviewDialog", "Video copied to temp file, size: ${tempFile.length()} bytes")
            
            // Try to play from temp file
            mBinding.previewVideo.apply {
                stopPlayback()
                setVideoPath(tempFile.absolutePath)
                
                setOnPreparedListener { mp ->
                    Log.d("VideoPreviewDialog", "Temp file loading successful")
                    mp.isLooping = true
                    start()
                    
                    // Clean up temp file when video is prepared
                    tempFile.delete()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("VideoPreviewDialog", "Temp file loading also failed: what=$what, extra=$extra")
                    Toast.makeText(
                        context,
                        "Unable to load video. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Clean up temp file
                    tempFile.delete()
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e("VideoPreviewDialog", "Error copying to temp file", e)
            Toast.makeText(
                context,
                "Video loading failed. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun dismiss() {
        try {
            mBinding.previewVideo.stopPlayback()
            mediaController = null
            Log.d("VideoPreviewDialog", "Video preview dialog dismissed, resources cleaned up")
            
            // Call the callback to notify parent activity
            onDismissCallback?.invoke()
        } catch (e: Exception) {
            Log.e("VideoPreviewDialog", "Error cleaning up video resources", e)
        }
        super.dismiss()
    }

    override fun onClickViews() {
        super.onClickViews()
        
        mBinding.btnClose.setOnClickListener {
            dismiss()
        }
        
        // Prevent dialog background clicks from affecting video playback
        // Make sure clicks on the video area don't dismiss the dialog
        mBinding.previewVideo.setOnClickListener {
            // Do nothing - prevent click events from propagating
        }
        
        // Handle dialog background click - dismiss dialog
        mBinding.root.setOnClickListener {
            dismiss()
        }
    }
} 