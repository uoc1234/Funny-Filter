package com.titanbbl.funny.face.filter.game.ui.bases.ext

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R

/**
 * Extension function để load Bitmap với placeholder và error handling
 */
fun ImageView.loadBitmapWithPlaceholder(
    context: Context,
    bitmap: Bitmap?,
    placeholderResId: Int = R.drawable.ic_launcher_foreground,
    errorResId: Int = R.drawable.ic_launcher_foreground
) {
    if (bitmap != null) {
        setImageBitmap(bitmap)
    } else {
        setImageResource(placeholderResId)
    }
}

/**
 * Extension function để set placeholder image
 */
fun ImageView.setPlaceholder(resId: Int = R.drawable.ic_launcher_foreground) {
    setImageResource(resId)
}

/**
 * Extension function để set error image
 */
fun ImageView.setErrorImage(resId: Int = R.drawable.ic_launcher_foreground) {
    setImageResource(resId)
}
