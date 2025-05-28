package com.example.bixi.customViews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.bixi.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class BulbSurfaceView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val drawThread = DrawThread(holder, this)

    private lateinit var lightBulbOnBitmap : Bitmap
    private lateinit var lightBulbOffBitmap : Bitmap
    private lateinit var backgroundBitmap : Bitmap

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var anchorX = 0f
    private var anchorY = 0f

    private var lampX = 0f
    private var lampY = 0f
    private var velocityX = 0f
    private var velocityY = 0f

    private var lampWidth = 0
    private var lampHeight = 0

    private var lampDefaultYPos = 0f

    private var textPaint = Paint();

    private var turnLightOnText: String = ""
    private var turnLightOffText: String = ""
    private var lightStatusText: String = ""

    private var shouldDraw : Boolean = false

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        turnLightOnText = context.getString(R.string.turn_on_the_light_for_start)
        turnLightOffText = context.getString(R.string.turn_off_the_light_for_start)
        lightStatusText = turnLightOnText

        anchorX = width / 2f
        anchorY = 0f

        lampDefaultYPos = width.toFloat() / 1.4f

        lampX = anchorX
        lampY = lampDefaultYPos

        drawThread.running = true
        drawThread.start()

        lampHeight = (width / 2.5f).toInt();
        lampWidth = (lampHeight / 1.9f).toInt()

        lightBulbOnBitmap = getScaledBitmap(R.drawable.ic_light_bulb_on2, lampWidth, lampHeight)
        lightBulbOffBitmap = getScaledBitmap(R.drawable.ic_light_bulb_off, lampWidth, lampHeight)
        backgroundBitmap = getScaledBitmap(R.drawable.ic_lamp_background, height, height)

        textPaint = Paint().apply {
            color = Color.WHITE // Color.argb(255, 255, 162, 0)
            textSize = 50f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bouncingXFactor = (width / 5) / 5
        val minBouncingYFactor = (width / 5) / 6
        val maxBouncingYFactor = (width / 5) / 7

        bounceVelocityYRange = -minBouncingYFactor.toFloat()..-maxBouncingYFactor.toFloat()
        bounceVelocityXRange = -bouncingXFactor.toFloat()..bouncingXFactor.toFloat()

        shouldDraw = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        drawThread.running = false
        drawThread.join()
    }

    private var dragging = false
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    var isLampOn = false
    var wasTouchingLamp = false

    private var downX = 0f
    private var downY = 0f
    private val bounceThreshold = 20f  // toleranță la mișcare
    private var bounceVelocityYRange = -40f..-25f
    private var bounceVelocityXRange = -20f..20f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y

                val lampWidth = lightBulbOnBitmap.width
                val lampHeight = lightBulbOnBitmap.height
                val left = lampX - lampWidth / 2f
                val top = lampY - lampHeight / 2f
                val right = lampX + lampWidth / 2f
                val bottom = lampY + lampHeight / 2f

                if (event.x in left..right && event.y in top..bottom) {
                    dragging = true
                    touchOffsetX = event.x - lampX
                    touchOffsetY = event.y - lampY
                }

                val dx = event.x - lampX
                val dy = event.y - lampY
                val distance = sqrt(dx * dx + dy * dy)
                val radius = lampWidth / 2f + 50f
                val touchingNow = distance <= radius

                wasTouchingLamp = touchingNow
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    lampX = event.x - touchOffsetX
                    lampY = event.y - touchOffsetY
                    velocityX = 0f
                    velocityY = 0f
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val distanceMoved = sqrt(dx * dx + dy * dy)

                    if (distanceMoved < bounceThreshold) {
                        velocityY = Random.nextFloat() * (bounceVelocityYRange.endInclusive - bounceVelocityYRange.start) + bounceVelocityYRange.start
                        velocityX = Random.nextFloat() * (bounceVelocityXRange.endInclusive - bounceVelocityXRange.start) + bounceVelocityXRange.start

                        isLampOn = !isLampOn
                        lightStatusText = if(isLampOn) turnLightOffText else turnLightOnText
                    }
                }

                dragging = false
            }
        }
        return true
    }


    fun update() {
        if (!dragging) {
            val stiffness = 0.015f  // mai mic = mai puțin elastic
            val damping = 0.90f     // mai mic = se oprește mai repede

            val dx = anchorX - lampX
            val dy = (lampDefaultYPos) - lampY

            velocityX += dx * stiffness
            velocityY += dy * stiffness

            velocityX *= damping
            velocityY *= damping

            lampX += velocityX
            lampY += velocityY
        }
    }

    fun drawCanvas(canvas: Canvas) {

        if(!shouldDraw){
            return
        }

        canvas.drawColor(Color.BLACK)
//        canvas.drawColor(if(isLampOn) Color.argb(255, 92, 40, 0) else Color.BLACK)

        if(isLampOn){
            canvas.drawBitmap(backgroundBitmap, (-(width / 2)).toFloat(), 0f, null)
        }

        drawText(canvas)

        val lampWidth = lightBulbOnBitmap.width
        val lampHeight = lightBulbOnBitmap.height

        // 🔹 Calculează unghiul firului și inversarea pentru realism
        val angleRadians = atan2(lampX - anchorX, lampY - anchorY)
        val angleDegrees = -Math.toDegrees(angleRadians.toDouble()).toFloat()

        // 🔹 Calculează poziția de sus a becului după rotație
        val sin = sin(Math.toRadians(angleDegrees.toDouble())).toFloat()
        val cos = cos(Math.toRadians(angleDegrees.toDouble())).toFloat()

        val topLocalX = 0f
        val topLocalY = -lampHeight / 2f

        val lampTopX = lampX + topLocalX * cos - topLocalY * sin
        val lampTopY = lampY + topLocalX * sin + topLocalY * cos

        // 🔹 Desenează firul curbat de la anchor la partea superioară rotită a becului
        paint.color = if(isLampOn) Color.BLACK else Color.DKGRAY
        paint.strokeWidth = 8f
        paint.style = Paint.Style.STROKE

        val path = Path()
        path.moveTo(anchorX, anchorY)

        val controlX = (anchorX + lampTopX) / 2f
        val controlY = (anchorY + lampTopY) / 2f + 100f  // Curbura spre jos

        path.quadTo(controlX, controlY, lampTopX, lampTopY)
        canvas.drawPath(path, paint)

        // 🔹 Desenează becul rotit cu Matrix
        val matrix = Matrix()
        matrix.postTranslate(-lampWidth / 2f, -lampHeight / 2f)  // Centrare
        matrix.postRotate(angleDegrees)                          // Rotire în jurul centrului
        matrix.postTranslate(lampX, lampY)                       // Poziționare finală

        val lampBitmap = if (isLampOn) lightBulbOnBitmap else lightBulbOffBitmap
        canvas.drawBitmap(lampBitmap, matrix, null)
    }

    private fun drawText(canvas : Canvas){
        val bounds = Rect()
        textPaint.getTextBounds(lightStatusText, 0, lightStatusText.length, bounds)

        val x = (canvas.width - bounds.width()) / 2f - bounds.left
        val y = (canvas.height + bounds.height()) / 2f - bounds.bottom

        canvas.drawText(lightStatusText, x, y + 200, textPaint)
    }

    private fun getScaledBitmap(id: Int, width: Int, height: Int): Bitmap {
        val contextR = context.applicationContext
        val resources = contextR.resources
        val MyCarBitmap = BitmapFactory.decodeResource(resources, id)
        return Bitmap.createScaledBitmap(MyCarBitmap, width, height, false)
    }

    inner class DrawThread(private val surfaceHolder: SurfaceHolder, private val lampView: BulbSurfaceView) : Thread() {
        var running = false

        override fun run() {
            while (running) {
                val canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        lampView.update()
                        lampView.drawCanvas(canvas)
                    }
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
                sleep(16) // ~60fps
            }
        }
    }
}
