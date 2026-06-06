package com.titanbbl.funny.face.filter.game.ui.component.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.titanbbl.funny.face.filter.game.R


class TextViewWithStroke @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var strokeWidth: Float = 0f
    private var strokeColor: Int = 0
    private var fillColor: Int = 0

    init {
        // Get custom attributes from XML
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TextViewWithStroke)
            try {
                strokeWidth = typedArray.getDimension(R.styleable.TextViewWithStroke_strokeWidth, 0f)
                strokeColor = typedArray.getColor(
                    R.styleable.TextViewWithStroke_strokeColor,
                    ContextCompat.getColor(context, android.R.color.black)
                )
                fillColor = typedArray.getColor(
                    R.styleable.TextViewWithStroke_fillColor,
                    currentTextColor
                )
            } finally {
                typedArray.recycle()
            }
        }

        // Enable layer for better drawing performance
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        // If no stroke, just draw normally
        if (strokeWidth <= 0) {
            super.onDraw(canvas)
            return
        }

        // Save the original paint settings
        val originalStyle = paint.style
        val originalStrokeWidth = paint.strokeWidth
        val originalStrokeJoin = paint.strokeJoin
        val originalStrokeCap = paint.strokeCap
        val originalColor = currentTextColor

        try {
            // Draw stroke first (background)
            paint.apply {
                style = Paint.Style.STROKE
                strokeWidth = this@TextViewWithStroke.strokeWidth
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            setTextColor(strokeColor)
            super.onDraw(canvas)

            // Draw fill text (foreground)
            paint.apply {
                style = Paint.Style.FILL
                strokeWidth = 0f
                isAntiAlias = true
            }
            setTextColor(if (fillColor != 0) fillColor else originalColor)
            super.onDraw(canvas)

        } finally {
            // Always restore original paint state
            paint.apply {
                style = originalStyle
                strokeWidth = originalStrokeWidth
                strokeJoin = originalStrokeJoin
                strokeCap = originalStrokeCap
            }
            setTextColor(originalColor)
        }
    }

    // Methods to set properties programmatically
    fun setStrokeWidth(width: Float) {
        if (strokeWidth != width) {
            strokeWidth = width
            invalidate()
        }
    }

    fun setStrokeColor(color: Int) {
        if (strokeColor != color) {
            strokeColor = color
            invalidate()
        }
    }

    fun setFillColor(color: Int) {
        if (fillColor != color) {
            fillColor = color
            invalidate()
        }
    }
}