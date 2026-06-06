package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.databinding.ItemPhysicalFeatureBinding
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureItem

class PhysicalFeatureAdapter : ListAdapter<PhysicalFeatureItem, PhysicalFeatureAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((PhysicalFeatureItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhysicalFeatureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPhysicalFeatureBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: PhysicalFeatureItem) {
            binding.apply {
                // Set main image

                
                // Set title label
                tvTitle.text = item.title



                ivOverlay.setImageResource(item.imageRes)


                
                // Click listener
                root.setOnClickListener {
                    onItemClick?.invoke(item)
                }
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<PhysicalFeatureItem>() {
        override fun areItemsTheSame(oldItem: PhysicalFeatureItem, newItem: PhysicalFeatureItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhysicalFeatureItem, newItem: PhysicalFeatureItem): Boolean {
            return oldItem == newItem
        }
    }
} 