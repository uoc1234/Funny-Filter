package com.titanbbl.funny.face.filter.game.ui.component.dialog

import android.content.Context
import android.view.View
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.DialogConnectionBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseDialog

/**
 * No Internet Connection Dialog
 * 
 * This dialog displays when there's no internet connection available.
 * It shows a friendly cloud illustration with surrounding icons and a retry button.
 * 
 * Features:
 * - Central cloud illustration with distressed expression
 * - Surrounding icons representing unavailable services
 * - "NO INTERNET" text message
 * - Orange gradient retry button
 * - Proper dialog styling with rounded corners
 */
class DialogConnect(
    private val mContext: Context,
    private val onRetryClick: (() -> Unit)? = null
) : BaseDialog<DialogConnectionBinding>(mContext) {

    override fun getLayoutDialog(): Int {
        return R.layout.dialog_connection
    }

    override fun initViews() {
        // Set dialog properties
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        
        // Center the dialog on screen
        window?.setLayout(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onClickViews() {
        // Set up retry button click listener
        mBinding.btnRetry.setOnClickListener {
            onRetryClick?.invoke()
            dismiss()
        }
    }

    /**
     * Show the dialog with a fade-in animation
     */
    fun showWithAnimation() {
        show()
        mBinding.root.alpha = 0f
        mBinding.root.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /**
     * Dismiss the dialog with a fade-out animation
     */
    fun dismissWithAnimation() {
        mBinding.root.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                dismiss()
            }
            .start()
    }

    companion object {
        /**
         * Show the No Internet dialog
         * 
         * @param context The context to show the dialog in
         * @param onRetryClick Optional callback when retry button is clicked
         */
        fun show(
            context: Context,
            onRetryClick: (() -> Unit)? = null
        ): DialogConnect {
            return DialogConnect(context, onRetryClick).apply {
                show()
            }
        }
    }
}