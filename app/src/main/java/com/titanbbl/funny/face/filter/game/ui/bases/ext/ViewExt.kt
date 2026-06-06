package com.titanbbl.funny.face.filter.game.ui.bases.ext

import android.os.SystemClock
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.databinding.ViewDataBinding

internal const val DISPLAY = 1080

fun View.goneView() {
    visibility = View.GONE
}

fun View.visibleView() {
    visibility = View.VISIBLE
}

fun View.invisibleView() {
    visibility = View.INVISIBLE
}

fun View.isVisible() = visibility == View.VISIBLE

fun View.isInvisible() = visibility == View.INVISIBLE

fun View.isGone() = visibility == View.GONE

fun ViewDataBinding.goneView() {
    this.root.goneView()
}

fun ViewDataBinding.visibleView() {
    this.root.visibleView()
}

fun ViewDataBinding.invisibleView() {
    this.root.invisibleView()
}

fun ViewDataBinding.isVisible() = this.root.visibility == View.VISIBLE

fun ViewDataBinding.isInvisible() = this.root.visibility == View.INVISIBLE

fun ViewDataBinding.isGone() = this.root.visibility == View.GONE

/*Resize View*/
fun View.resizeView(width: Int, height: Int = 0) {
    val pW = context.getWidthScreenPx() * width / DISPLAY
    val pH = if (height == 0) pW else pW * height / width
    val params = layoutParams
    params.let {
        it.width = pW
        it.height = pH
    }
}

var lastClickTime = 0L

fun View.click(action: (view: View?) -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < 250L) return
            else action(v)
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

/* Animations */
fun View.slideInUp(duration: Long = 220L) {
    if (visibility == View.VISIBLE && translationY == 0f) return
    post {
        val distance = if (height > 0) height.toFloat() else measuredHeight.toFloat().takeIf { it > 0 } ?: 200f
        alpha = 0f
        translationY = distance
        visibleView()
        animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}

fun View.slideOutDown(duration: Long = 220L, makeGone: Boolean = true) {
    if (visibility != View.VISIBLE) {
        if (makeGone) goneView() else invisibleView()
        return
    }
    post {
        val distance = if (height > 0) height.toFloat() else measuredHeight.toFloat().takeIf { it > 0 } ?: 200f
        animate()
            .translationY(distance)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                translationY = 0f
                alpha = 1f
                if (makeGone) goneView() else invisibleView()
            }
            .start()
    }
}