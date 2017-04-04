package it.facile.main

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
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

    private var scannedDocument: Scan? = null
    private var imageBitmap: Bitmap? = null
    private var imageView: ImageView? = null
    private var pointerView1: ImageView? = null
    private var pointerView2: ImageView? = null
    private var pointerView3: ImageView? = null
    private var pointerView4: ImageView? = null

    private val cornerIndicatorRadiusPx: Int by lazy { resources.getDimension(R.dimen.pointer_radius).toInt() / 2 }

    private var alreadyMeasured = false

    private val paint: Paint by lazy {
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.adjustViewColor)
            strokeWidth = 2f
            isAntiAlias = true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (alreadyMeasured.not()) drawPointers()
        alreadyMeasured = true
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)

        val zeroPt = 0 to 0
        canvas?.drawLine(from = pointerView1?.getPosition() ?: zeroPt, to = pointerView2?.getPosition() ?: zeroPt, paint = paint)
        canvas?.drawLine(from = pointerView2?.getPosition() ?: zeroPt, to = pointerView3?.getPosition() ?: zeroPt, paint = paint)
        canvas?.drawLine(from = pointerView3?.getPosition() ?: zeroPt, to = pointerView4?.getPosition() ?: zeroPt, paint = paint)
        canvas?.drawLine(from = pointerView4?.getPosition() ?: zeroPt, to = pointerView1?.getPosition() ?: zeroPt, paint = paint)
    }

    fun init(scannedDoc: Scan) {
        imageBitmap = BitmapFactory.decodeFile(scannedDoc.scannedImageURI.path)
        imageView = buildImageView()
        scannedDocument = scannedDoc
        addView(imageView)
        invalidate()
    }

    fun getNewRectangle(): Rectangle? {
        if (scannedDocument == null) throw IllegalStateException("The init method was not called.")
        val detectedRectangle = scannedDocument!!.detectedRectangle
        return detectedRectangle.copy(
                        p1 = pointerView1?.getPosition()?.scale(1 / calculateScaleFactor(imageBitmap!!, imageView!!)) ?: detectedRectangle.p1,
                        p2 = pointerView2?.getPosition()?.scale(1 / calculateScaleFactor(imageBitmap!!, imageView!!)) ?: detectedRectangle.p2,
                        p3 = pointerView3?.getPosition()?.scale(1 / calculateScaleFactor(imageBitmap!!, imageView!!)) ?: detectedRectangle.p3,
                        p4 = pointerView4?.getPosition()?.scale(1 / calculateScaleFactor(imageBitmap!!, imageView!!)) ?: detectedRectangle.p4)
    }


    private fun calculateScaleFactor(bitmap: Bitmap, imageView: ImageView): Float =
            minOf(imageView.measuredWidth.toFloat() / bitmap.width,
                    imageView.measuredHeight.toFloat() / bitmap.height)

    private fun drawPointers() {
        if (alreadyMeasured || imageBitmap == null || imageView == null || scannedDocument == null) return
        initCornerViews(scannedDocument!!.detectedRectangle, imageBitmap!!, imageView!!)
        alreadyMeasured = true
        invalidate()
    }

    private fun initCornerViews(rectangle: Rectangle, bitmap: Bitmap, imageView: ImageView) {
        val scaledRectangle = rectangle.scale(calculateScaleFactor(bitmap, imageView))
        pointerView1 = buildPointerView(scaledRectangle.p1)
        pointerView2 = buildPointerView(scaledRectangle.p2)
        pointerView3 = buildPointerView(scaledRectangle.p3)
        pointerView4 = buildPointerView(scaledRectangle.p4)
        addView(pointerView1)
        addView(pointerView2)
        addView(pointerView3)
        addView(pointerView4)
    }

    private fun buildImageView() = ImageView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        adjustViewBounds = true
        setImageBitmap(imageBitmap)
    }

    private fun buildPointerView(pt: Pt): ImageView = ImageView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setImageResource(R.drawable.pointer)
        setPosition(pt)
        setOnTouchListener(TouchListener())
    }

    private fun ImageView.getPosition(): Pt = Math.round(x) + cornerIndicatorRadiusPx to Math.round(y) + cornerIndicatorRadiusPx

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
                    if (insideImage(mv)) {
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

        private fun insideImage(mv: PointF) = startPt.x + mv.x < imageView?.width ?: width && startPt.y + mv.y < imageView?.height ?: height && startPt.x + mv.x > 0 && startPt.y + mv.y > 0

    }
}