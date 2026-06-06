package com.titanbbl.funny.face.filter.game.ui.component.dialog

import android.content.Context
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.DialogConfirmDeleteBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseDialog

class DialogConfirmDelete(
    context: Context,
    private val titleText: String,
    private val messageText: String,
    private val onConfirm: () -> Unit,
    private val onCancel: (() -> Unit)? = null
) : BaseDialog<DialogConfirmDeleteBinding>(context,R.style.ThemeDialogWrapcontent) {

    override fun getLayoutDialog(): Int = R.layout.dialog_confirm_delete

    override fun initViews() {
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        mBinding.tvMessage.text = messageText
    }

    override fun onClickViews() {
        mBinding.btnCancel.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }
        mBinding.btnDelete.setOnClickListener {
            dismiss()
            onConfirm.invoke()
        }
    }

    companion object {
        fun show(
            context: Context,
            title: String,
            message: String,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null
        ) {
            DialogConfirmDelete(context, title, message, onConfirm, onCancel).show()
        }
    }
}


