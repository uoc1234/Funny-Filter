package com.titanbbl.funny.face.filter.game.ui.bases.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.titanbbl.funny.face.filter.game.R
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


fun ImageView.loadImage(context: Context, path: Bitmap?, radius: Int = 1, onError: (() -> Unit)? = null) {
    val options: RequestOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
        .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(radius)))

    Glide.with(context)
        .load(path)
        .apply(options)
        .error(R.drawable.ic_error_image)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                onError?.invoke()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }
        })
        .into(this)
}


fun ImageView.loadImage(context: Context, path: Any?, radius: Int = 1, onError: (() -> Unit)? = null) {
    val options: RequestOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .fitCenter()
        .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(radius)))

    Glide.with(context)
        .load(path)
        .apply(options)
        .error(R.drawable.ic_error_image)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                onError?.invoke()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }
        })
        .into(this)
}

fun ImageView.loadImageNoCache(context: Context, path: Any, radius: Int = 1) {
    val options: RequestOptions = RequestOptions()
        .apply(
            RequestOptions().transform(
                CenterCrop(),
                RoundedCorners(radius)
            )
        )

    Glide.with(context)
        .load(path)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .apply(options)
        .into(this)
}

