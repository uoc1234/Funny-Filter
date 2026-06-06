package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.databinding.ItemChallengeBinding
import com.titanbbl.funny.face.filter.game.model.ChallengeClickListener
import com.titanbbl.funny.face.filter.game.model.ChallengeItem
import com.bumptech.glide.Glide

class ChallengeAdapter :
    ListAdapter<ChallengeItem, ChallengeAdapter.ChallengeViewHolder>(ChallengeDiffCallback()) {

    private var selectedPosition = -1
    private var listener: ChallengeClickListener? = null

    fun setOnChallengeClickListener(listener: ChallengeClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val binding = ItemChallengeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position)
    }

    inner class ChallengeViewHolder(
        private val binding: ItemChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChallengeItem, position: Int) {
            // Set item data
            binding.item = item

            // Set selection state
            item.isSelected = position == selectedPosition

            // Load image if URL is not empty
            Glide.with(binding.ivChallenge)
                .load(item.imageUrl)
                .centerCrop()
                .into(binding.ivChallenge)


            // Set stroke properties
            binding.tvNameDiyDraw.apply {
            }

            // Set click listener
            binding.ivChallenge.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                // Update previous selected item
                if (previousPosition != -1) {
                    getItem(previousPosition).isSelected = false
                    notifyItemChanged(previousPosition)
                }

                // Update newly selected item
                item.isSelected = true
                notifyItemChanged(selectedPosition)

                listener?.onChallengeClick(item)
            }

            binding.executePendingBindings()
        }
    }
}

private class ChallengeDiffCallback : DiffUtil.ItemCallback<ChallengeItem>() {
    override fun areItemsTheSame(oldItem: ChallengeItem, newItem: ChallengeItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChallengeItem, newItem: ChallengeItem): Boolean {
        return oldItem == newItem
    }
} 