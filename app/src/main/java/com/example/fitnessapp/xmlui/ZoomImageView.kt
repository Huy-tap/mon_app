package com.example.fitnessapp.xmlui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.graphics.Canvas

/** Same 1–5x pinch and drag behavior as the previous exercise image screen. */
class ZoomImageView(context: Context, attrs: AttributeSet? = null) : ImageView(context, attrs) {
    var zoom = 1f
    var panX = 0f
    var panY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private val detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoom = (zoom * detector.scaleFactor).coerceIn(1f, 5f)
            if (zoom == 1f) { panX = 0f; panY = 0f }
            invalidate(); return true
        }
    })
    init { scaleType = ScaleType.FIT_CENTER }
    override fun onDraw(canvas: Canvas) {
        canvas.save(); canvas.translate(panX, panY); canvas.scale(zoom, zoom, width / 2f, height / 2f)
        super.onDraw(canvas); canvas.restore()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y }
            MotionEvent.ACTION_MOVE -> {
                if (!detector.isInProgress && zoom > 1f) { panX += event.x - lastX; panY += event.y - lastY; invalidate() }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> performClick()
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
