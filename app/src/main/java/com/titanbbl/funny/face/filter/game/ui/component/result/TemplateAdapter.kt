package com.titanbbl.funny.face.filter.game.ui.component.result

import androidx.databinding.ViewDataBinding
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ItemTemplateBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseRecyclerView

data class TemplateItem(
    val imageRes: Int,
    val title: String
)

class TemplateAdapter : BaseRecyclerView<TemplateItem>() {
    override fun getItemLayout(): Int = R.layout.item_template

    override fun submitData(newData: List<TemplateItem>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
    }

    override fun setData(binding: ViewDataBinding, item: TemplateItem, layoutPosition: Int) {
        val itemBinding = binding as ItemTemplateBinding
        itemBinding.apply {
            templateImage.setImageResource(item.imageRes)
            templateTitle.text = item.title
        }
    }

    override fun onClickViews(binding: ViewDataBinding, obj: TemplateItem, layoutPosition: Int) {
        val itemBinding = binding as ItemTemplateBinding
        itemBinding.root.setOnClickListener {
            // Handle click event
        }
    }
} 