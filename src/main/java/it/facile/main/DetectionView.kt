package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import it.facile.skanner.R
import java.io.File


class DetectionView @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {

    private data class PointerAttr(val pointerRadius: Int, val pointerColor: Int, val lineThickness: Float)

    private val viewAttrs: PointerAttr

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DetectionView, 0, 0)
        try {
            val pointerRadius = a.getInteger(R.styleable.DetectionView_pointerRadius, 96)
            val pointerColor = a.getColor(R.styleable.DetectionView_pointerColor, R.color.adjustViewColor)
            val lineThickness = a.getFloat(R.styleable.DetectionView_lineThickness, 4f)
            viewAttrs = PointerAttr(pointerRadius, pointerColor, lineThickness)
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

    /* View methods */

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (isInEditMode) return
        if (requirements == null) return


        setPointersTouchListener(pointerView1, docImageView.measuredWidth, docImageView.measuredHeight, viewAttrs.pointerRadius)
        setPointersTouchListener(pointerView2, docImageView.measuredWidth, docImageView.measuredHeight, viewAttrs.pointerRadius)
        setPointersTouchListener(pointerView3, docImageView.measuredWidth, docImageView.measuredHeight, viewAttrs.pointerRadius)
        setPointersTouchListener(pointerView4, docImageView.measuredWidth, docImageView.measuredHeight, viewAttrs.pointerRadius)

        positionPointers(requirements!!)

        docImageView.imageMatrix = Matrix().apply { preRotate(90f) }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.drawLine(
                from = pointerView1.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                to = pointerView2.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                paint = paint)
        canvas?.drawLine(
                from = pointerView2.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                to = pointerView3.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                paint = paint)
        canvas?.drawLine(
                from = pointerView3.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                to = pointerView4.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                paint = paint)
        canvas?.drawLine(
                from = pointerView4.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                to = pointerView1.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight),
                paint = paint)
    }

    /* APIs */

    var scanChangedListener: (Scan) -> Unit = {}

    fun rotate90() {
        class Trans(context: Context) : BitmapTransformation(context) {
            override fun getId() = "Trans"

            override fun transform(pool: BitmapPool?, toTransform: Bitmap?, outWidth: Int, outHeight: Int): Bitmap {
                return TransformationUtils.rotateImage(toTransform, 90).saveImage(requirements!!.scan.scannedImageURI)!!
            }

        }
        Glide.with(context)
                .load(File(requirements!!.scan.scannedImageURI.path)) // Uri of the picture
                .transform(Trans(context))
                .into(docImageView)
    }

    fun setScan(scan: Scan) {
        if (scan == requirements?.scan) return
        requirements = DetectionRequirements(
                scan = scan,
                bitmapDimensions = scan.scannedImageURI.detectBitmapDimension() ?: return)
        requestLayout()
        invalidate()
    }

    fun getScan(): Scan? = requirements?.scan

/* Utils method */

    private fun setPointersTouchListener(pointer: ImageView, imageWidth: Int, imageHeight: Int, radius: Int) {
        pointer.setOnTouchListener(PointerTouchListener(
                pointerRadius = radius,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                onTouch = { invalidate() },
                onTouchFinished = {
                    requirements = requirements?.updateScan()
                    scanChangedListener.invoke(requirements!!.scan) // We know requirements has not changed since we are on the MainThread
                }))
    }

    private fun DetectionRequirements.updateScan(): DetectionRequirements {
        val scaleFactor = 1 / calculateScaleFactor(this.bitmapDimensions, docImageView)
        return this.copy(scan = this.scan.copy(detectedRectangle = this.scan.detectedRectangle.copy(
                p1 = pointerView1.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight).scale(scaleFactor),
                p2 = pointerView2.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight).scale(scaleFactor),
                p3 = pointerView3.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight).scale(scaleFactor),
                p4 = pointerView4.getPosition(viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight).scale(scaleFactor)
        )))
    }

    private fun positionPointers(requirements: DetectionRequirements) {
        val scaledRectangle = requirements.scan.detectedRectangle.scale(calculateScaleFactor(requirements.bitmapDimensions, docImageView))
        pointerView1.setPosition(scaledRectangle.p1.first, scaledRectangle.p1.second, viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight)
        pointerView2.setPosition(scaledRectangle.p2.first, scaledRectangle.p2.second, viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight)
        pointerView3.setPosition(scaledRectangle.p3.first, scaledRectangle.p3.second, viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight)
        pointerView4.setPosition(scaledRectangle.p4.first, scaledRectangle.p4.second, viewAttrs.pointerRadius, measuredWidth - docImageView.measuredWidth, measuredHeight - docImageView.measuredHeight)
    }

    private fun createAndAddPointer(context: Context, radius: Int, color: Int): ImageView =
            createPointer(context, radius, color).apply { addView(this) }

    private fun buildAndAddDocImageView(context: Context) = ImageView(context).apply {
        val params = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        layoutParams = params
        scaleType = ImageView.ScaleType.MATRIX
        adjustViewBounds = true
        addView(this)
    }
}