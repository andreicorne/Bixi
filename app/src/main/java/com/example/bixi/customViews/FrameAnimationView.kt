package com.example.bixi.customViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class FrameAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var frames: MutableList<Drawable> = mutableListOf()
    private var currentFrameIndex = 0
    private var isAnimating = false
    private var frameDuration = 100L // millisecunde per frame
    private var handler = Handler(Looper.getMainLooper())
    private var animationRunnable: Runnable? = null
    private var isLooping = false
    private var onAnimationCompleteListener: (() -> Unit)? = null

    init {
        // Configurări inițiale dacă este necesar
    }

    /**
     * Adaugă o imagine la lista de frame-uri
     */
    fun addFrame(drawableRes: Int) {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        drawable?.let {
            frames.add(it)
            if (frames.size == 1) {
                invalidate() // Redraw pentru primul frame
            }
        }
    }

    /**
     * Adaugă multiple imagini dintr-o dată
     */
    fun addFrames(drawableResList: List<Int>) {
        drawableResList.forEach { addFrame(it) }
    }

    /**
     * Setează durata unui frame în millisecunde
     */
    fun setFrameDuration(duration: Long) {
        frameDuration = duration
    }

    /**
     * Setează dacă animația să se repete
     */
    fun setLooping(loop: Boolean) {
        isLooping = loop
    }

    /**
     * Setează listener pentru sfârșitul animației
     */
    fun setOnAnimationCompleteListener(listener: () -> Unit) {
        onAnimationCompleteListener = listener
    }

    /**
     * Pornește animația
     */
    fun startAnimation() {
        if (frames.isEmpty() || isAnimating) return

        isAnimating = true
        currentFrameIndex = 0
        scheduleNextFrame()
    }

    /**
     * Oprește animația
     */
    fun stopAnimation() {
        isAnimating = false
        animationRunnable?.let { handler.removeCallbacks(it) }
        animationRunnable = null
    }

    /**
     * Resetează animația la primul frame
     */
    fun resetAnimation() {
        stopAnimation()
        currentFrameIndex = 0
        invalidate()
    }

    /**
     * Verifică dacă animația este în desfășurare
     */
    fun isAnimating(): Boolean {
        return isAnimating
    }

    /**
     * Obține numărul total de frame-uri
     */
    fun getFrameCount(): Int {
        return frames.size
    }

    /**
     * Setează frame-ul curent manual
     */
    fun setCurrentFrame(index: Int) {
        if (index >= 0 && index < frames.size) {
            currentFrameIndex = index
            invalidate()
        }
    }

    private fun scheduleNextFrame() {
        animationRunnable = Runnable {
            if (!isAnimating) return@Runnable

            invalidate() // Redraw cu frame-ul curent

            currentFrameIndex++

            if (currentFrameIndex >= frames.size) {
                if (isLooping) {
                    currentFrameIndex = 0
                    scheduleNextFrame()
                } else {
                    // Animația s-a terminat
                    isAnimating = false
                    currentFrameIndex = frames.size - 1 // Rămâne la ultimul frame
                    onAnimationCompleteListener?.invoke()
                }
            } else {
                scheduleNextFrame()
            }
        }

        handler.postDelayed(animationRunnable!!, frameDuration)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (frames.isEmpty()) return

        val currentFrame = frames[currentFrameIndex]

        // Calculează dimensiunile pentru a centra imaginea
        val viewWidth = width
        val viewHeight = height

        if (viewWidth == 0 || viewHeight == 0) return

        // Setează bounds-urile pentru drawable
        currentFrame.setBounds(0, 0, viewWidth, viewHeight)
        currentFrame.draw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    /**
     * Șterge toate frame-urile
     */
    fun clearFrames() {
        stopAnimation()
        frames.clear()
        currentFrameIndex = 0
        invalidate()
    }
}