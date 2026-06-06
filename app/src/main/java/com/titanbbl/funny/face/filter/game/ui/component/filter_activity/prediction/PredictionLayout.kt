package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.prediction

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.LayoutPredictionBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class PredictionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutPredictionBinding

    init {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.layout_prediction,
            this,
            true
        )

        // Parse custom attributes
        attrs?.let { parseAttributes(it) }
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PredictionLayout)
        try {
            // Set text
            val text = typedArray.getString(R.styleable.PredictionLayout_predictionText)
            text?.let { setText(it) }

            // Set image resource
            val imageRes = typedArray.getResourceId(R.styleable.PredictionLayout_predictionImage, 0)
            if (imageRes != 0) {
                setImageResource(imageRes)
            }

            // Set image URL
            val imageUrl = typedArray.getString(R.styleable.PredictionLayout_predictionImageUrl)
            imageUrl?.let { setImageUrl(it) }

            // Set text size
            val textSize = typedArray.getDimension(R.styleable.PredictionLayout_predictionTextSize, 20f)
            setTextSize(textSize)

            // Set text color
            val textColor = typedArray.getColor(R.styleable.PredictionLayout_predictionTextColor, 0)
            if (textColor != 0) {
                setTextColor(textColor)
            }

            // Set background color
            val backgroundColor = typedArray.getColor(R.styleable.PredictionLayout_predictionBackgroundColor, 0)
            if (backgroundColor != 0) {
                setBackgroundColor(backgroundColor)
            }

            // Set corner radius
            val cornerRadius = typedArray.getDimension(R.styleable.PredictionLayout_predictionCornerRadius, 0f)
            if (cornerRadius > 0f) {
                setCornerRadius(cornerRadius)
            }

        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Set text content
     */
    fun setText(text: String) {
        binding.tvContent.text = text
    }

    /**
     * Set text content with animation
     */
    fun setTextWithAnimation(text: String) {
        binding.tvContent.alpha = 0f
        binding.tvContent.text = text
        binding.tvContent.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /**
     * Set background drawable for text display
     */
    fun setBackgroundDrawable(drawableRes: Int) {
        binding.imgBackground.setBackgroundResource(drawableRes)
    }

    /**
     * Set image from resource ID
     */
    fun setImageResource(resourceId: Int) {
        Glide.with(context)
            .load(resourceId)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imgBackground)
    }

    /**
     * Set image from URL
     */
    fun setImageUrl(url: String) {
        Glide.with(context)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imgBackground)
    }

    /**
     * Set image from any type (String URL, Integer Resource, etc.)
     */
    fun setImage(image: Any) {
        Glide.with(context)
            .load(image)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imgBackground)
    }

    /**
     * Set text size
     */
    fun setTextSize(size: Float) {
        binding.
        tvContent.textSize = size
    }

    /**
     * Set Question
     */
    fun setQuestion(text: String){
        binding.tvQuestion.text = text
    }

    /**
     * Set text color
     */
    fun setTextColor(color: Int) {
        binding.tvContent.setTextColor(color)
    }

    /**
     * Set background color
     */
    fun setBackgroundColorCode(color: Int) {
        binding.imgBackground.setBackgroundColor(color)
    }

    /**
     * Set corner radius
     */
    fun setCornerRadius(radius: Float) {
        binding.imgBackground.clipToOutline = true
        binding.imgBackground.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }

    /**
     * Get current text
     */
    fun getText(): String {
        return binding.tvContent.text.toString()
    }

    /**
     * Clear image
     */
    fun clearImage() {
        binding.imgBackground.setImageDrawable(null)
    }
} 