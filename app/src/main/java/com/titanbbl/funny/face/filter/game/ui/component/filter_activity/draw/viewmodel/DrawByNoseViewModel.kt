package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.viewmodel

import android.graphics.PointF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DrawByNoseViewModel : ViewModel() {
    private val _currentPoint = MutableLiveData<PointF>()
    val currentPoint: LiveData<PointF> = _currentPoint

    private val _isDrawing = MutableLiveData<Boolean>()
    val isDrawing: LiveData<Boolean> = _isDrawing
    
    private val _drawingMode = MutableLiveData<Boolean>()
    val drawingMode: LiveData<Boolean> = _drawingMode

    private val _shouldClearDrawing = MutableLiveData<Boolean>()
    val shouldClearDrawing: LiveData<Boolean> = _shouldClearDrawing
    
    // For tracking nose position stability
    private var lastUpdateTime = 0L
    private val updateThreshold = 16L // ~60fps
    private var lastX = 0f
    private var lastY = 0f
    private val minMoveDistance = 1.5f // Minimum movement to update
    
    init {
        _drawingMode.value = false
        _isDrawing.value = false
    }

    fun updateNosePosition(x: Float, y: Float) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle updates for smoother performance
        if (currentTime - lastUpdateTime < updateThreshold) {
            return
        }
        
        // Check if movement is significant enough to update
        if (lastX != 0f && lastY != 0f) {
            val dx = Math.abs(x - lastX)
            val dy = Math.abs(y - lastY)
            
            if (dx < minMoveDistance && dy < minMoveDistance) {
                return // Skip small movements for stability
            }
        }
        
        lastUpdateTime = currentTime
        lastX = x
        lastY = y
        _currentPoint.value = PointF(x, y)
    }

    fun setDrawing(drawing: Boolean) {
        if (_isDrawing.value != drawing) {
            _isDrawing.value = drawing
        }
    }
    
    fun toggleDrawingMode() {
        _drawingMode.value = _drawingMode.value?.not() ?: true
    }

    fun clearDrawing() {
        _shouldClearDrawing.value = true
    }

    fun clearDrawingComplete() {
        _shouldClearDrawing.value = false
    }
} 