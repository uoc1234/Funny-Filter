package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.apply
import kotlin.collections.isNotEmpty
import kotlin.let
import kotlin.math.sqrt
import kotlin.ranges.until

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.GREEN
        strokeWidth = 12f
    }
    
    private val fingerPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 220
    }
    
    private val fingerOutlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.RED
        alpha = 255
    }

    private var currentPoint: PointF? = null
    private var lastPoint: PointF? = null
    private val path = Path()
    private val points = mutableListOf<PointF>()
    
    // Thêm phương thức getTriangleInfo để tránh lỗi
    fun getTriangleInfo(): String {
        return "Không có thông tin tam giác"
    }
    
    // For smoother movement
    private var targetX = 0f
    private var targetY = 0f
    private var smoothingFactor = 0.3f // Lower = smoother but more lag
    
    // Finger indicator size
    private var fingerIndicatorRadius = 30f

    fun updateDrawing(point: PointF?, isDrawing: Boolean) {
        if (point == null) return
        
        // Apply smoothing for more stable drawing
        if (currentPoint == null) {
            currentPoint = point
            targetX = point.x
            targetY = point.y
        } else {
            targetX = point.x
            targetY = point.y
            
            // Smooth movement
            val smoothX = currentPoint!!.x + (targetX - currentPoint!!.x) * smoothingFactor
            val smoothY = currentPoint!!.y + (targetY - currentPoint!!.y) * smoothingFactor
            
            currentPoint = PointF(smoothX, smoothY)
        }
        
        // Add point to drawing path
        if (points.isEmpty() || lastPoint == null || lastPoint!!.distanceTo(currentPoint!!) > MIN_DISTANCE) {
            points.add(PointF(currentPoint!!.x, currentPoint!!.y))
            lastPoint = currentPoint
            updatePath()
        }
        
        invalidate()
    }

    private fun updatePath() {
        if (points.isEmpty()) return
        
        path.reset()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
    }

    fun clearDrawing() {
        points.clear()
        path.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the path
        if (points.isNotEmpty()) {
            canvas.drawPath(path, paint)
        }
        
        // Draw current finger position indicator
        currentPoint?.let { point ->
            // Draw finger indicator
            canvas.drawCircle(point.x, point.y, fingerIndicatorRadius, fingerPaint)
            canvas.drawCircle(point.x, point.y, fingerIndicatorRadius, fingerOutlinePaint)
        }
    }

    private fun PointF.distanceTo(other: PointF): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        private const val MIN_DISTANCE = 5f
    }
} 