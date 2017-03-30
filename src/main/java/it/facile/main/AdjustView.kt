package it.facile.main

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import it.facile.skanner.R

class AdjustView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var detectedRectangle: Rectangle? = null
    private var imageBitmap: Bitmap? = null
    private var imageView: ImageView? = null

    private var alreadyMeasured = false

    private val cornerIndicatorRadiusPx: Int by lazy { resources.getDimension(R.dimen.corner_indicator_radius).toInt() / 2 }
    private val paint: Paint by lazy {
        Paint().apply {
            color = resources.getColor(R.color.adjustViewColor)
            strokeWidth = 2f
            isAntiAlias = true
        }
    }

    private lateinit var cornerView1: ImageView
    private lateinit var cornerView2: ImageView
    private lateinit var cornerView3: ImageView
    private lateinit var cornerView4: ImageView


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (alreadyMeasured) return
        Log.d("AdjustView", "OnMeasure - imageView: (${imageView?.measuredWidth}, ${imageView?.measuredHeight})")
        val scaleFactor = calculateScaleFactor(imageBitmap, imageView)
        Log.d("AdjustView", "OnMeasure - scaleFactor: $scaleFactor")
        detectedRectangle?.scale(scaleFactor)?.let { initCornerViews(it) }
        alreadyMeasured = true
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.drawLine(from = cornerView1.getPosition(), to = cornerView2.getPosition(), paint = paint)
        canvas?.drawLine(from = cornerView2.getPosition(), to = cornerView3.getPosition(), paint = paint)
        canvas?.drawLine(from = cornerView3.getPosition(), to = cornerView4.getPosition(), paint = paint)
        canvas?.drawLine(from = cornerView4.getPosition(), to = cornerView1.getPosition(), paint = paint)
    }

    fun init(imagePath: String, detectedRectangle: Rectangle) {
        val options = BitmapFactory.Options()
        options.inSampleSize = 2
        imageBitmap = BitmapFactory.decodeFile(imagePath, options)
        imageBitmap?.let { imageView = buildImageView(it) }
        this.detectedRectangle = detectedRectangle.scale(1 / options.inSampleSize.toFloat())
        addView(imageView)
        invalidate()
    }

    private fun calculateScaleFactor(bitmap: Bitmap?, imageView: ImageView?): Float {
        if (bitmap == null || imageView == null) return 1f
        val toFloat = minOf(imageView.measuredWidth.toFloat() / bitmap.width,
                imageView.measuredHeight.toFloat() / bitmap.height)
        Log.d("AdjustView", "scaleFactor: $toFloat")
        return toFloat
    }

    private fun initCornerViews(rectangle: Rectangle) {
        cornerView1 = buildCornerView(rectangle.p1)
        cornerView2 = buildCornerView(rectangle.p2)
        cornerView3 = buildCornerView(rectangle.p3)
        cornerView4 = buildCornerView(rectangle.p4)
        addView(cornerView1)
        addView(cornerView2)
        addView(cornerView3)
        addView(cornerView4)
    }

    private fun buildImageView(bitmap: Bitmap) = ImageView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        adjustViewBounds = true
        setImageBitmap(bitmap)
    }

    private fun buildCornerView(pt: Pt): ImageView = ImageView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setImageResource(R.drawable.pointer)
        setPosition(pt)
        setOnTouchListener(TouchListener())
    }

    private fun ImageView.getPosition(): Pt = x.toInt() + cornerIndicatorRadiusPx to y.toInt() + cornerIndicatorRadiusPx

    private fun ImageView.setPosition(pt: Pt) {
        x = (pt.first.toFloat()) - cornerIndicatorRadiusPx
        y = (pt.second.toFloat()) - cornerIndicatorRadiusPx
    }

    private fun Canvas.drawLine(from: Pt, to: Pt, paint: Paint) =
            drawLine(from.first.toFloat(), from.second.toFloat(), to.first.toFloat(), to.second.toFloat(), paint)


    private inner class TouchListener : OnTouchListener {

        private var downPt = PointF()
        private var startPt = PointF()

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val eid = event.action
            when (eid) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPt.x, event.y - downPt.y)
                    if (startPt.x + mv.x < imageView?.width ?: width && startPt.y + mv.y < imageView?.height ?: height && startPt.x + mv.x > 0 && startPt.y + mv.y > 0) {
                        v.x = startPt.x + mv.x - cornerIndicatorRadiusPx
                        v.y = startPt.y + mv.y - cornerIndicatorRadiusPx
                        startPt = PointF(v.x + cornerIndicatorRadiusPx, v.y + cornerIndicatorRadiusPx)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    downPt.x = event.x
                    downPt.y = event.y
                    startPt = PointF(v.x + cornerIndicatorRadiusPx, v.y + cornerIndicatorRadiusPx)
                }
            }
            invalidate()
            return true
        }

    }
}

private fun Rectangle.scale(scaleFactor: Float) = Rectangle(
        p1.scale(scaleFactor),
        p2.scale(scaleFactor),
        p3.scale(scaleFactor),
        p4.scale(scaleFactor))

private fun Pt.scale(scaleFactor: Float): Pt = (first * scaleFactor).toInt() to (second * scaleFactor).toInt()
