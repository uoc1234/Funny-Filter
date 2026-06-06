package com.titanbbl.funny.face.filter.game.ui.component.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.databinding.ItemGameCardBinding
import com.titanbbl.funny.face.filter.game.model.GameItem

class GameAdapter(
    private val gameItems: List<GameItem>,
    private val onItemClick: (GameItem) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(gameItems[position])
    }

    override fun getItemCount(): Int = gameItems.size

    inner class GameViewHolder(
        private val binding: ItemGameCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(gameItems[position])
                }
            }
        }

        fun bind(item: GameItem) {
            binding.ivGameImage.setImageResource(item.imageResId)
            binding.tvGameTitle.text = item.title
        }
    }
}