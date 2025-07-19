package com.example.bixi.helper

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import kotlin.math.min

class ImageZoomHelper(private val imageView: ImageView) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    // Puncte pentru tracking
    private val start = PointF()
    private val mid = PointF()

    // Scale limits
    private var minScale = 1f
    private var maxScale = 3f
    private var currentScale = 1f

    // Mode tracking
    private var mode = NONE

    // Pentru double tap
    private var lastTouchTime = 0L
    private val DOUBLE_TAP_TIMEOUT = 300L

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
    }

    private val scaleGestureDetector = ScaleGestureDetector(imageView.context, ScaleListener())

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            handleTouch(event)
            true
        }
    }

    fun setImage(drawable: Drawable?) {
        if (drawable == null) return

        imageView.setImageDrawable(drawable)

        // Reset matrix
        matrix.reset()
        imageView.imageMatrix = matrix

        // Calculate initial scale to fit image
        fitImageToView()
    }

    private fun fitImageToView() {
        val drawable = imageView.drawable ?: return

        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()

        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = min(scaleX, scaleY)

        minScale = scale
        currentScale = scale

        matrix.reset()
        matrix.postScale(scale, scale)

        // Center the image
        val dx = (viewWidth - imageWidth * scale) / 2
        val dy = (viewHeight - imageHeight * scale) / 2
        matrix.postTranslate(dx, dy)

        imageView.imageMatrix = matrix
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG

                // Check for double tap
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTouchTime < DOUBLE_TAP_TIMEOUT) {
                    handleDoubleTap(event.x, event.y)
                }
                lastTouchTime = currentTime
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(matrix)
                midPoint(mid, event)
                mode = ZOOM
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                    checkAndSetTranslate()
                } else if (mode == ZOOM) {
                    // Zoom handling is done in ScaleListener
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        return true
    }

    private fun handleDoubleTap(x: Float, y: Float) {
        if (currentScale > minScale) {
            // Zoom out to fit
            animateToScale(minScale, x, y)
        } else {
            // Zoom in
            val targetScale = min(maxScale, minScale * 2f)
            animateToScale(targetScale, x, y)
        }
    }

    private fun animateToScale(targetScale: Float, focusX: Float, focusY: Float) {
        val scaleFactor = targetScale / currentScale
        matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
        currentScale = targetScale
        checkAndSetTranslate()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun checkAndSetTranslate() {
        val drawable = imageView.drawable ?: return

        val imageRect = RectF()
        val viewRect = RectF(0f, 0f, imageView.width.toFloat(), imageView.height.toFloat())

        // Calculate image bounds
        imageRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        matrix.mapRect(imageRect)

        var deltaX = 0f
        var deltaY = 0f

        // Check horizontal bounds
        if (imageRect.width() <= viewRect.width()) {
            deltaX = viewRect.centerX() - imageRect.centerX()
        } else {
            if (imageRect.left > viewRect.left) {
                deltaX = viewRect.left - imageRect.left
            } else if (imageRect.right < viewRect.right) {
                deltaX = viewRect.right - imageRect.right
            }
        }

        // Check vertical bounds
        if (imageRect.height() <= viewRect.height()) {
            deltaY = viewRect.centerY() - imageRect.centerY()
        } else {
            if (imageRect.top > viewRect.top) {
                deltaY = viewRect.top - imageRect.top
            } else if (imageRect.bottom < viewRect.bottom) {
                deltaY = viewRect.bottom - imageRect.bottom
            }
        }

        matrix.postTranslate(deltaX, deltaY)
        imageView.imageMatrix = matrix
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val newScale = currentScale * scaleFactor

            // Limit scale
            scaleFactor = when {
                newScale < minScale -> minScale / currentScale
                newScale > maxScale -> maxScale / currentScale
                else -> scaleFactor
            }

            currentScale *= scaleFactor
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            checkAndSetTranslate()

            return true
        }
    }

    fun onViewSizeChanged() {
        // Re-fit image when view size changes
        if (imageView.drawable != null) {
            imageView.post { fitImageToView() }
        }
    }
}