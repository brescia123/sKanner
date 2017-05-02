package it.facile.main

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View

internal class PointerTouchListener(val pointerRadius: Int, val imageWidth: Int, val imageHeight: Int, val onTouch: () -> Unit, val onTouchFinished: () -> Unit) : View.OnTouchListener {

    private var downPt = PointF()
    private var startPt = PointF()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val mv = PointF(event.x - downPt.x, event.y - downPt.y)
                val (newX, newY) = positionInside(startPt.x + mv.x, startPt.y + mv.y, imageWidth, imageHeight)

                v.x = newX
                v.y = newY
                startPt = PointF(v.x, v.y)
            }
            MotionEvent.ACTION_DOWN -> {
                downPt.x = event.x
                downPt.y = event.y
                startPt = PointF(v.x, v.y)
            }
            MotionEvent.ACTION_UP -> {
                onTouchFinished.invoke()
            }
        }
        onTouch.invoke()
        return true
    }

    private fun positionInside(x: Float, y: Float, width: Int, height: Int): Pair<Float, Float> {
        val newX = if (x + pointerRadius > width) width.toFloat() - pointerRadius
        else if (x + pointerRadius < 0) 0f - pointerRadius
        else x

        val newY = if (y + pointerRadius > height) height.toFloat() - pointerRadius
        else if (y + pointerRadius < 0) 0f - pointerRadius
        else y

        return newX to newY
    }
}