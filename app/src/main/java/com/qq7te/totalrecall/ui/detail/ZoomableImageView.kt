package com.qq7te.totalrecall.ui.detail

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs

/**
 * Custom ImageView that supports pinch-to-zoom and pan gestures.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val initialMatrix = Matrix()
    
    // Touch event modes
    private enum class Mode {
        NONE, DRAG, ZOOM
    }
    
    private var mode = Mode.NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var minScale = 1f
    private var maxScale = 4f
    private var currentScale = 1f
    
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    
    // Callback for single tap events
    var onSingleTapListener: (() -> Unit)? = null
    
    init {
        // Start with FIT_CENTER so image is fully visible
        scaleType = ScaleType.FIT_CENTER
        
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }
    
    override fun setImageURI(uri: android.net.Uri?) {
        super.setImageURI(uri)
        // Switch to MATRIX mode after image is loaded for zoom support
        post {
            if (drawable != null) {
                initializeMatrix()
            }
        }
    }
    
    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        // Switch to MATRIX mode after image is loaded for zoom support
        post {
            if (drawable != null) {
                initializeMatrix()
            }
        }
    }
    
    private fun initializeMatrix() {
        // Get the fit-center matrix that was computed by the ImageView
        val fitMatrix = imageMatrix
        if (fitMatrix != null) {
            matrix.set(fitMatrix)
            savedMatrix.set(fitMatrix)
            initialMatrix.set(fitMatrix) // Store for reset
        }
        // Now switch to MATRIX mode for zoom control
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = Mode.DRAG
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = Mode.ZOOM
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.NONE
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG) {
                    matrix.set(savedMatrix)
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                } else if (mode == Mode.ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                    }
                }
            }
        }
        
        // Apply the matrix transformation
        imageMatrix = matrix
        return true
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = currentScale * scaleFactor
            
            if (newScale in minScale..maxScale) {
                currentScale = newScale
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                imageMatrix = matrix
            }
            return true
        }
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTapListener?.invoke()
            return true
        }
    }
    
    /**
     * Calculate the distance between two touch points
     */
    private fun spacing(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }
    
    /**
     * Calculate the midpoint between two touch points
     */
    private fun midPoint(point: PointF, event: MotionEvent) {
        if (event.pointerCount < 2) return
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
    
    /**
     * Reset the zoom to the original fit-to-screen scale
     */
    fun resetZoom() {
        matrix.set(initialMatrix)
        currentScale = 1f
        imageMatrix = matrix
        invalidate()
    }
}
