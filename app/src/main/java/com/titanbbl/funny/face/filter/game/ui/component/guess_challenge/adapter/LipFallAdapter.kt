package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.databinding.ItemLipFallChallengeBinding
import com.titanbbl.funny.face.filter.game.databinding.ItemLipFallAdsBinding
import com.titanbbl.funny.face.filter.game.model.LipFallItem

class LipFallAdapter : ListAdapter<LipFallItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION
    var onItemClick: ((LipFallItem) -> Unit)? = null

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_ADS = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            else -> VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ADS -> {
                val binding = ItemLipFallAdsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AdsViewHolder(binding)
            }
            else -> {
                val binding = ItemLipFallChallengeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                LipFallViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LipFallViewHolder -> holder.bind(getItem(position), position == selectedPosition)
            is AdsViewHolder -> holder.bind(getItem(position))
        }
    }

    inner class LipFallViewHolder(
        private val binding: ItemLipFallChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: LipFallItem, isSelected: Boolean) {
            binding.apply {
                ivImage.setImageResource(item.iconRes)
                tvTitle.text = item.title
                
                // Update selection state - you can add visual feedback here if needed
                cardView.isSelected = isSelected

            }
        }
    }

    inner class AdsViewHolder(
        private val binding: ItemLipFallAdsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: LipFallItem) {
            // Ads items are not selectable
            binding.root.setOnClickListener {
                // Handle ads click if needed
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<LipFallItem>() {
        override fun areItemsTheSame(oldItem: LipFallItem, newItem: LipFallItem): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: LipFallItem, newItem: LipFallItem): Boolean {
            return oldItem == newItem
        }
    }
} 