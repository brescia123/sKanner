package it.facile.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import it.facile.skanner.R


class DetectionView @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {

    private data class PointerAttr(val pointerRadius: Int, val pointerColor: Int, val lineThickness: Float)

    private val viewAttrs by lazy {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DetectionView, 0, 0)
        try {
            val pointerRadius = a.getInteger(R.styleable.DetectionView_pointerRadius, 32)
            val pointerColor = a.getColor(R.styleable.DetectionView_pointerColor, R.color.adjustViewColor)
            val lineThickness = a.getFloat(R.styleable.DetectionView_lineThickness, 2f)
            return@lazy PointerAttr(pointerRadius, pointerColor, lineThickness)
        } finally {
            a.recycle()
        }
    }

    private val pointerView1: ImageView by lazy { createAndAddPointer(context, viewAttrs.pointerRadius, viewAttrs.pointerColor) }
    private val pointerView2: ImageView by lazy { createAndAddPointer(context, viewAttrs.pointerRadius, viewAttrs.pointerColor) }
    private val pointerView3: ImageView by lazy { createAndAddPointer(context, viewAttrs.pointerRadius, viewAttrs.pointerColor) }
    private val pointerView4: ImageView by lazy { createAndAddPointer(context, viewAttrs.pointerRadius, viewAttrs.pointerColor) }

    private val docImageView: ImageView by lazy { buildAndAddDocImageView(context) }

    private val TAG = this::class.java.simpleName
    private val paint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = viewAttrs.lineThickness
            color = viewAttrs.pointerColor
        }
    }


    private var requirements: DetectionRequirements? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (isInEditMode) return
        if (requirements == null) return

        positionPointers(requirements!!)
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.drawLine(
                from = pointerView1.getPosition(viewAttrs.pointerRadius),
                to = pointerView2.getPosition(viewAttrs.pointerRadius),
                paint = paint)
        canvas?.drawLine(
                from = pointerView2.getPosition(viewAttrs.pointerRadius),
                to = pointerView3.getPosition(viewAttrs.pointerRadius),
                paint = paint)
        canvas?.drawLine(
                from = pointerView3.getPosition(viewAttrs.pointerRadius),
                to = pointerView4.getPosition(viewAttrs.pointerRadius),
                paint = paint)
        canvas?.drawLine(
                from = pointerView4.getPosition(viewAttrs.pointerRadius),
                to = pointerView1.getPosition(viewAttrs.pointerRadius),
                paint = paint)
    }

    fun setScan(scan: Scan) {
        requirements?.bitmap?.recycle()
        requirements = DetectionRequirements(
                scan = scan,
                bitmap = loadBitmap(scan.scannedImageURI) ?: return)
        docImageView.setImageBitmap(requirements?.bitmap)
        setPointersTouchListener(pointerView1, requirements!!, viewAttrs.pointerRadius)
        setPointersTouchListener(pointerView2, requirements!!, viewAttrs.pointerRadius)
        setPointersTouchListener(pointerView3, requirements!!, viewAttrs.pointerRadius)
        setPointersTouchListener(pointerView4, requirements!!, viewAttrs.pointerRadius)
        invalidate()
    }

    fun getScan(): Scan? = requirements?.scan

    private fun setPointersTouchListener(pointer: ImageView,requirements: DetectionRequirements, radius: Int) {
        pointer.setOnTouchListener(PointerTouchListener(
                radius,
                requirements.bitmap.width,
                requirements.bitmap.height,
                { x, y ->
                    pointer.setPosition(x, y, radius)
                    invalidate()
                }))
    }

    private fun positionPointers(requirements: DetectionRequirements) {
        val scaledRectangle = requirements.scan.detectedRectangle.scale(calculateScaleFactor(requirements.bitmap, docImageView))
        pointerView1.setPosition(scaledRectangle.p1.first, scaledRectangle.p1.second, viewAttrs.pointerRadius)
        pointerView2.setPosition(scaledRectangle.p2.first, scaledRectangle.p2.second, viewAttrs.pointerRadius)
        pointerView3.setPosition(scaledRectangle.p3.first, scaledRectangle.p3.second, viewAttrs.pointerRadius)
        pointerView4.setPosition(scaledRectangle.p4.first, scaledRectangle.p4.second, viewAttrs.pointerRadius)
    }

    private fun createAndAddPointer(context: Context, radius: Int, color: Int): ImageView =
            createPointer(context, radius, color).apply { addView(this) }

    private fun buildAndAddDocImageView(context: Context) = ImageView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        adjustViewBounds = true
        addView(this)
    }
}