package com.titanbbl.funny.face.filter.game.ui.bases

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T>(mBinding: ViewDataBinding) :
    RecyclerView.ViewHolder(mBinding.root) {

    abstract fun bindData(obj: T)

    open fun onResizeViews() {}

    open fun onClickViews(obj: T) {}
}