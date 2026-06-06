package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ItemGuessChallengeBinding
import com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem
import com.titanbbl.funny.face.filter.game.utils.SystemUtil.loadImageWithHeader

class GuessItemAdapter : ListAdapter<PredictionResponseItem, GuessItemAdapter.GuessViewHolder>(DiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION
    var onItemClick: ((PredictionResponseItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuessViewHolder {
        val binding = ItemGuessChallengeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GuessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuessViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class GuessViewHolder(
        private val binding: ItemGuessChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: PredictionResponseItem, isSelected: Boolean) {
            binding.apply {
                ivIcon.loadImageWithHeader(this.root.context, item.image)
                tvTitle.text = item.question
                
                // Set stroke color based on selection using ColorStateList
                val strokeColor = ContextCompat.getColor(
                    root.context,
                    if (isSelected) R.color.selected_stroke_color else R.color.default_stroke_color
                )


                binding.ivIcon.setStrokeColorResource(if (isSelected) R.color.selected_stroke_color else R.color.default_stroke_color)
                
                root.setOnClickListener { 
                    val previousSelected = selectedPosition
                    selectedPosition = adapterPosition
                    
                    // Update previous and new selected items
                    notifyItemChanged(previousSelected)
                    notifyItemChanged(selectedPosition)
                    
                    onItemClick?.invoke(item)
                }
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<PredictionResponseItem>() {
        override fun areItemsTheSame(oldItem: PredictionResponseItem, newItem: PredictionResponseItem): Boolean {
            return oldItem.image == newItem.image
        }

        override fun areContentsTheSame(oldItem: PredictionResponseItem, newItem: PredictionResponseItem): Boolean {
            return oldItem == newItem
        }
    }
} 