package com.titanbbl.funny.face.filter.game.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import java.util.*

object SystemUtil {
    private var myLocale: Locale? = null

    // Load lại ngôn ngữ đã lưu và thay đổi chúng
    fun setLocale(context: Context) {
        val language = getPreLanguage(context)
        if (language == "") {
            val config = Configuration()
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } else {
            changeLang(language, context)
        }
    }

    // method phục vụ cho việc thay đổi ngôn ngữ.
    private fun changeLang(lang: String?, context: Context) {
        if (lang.equals("", ignoreCase = true)) return
        myLocale = Locale(lang)
        saveLocale(context, lang)
        Locale.setDefault(myLocale)
        val config = Configuration()
        config.locale = myLocale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private fun saveLocale(context: Context, lang: String?) {
        setPreLanguage(context, lang)
    }

    @SuppressLint("ObsoleteSdkInt")
    fun getPreLanguage(mContext: Context): String? {
        val preferences = mContext.getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
        Locale.getDefault().displayLanguage
        val lang: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0].language
        } else {
            Resources.getSystem().configuration.locale.language
        }
        return if (!languageApp.contains(lang)) {
            preferences.getString("KEY_LANGUAGE", "en")
        } else {
            preferences.getString("KEY_LANGUAGE", lang)
        }
    }

    fun setPreLanguage(context: Context, language: String?) {
        if (language == null || language == "") {
            return
        } else {
            val preferences = context.getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
            preferences.edit().putString("KEY_LANGUAGE", language).apply()
        }
    }

    val languageApp: List<String>
        get() {
            val languages: MutableList<String> = ArrayList()
            /*languages.add("ar"); // Arabic*/
            languages.add("cs") // Czech
            languages.add("de") // Germany
            languages.add("en") // English
            languages.add("es") // Spanish
            languages.add("fil") // Filipino
            languages.add("fr") // French
            languages.add("hi") // Hindi
            languages.add("hr") // Croatian
            languages.add("in") // indonesian
            languages.add("it") // italian
            languages.add("ko") // korean
            languages.add("ja") //japanese
            languages.add("ms") // Malay
            languages.add("nl") // Dutch
            languages.add("pl") // Polish
            languages.add("pt") // Portugal
            languages.add("ru") // Russian
            languages.add("sr") // Serbian
            languages.add("sv") // Swedish
            languages.add("tr") // Turkish
            languages.add("vi") // Vietnamese
            languages.add("zh") // Chinese
            return languages
        }


    fun ImageView.loadImageWithHeader(
        context: Context,
        url: String,
        requestOptions: RequestOptions = RequestOptions(),
        onResourceReady: (Bitmap) -> Unit = {}, // Callback khi Bitmap sẵn sàng
        onLoadFailed: (Exception?) -> Unit = {} // Callback khi tải thất bại (tùy chọn)
    ) {
        val shimmer = Shimmer.AlphaHighlightBuilder()
            .setDuration(1800)
            .setBaseAlpha(0.7f)
            .setHighlightAlpha(0.6f)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()

        val shimmerDrawable = ShimmerDrawable().apply {
            setShimmer(shimmer)
        }

        // Tạo GlideUrl với header
        val glideUrl = GlideUrl(
            url, LazyHeaders.Builder().addHeader("accessKey","03d01bc4-2cec-4d16-92ca5d0d2ca5-3eae-4cb8").build()
        )

        // Sử dụng Glide để tải ảnh dưới dạng Bitmap
        Glide.with(context).asBitmap() // Chuyển sang tải Bitmap thay vì trực tiếp vào ImageView
            .load(glideUrl).placeholder(shimmerDrawable).diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(requestOptions).listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    onLoadFailed(e) // Gọi callback khi thất bại
                    Log.d("TAG", "onLoadFailed: " + e)
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@loadImageWithHeader.setImageBitmap(resource) // Gán Bitmap vào ImageView
                    resource?.let {
                        onResourceReady(it)
                    } // Gọi callback khi Bitmap sẵn sàng
                    return true // Trả về true để ngăn Glide tự động gán ảnh
                }
            }).into(this) // Vẫn cần target là ImageView để Glide quản lý lifecycle
    }
}