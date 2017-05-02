package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Build
import android.widget.FrameLayout
import android.widget.ImageView
import it.facile.skanner.R

internal data class DetectionRequirements(val scan: Scan, val bitmap: Bitmap)

internal fun createPointer(context: Context, radius: Int, color: Int): ImageView {
    val pointer = if (Build.VERSION.SDK_INT >= 22)
        context.resources.getDrawable(R.drawable.pointer, context.theme)
    else
        context.resources.getDrawable(R.drawable.pointer)

    pointer.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

    return ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(radius * 2, radius * 2)
        setPadding(radius / 2, radius / 2, radius / 2, radius / 2)
        setImageDrawable(pointer)
    }
}

internal fun calculateScaleFactor(bitmap: Bitmap, imageView: ImageView): Float =
        minOf(imageView.measuredWidth.toFloat() / bitmap.width,
                imageView.measuredHeight.toFloat() / bitmap.height)

internal fun ImageView.setPosition(newX: Int, newY: Int, radius: Int) {
    x = newX.toFloat() - radius
    y = newY.toFloat() - radius
}

internal fun ImageView.getPosition(radius: Int): Pt =
        Math.round(x) + radius to Math.round(y) + radius

internal fun Canvas.drawLine(from: Pt, to: Pt, paint: Paint) =
        drawLine(from.first.toFloat(), from.second.toFloat(), to.first.toFloat(), to.second.toFloat(), paint)

