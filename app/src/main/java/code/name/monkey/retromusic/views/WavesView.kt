package code.name.monkey.retromusic.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.util.TiltListener
import code.name.monkey.retromusic.util.WaveTiltSensor
import code.name.monkey.retromusic.util.theme.ThemeManager
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin


/**
 * @Author: javlon
 * @Date: 15/02/2022
 */
class WavesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = R.attr.wavesViewStyle
) :
    View(context, attrs, defStyleAttr), TiltListener {

    private val wavePaint: Paint
    private val waveGap: Float

    private var maxRadius = 0f
    private var center = PointF(0f, 0f)
    private var initialRadius = 0f

    private val wavePath = Path()

    private val gradientMatrix = Matrix()

    private val tiltSensor = WaveTiltSensor(context)

    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Highlight only the areas already touched on the canvas
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    // gradient colors
    private val green = ThemeStore.accentColor(context)

    // solid green in the center, transparent green at the edges
    private val gradientColors =
        intArrayOf(
            green, modifyAlpha(green, 22),
            modifyAlpha(green, 5)
        )

    private var waveAnimator: ValueAnimator? = null
    private var waveRadiusOffset = 0f
        set(value) {
            field = value
            postInvalidateOnAnimation()
        }

    init {
        val attrs = context.obtainStyledAttributes(attrs, R.styleable.WavesView, defStyleAttr, 0)
        //init paint with custom attrs
        wavePaint = Paint(ANTI_ALIAS_FLAG).apply {
            color = attrs.getColor(R.styleable.WavesView_waveColor, 0)
            strokeWidth = attrs.getDimension(R.styleable.WavesView_waveStrokeWidth, 0f)
            style = Paint.Style.STROKE
        }

        waveGap = attrs.getDimension(R.styleable.WavesView_waveGap, 50f)
        attrs.recycle()
    }

    fun playAnimation() {
        if (waveAnimator?.isStarted == true) {
            waveAnimator?.resume()
        } else {
            waveAnimator?.start()
        }
    }

    fun pauseAnimation() {
        waveAnimator?.pause()
    }

    fun toggleAnimation() {
        if (waveAnimator?.isRunning == true) {
            if (waveAnimator?.isPaused == true) {
                waveAnimator?.resume()
            } else {
                waveAnimator?.pause()
            }
        } else {
            if (waveAnimator?.isStarted == true) {
                waveAnimator?.resume()
            } else {
                waveAnimator?.start()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        waveAnimator = ValueAnimator.ofFloat(0f, waveGap).apply {
            addUpdateListener {
                waveRadiusOffset = it.animatedValue as Float
            }
            duration = 1500L
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
//            start()
        }
        tiltSensor.addListener(this)
        tiltSensor.register()
    }

    override fun onDetachedFromWindow() {
        waveAnimator?.cancel()
        tiltSensor.unregister()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        //set the center of all circles to be center of the view
        center.set(w / 2f, h / 2f)
        maxRadius = hypot(center.x.toDouble(), center.y.toDouble()).toFloat()
        initialRadius = w / waveGap

        gradientPaint.shader = RadialGradient(
            center.x, center.y, maxRadius,
            gradientColors, null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //draw circles separated by a space the size of waveGap
        var currentRadius = initialRadius + waveRadiusOffset
        while (currentRadius < maxRadius) {
            canvas.drawCircle(center.x, center.y, currentRadius, wavePaint)
            currentRadius += waveGap
        }

//        var currentRadius = initialRadius + waveRadiusOffset
//        while (currentRadius < maxRadius) {
//            val path = createStarPath(currentRadius, wavePath)
//            canvas.drawPath(path, wavePaint)
//            currentRadius += waveGap
//        }

        canvas.drawPaint(gradientPaint)
    }

    private fun createStarPath(
        radius: Float,
        path: Path = Path(),
        points: Int = 8
    ): Path {
        path.reset()
        val pointDelta = 0.7f // difference between the "far" and "close" points from the center
        val angleInRadians =
            2.0 * Math.PI / points // essentially 360/20 or 18 degrees, angle each line should be drawn
        val startAngleInRadians = 0.0 //starting to draw star at 0 degrees

        //move pointer to 0 degrees relative to the center of the screen
        path.moveTo(
            center.x + (radius * pointDelta * cos(startAngleInRadians)).toFloat(),
            center.y + (radius * pointDelta * sin(startAngleInRadians)).toFloat()
        )

        //create a line between all the points in the star
        for (i in 1 until points) {
            val hypotenuse = if (i % 2 == 0) {
                //by reducing the distance from the circle every other points, we create the "dip" in the star
                pointDelta * radius
            } else {
                radius
            }

            val nextPointX =
                center.x + (hypotenuse * cos(startAngleInRadians - angleInRadians * i)).toFloat()
            val nextPointY =
                center.y + (hypotenuse * sin(startAngleInRadians - angleInRadians * i)).toFloat()
            path.lineTo(nextPointX, nextPointY)
        }

        path.close()
        return path
    }

    private fun modifyAlpha(color: Int, alpha: Int): Int {
        return color and 0x00ffffff or (alpha shl alpha)
    }

    private fun updateGradient(x: Float, y: Float) {
        gradientMatrix.setTranslate(x - center.x, y - center.y)
        gradientPaint.shader?.setLocalMatrix(gradientMatrix)
        postInvalidateOnAnimation()
    }

    override fun onTilt(pitchRollRad: Pair<Double, Double>) {
        val pitchRad = pitchRollRad.first
        val rollRad = pitchRollRad.second

        val maxYOffset = center.y.toDouble()
        val maxXOffset = center.x.toDouble()

        val yOffset = (sin(pitchRad) * maxYOffset)
        val xOffset = (sin(rollRad) * maxXOffset)

//        updateGradient(xOffset.toFloat() + center.x, yOffset.toFloat() + center.y)
    }
}