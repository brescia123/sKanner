package it.facile.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
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

    private lateinit var rectangle: Rectangle

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.drawLine(from = cornerView1.getPosition(), to = cornerView2.getPosition(), paint = paint)
        canvas?.drawLine(from = cornerView2.getPosition(), to = cornerView3.getPosition(), paint = paint)
        canvas?.drawLine(from = cornerView3.getPosition(), to = cornerView4.getPosition(), paint = paint)
        canvas?.drawLine(from = cornerView4.getPosition(), to = cornerView1.getPosition(), paint = paint)
    }

    fun init(detectedRectangle: Rectangle) {
        rectangle = detectedRectangle
        initCornerViews()
        invalidate()
    }

    private fun initCornerViews() {
        cornerView1 = buildCornerView(rectangle.p1)
        cornerView2 = buildCornerView(rectangle.p2)
        cornerView3 = buildCornerView(rectangle.p3)
        cornerView4 = buildCornerView(rectangle.p4)
        addView(cornerView1)
        addView(cornerView2)
        addView(cornerView3)
        addView(cornerView4)
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
                    if (startPt.x + mv.x + v.width < width && startPt.y + mv.y + v.height < height && startPt.x + mv.x > 0 && startPt.y + mv.y > 0) {
                        v.x = startPt.x + mv.x
                        v.y = startPt.y + mv.y
                        startPt = PointF(v.x, v.y)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    downPt.x = event.x
                    downPt.y = event.y
                    startPt = PointF(v.x, v.y)
                }
            }
            invalidate()
            return true
        }

    }
}