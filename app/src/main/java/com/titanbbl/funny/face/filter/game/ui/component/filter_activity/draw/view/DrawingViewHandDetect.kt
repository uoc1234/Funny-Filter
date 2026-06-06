package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sqrt

class DrawingViewHandDetect @JvmOverloads constructor(
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
    
    // Màu sắc khác khi bàn tay đóng
    private val fingerClosedPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLUE
        alpha = 220
    }
    
    private val fingerClosedOutlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.CYAN
        alpha = 255
    }

    private var currentPoint: PointF? = null
    private var lastPoint: PointF? = null
    
    // Trạng thái vẽ hiện tại
    private var isCurrentlyDrawing = true
    
    // For smoother movement
    private var targetX = 0f
    private var targetY = 0f
    private var smoothingFactor = 0.3f // Lower = smoother but more lag
    
    // Finger indicator size
    private var fingerIndicatorRadius = 30f

    // Lưu trữ các đoạn đường vẽ riêng biệt
    private val paths = mutableListOf<Path>()
    private val pathPoints = mutableListOf<MutableList<PointF>>()
    
    // Đoạn đường vẽ hiện tại
    private var currentPath = Path()
    private var currentPathPoints = mutableListOf<PointF>()
    
    // Biến để đánh dấu điểm bắt đầu của một đoạn đường vẽ mới
    private val newPathMarkers = mutableListOf<Int>()

    // Animation variables for smooth transitions
    private var animationProgress = 0f
    private var isAnimating = false

    fun updateDrawing(point: PointF?, isDrawing: Boolean) {
        if (point == null) return
        
        // Kiểm tra xem có chuyển từ trạng thái không vẽ sang trạng thái vẽ không
        val startNewPath = !isCurrentlyDrawing && isDrawing
        
        // Lưu trạng thái vẽ hiện tại
        isCurrentlyDrawing = isDrawing
        
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
        
        // Chỉ thêm điểm vào đường vẽ khi isDrawing là true (bàn tay mở)
        if (isDrawing) {
            // Nếu bắt đầu vẽ lại sau khi dừng, đánh dấu điểm bắt đầu mới
            if (startNewPath) {
                if (currentPathPoints.isNotEmpty()) {
                    // Lưu đường vẽ hiện tại
                    paths.add(Path(currentPath))
                    pathPoints.add(ArrayList(currentPathPoints))
                    
                    // Tạo đường vẽ mới
                    currentPath = Path()
                    currentPathPoints = mutableListOf()
                }
                
                // Bắt đầu đường vẽ mới tại vị trí hiện tại
                currentPath.moveTo(currentPoint!!.x, currentPoint!!.y)
                currentPathPoints.add(PointF(currentPoint!!.x, currentPoint!!.y))
                lastPoint = currentPoint
            } 
            // Nếu đang vẽ liên tục, thêm điểm vào đường vẽ hiện tại
            else if (currentPathPoints.isEmpty() || lastPoint == null || lastPoint!!.distanceTo(currentPoint!!) > MIN_DISTANCE) {
                if (currentPathPoints.isEmpty()) {
                    currentPath.moveTo(currentPoint!!.x, currentPoint!!.y)
                } else {
                    currentPath.lineTo(currentPoint!!.x, currentPoint!!.y)
                }
                
                currentPathPoints.add(PointF(currentPoint!!.x, currentPoint!!.y))
                lastPoint = currentPoint
            }
        }
        
        invalidate()
    }

    fun clearDrawing() {
        paths.clear()
        pathPoints.clear()
        currentPath = Path()
        currentPathPoints.clear()
        lastPoint = null
        invalidate()
    }

    /**
     * Set the drawing color
     */
    fun setDrawingColor(color: Int) {
        paint.color = color
        invalidate()
    }

    /**
     * Set the stroke width
     */
    fun setStrokeWidth(width: Float) {
        paint.strokeWidth = width
        invalidate()
    }

    /**
     * Get the current drawing as a bitmap
     */
    fun getDrawingBitmap(): Bitmap? {
        if (width <= 0 || height <= 0) return null
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        canvas.drawColor(Color.TRANSPARENT)
        
        // Draw all completed paths
        for (path in paths) {
            canvas.drawPath(path, paint)
        }
        
        // Draw current path
        canvas.drawPath(currentPath, paint)
        
        return bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ các đường vẽ đã hoàn thành
        for (path in paths) {
            canvas.drawPath(path, paint)
        }
        
        // Vẽ đường vẽ hiện tại
        canvas.drawPath(currentPath, paint)
        
        // Draw current finger position indicator
        currentPoint?.let { point ->
            // Chọn màu dựa trên trạng thái bàn tay
            val fillPaint = if (isCurrentlyDrawing) fingerPaint else fingerClosedPaint
            val outlinePaint = if (isCurrentlyDrawing) fingerOutlinePaint else fingerClosedOutlinePaint
            
            // Draw finger indicator with animation
            val radius = fingerIndicatorRadius * (1f + animationProgress * 0.2f)
            canvas.drawCircle(point.x, point.y, radius, fillPaint)
            canvas.drawCircle(point.x, point.y, radius, outlinePaint)
            
            // Add a small dot in the center for better precision
            val centerPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = if (isCurrentlyDrawing) Color.RED else Color.WHITE
            }
            canvas.drawCircle(point.x, point.y, 5f, centerPaint)
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