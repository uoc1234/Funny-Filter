package com.titanbbl.funny.face.filter.game.ui.component.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.databinding.ItemCollectionBinding
import android.widget.Toast
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.ui.bases.ext.loadBitmapWithPlaceholder
import com.titanbbl.funny.face.filter.game.ui.bases.ext.setPlaceholder
import com.titanbbl.funny.face.filter.game.ui.bases.ext.setErrorImage
import com.titanbbl.funny.face.filter.game.model.VideoItem
import kotlinx.coroutines.*
import android.content.Context

class VideoCollectionAdapter(
    private val onItemClick: (VideoItem) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {}
) : ListAdapter<VideoItem, VideoCollectionAdapter.VideoViewHolder>(VideoDiffCallback()) {

    // Coroutine scope cho adapter
    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Map để track các job đang chạy cho mỗi item
    private val loadingJobs = mutableMapOf<Long, Job>()
    
    // Selection mode state
    private var isSelectionMode = false

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        // Reset all selections when exiting selection mode
        if (!enabled) {
            currentList.forEach { it.isSelected = false }
        }
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<VideoItem> {
        return currentList.filter { it.isSelected }
    }

    fun getSelectedCount(): Int {
        return currentList.count { it.isSelected }
    }

    fun selectAll() {
        currentList.forEach { it.isSelected = true }
        notifyDataSetChanged()
        onSelectionChanged(getSelectedCount())
    }

    fun deselectAll() {
        currentList.forEach { it.isSelected = false }
        notifyDataSetChanged()
        onSelectionChanged(getSelectedCount())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemCollectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(
        private val binding: ItemCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var hasImageError: Boolean = false

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val videoItem = getItem(position)
                    
                    if (isSelectionMode) {
                        // In selection mode, toggle selection
                        videoItem.isSelected = !videoItem.isSelected
                        // Update the checkmark visibility instead of isChecked
                        binding.checkmark.visibility = if (videoItem.isSelected) android.view.View.VISIBLE else android.view.View.GONE
                        onSelectionChanged(getSelectedCount())
                    } else if (hasImageError) {
                        // In normal mode, show error toast
                        Toast.makeText(
                            binding.root.context,
                            binding.root.context.getString(R.string.video_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // In normal mode, play video
                        onItemClick(videoItem)
                    }
                }
            }
        }

        fun bind(videoItem: VideoItem) {
            // Cancel previous loading job for this item
            loadingJobs[videoItem.id]?.cancel()
            
            // Set placeholder initially
            binding.imvCollection.setPlaceholder()
            hasImageError = false
            
            // Handle selection mode UI
            binding.checkDelete.visibility = if (isSelectionMode) android.view.View.VISIBLE else android.view.View.GONE
            binding.checkmark.visibility = if (videoItem.isSelected) android.view.View.VISIBLE else android.view.View.GONE
            
            // Show/hide selection overlay
            binding.selectionOverlay.visibility = if (isSelectionMode && videoItem.isSelected) android.view.View.VISIBLE else android.view.View.GONE
            
            // Load bitmap asynchronously
            val job = adapterScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        videoItem.getLastFrameBitmap(binding.root.context)
                    }
                    
                    // Check if this view holder is still bound to the same item
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION && 
                        getItem(bindingAdapterPosition).id == videoItem.id) {
                        
                        if (bitmap != null) {
                            binding.imvCollection.loadBitmapWithPlaceholder(
                                binding.root.context,
                                bitmap
                            )
                        } else {
                            hasImageError = true
                            binding.imvCollection.setErrorImage()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION && 
                        getItem(bindingAdapterPosition).id == videoItem.id) {
                        hasImageError = true
                        binding.imvCollection.setErrorImage()
                    }
                }
            }
            
            loadingJobs[videoItem.id] = job
        }
    }

    private class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem == newItem
        }
    }
    
    /**
     * Cleanup resources when adapter is no longer needed
     */
    fun cleanup() {
        adapterScope.cancel()
        loadingJobs.values.forEach { it.cancel() }
        loadingJobs.clear()
    }
}

