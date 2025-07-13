package com.example.bixi.customViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class FrameCourveLineAnimationView @JvmOverloads constructor(
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
    private var imageMargins : Int = 0

    private val linePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 12f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

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

    private val waveAmplitudes = mutableListOf<Float>()
    private var lastHeightUsed = -1

    private fun generateWaveAmplitudes(viewHeight: Int, viewWidth: Int) {

        imageMargins = dpToPx(35f, context).toInt()

        waveAmplitudes.clear()
        val strokeWidth = linePaint.strokeWidth
        val wavelength = 2 * strokeWidth
        val numberOfWaves = (viewHeight / wavelength).toInt()

        var count = 1f
        repeat(numberOfWaves + 1) { index -> // +1 pentru siguranță la margine
            // Verifică dacă sunt primele 3 valuri
            if (index < 3) {
                count += 0.1f // Poți ajusta acest pas pentru un increment mai mic sau mare
            }
            // Verifică dacă sunt ultimele 3 valuri
            else if (index >= numberOfWaves - 3) {
                count -= 0.1f // Poți ajusta acest pas pentru un decrement mai mic sau mare
            }

            val minAmp = viewWidth * 0.25f * count
            val maxAmp = viewWidth * 0.4f * count
            val amp = (minAmp + Math.random() * (maxAmp - minAmp)).toFloat()
            waveAmplitudes.add(amp)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width
        val viewHeight = height

        if (viewWidth == 0 || viewHeight == 0) return

        val strokeWidth = linePaint.strokeWidth
        val wavelength = 2 * strokeWidth
        val frequency = (2 * Math.PI / wavelength)
        val centerX = viewWidth / 2f

        // Generează o singură dată amplitudinile pentru toți valii
        if (viewHeight != lastHeightUsed || waveAmplitudes.isEmpty()) {
            generateWaveAmplitudes(viewHeight, viewWidth)
            lastHeightUsed = viewHeight
        }

        val path = Path()
        path.moveTo(centerX, 0f)

        var y = 0f
        var waveIndex = 0
        var currentAmplitude = waveAmplitudes.getOrElse(0) { 0f }

        while (y <= viewHeight) {
            // Trecem la valul următor la fiecare wavelength
            if ((y % wavelength) == 0f && waveIndex < waveAmplitudes.size) {
                currentAmplitude = waveAmplitudes[waveIndex]
                waveIndex++
            }

            val x = centerX + currentAmplitude * kotlin.math.sin(frequency * y).toFloat()
            path.lineTo(x, y)
            y += 1f
        }

        canvas.drawPath(path, linePaint)

        // Desenează frame-ul curent
        if (frames.isNotEmpty()) {
            val currentFrame = frames[currentFrameIndex]
            currentFrame.setBounds(0 + imageMargins, 0 + imageMargins, viewWidth - imageMargins, viewHeight - imageMargins)
            currentFrame.draw(canvas)
        }
    }

    fun dpToPx(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.density
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