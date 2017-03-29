package it.facile.main

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

internal fun MatOfPoint.toMatOfPoint2f() = MatOfPoint2f().also { matOfPoint2f: MatOfPoint2f -> convertTo(matOfPoint2f, CvType.CV_32F) }
internal fun Bitmap.toMat(): Mat = Mat().also { mat: Mat -> Utils.bitmapToMat(this, mat) }
internal fun Bitmap.toMatOfPoint2f(): MatOfPoint2f = MatOfPoint2f().also { mat: Mat -> Utils.bitmapToMat(this, mat) }
internal fun Mat.toBitmap(config: Bitmap.Config): Bitmap = Bitmap.createBitmap(cols(), rows(), config).also { bitmap: Bitmap -> Utils.matToBitmap(this, bitmap) }

internal fun Rectangle.toMatOfPoint2f() = MatOfPoint2f(p1.toPoint(), p2.toPoint(), p3.toPoint(), p4.toPoint())
internal fun MatOfPoint2f.toRectangle(): Rectangle? {
    if (toList().size != 4) return null
    return Rectangle(toList()[0].toPt(), toList()[1].toPt(), toList()[2].toPt(), toList()[3].toPt())
}

internal fun Pt.toPoint() = Point(first.toDouble(), second.toDouble())
internal fun Point.toPt() = Pt(x.toInt(), y.toInt())

internal fun Bitmap.perimeterRectangle() = Rectangle(0 to 0, 0 to height, width to height, width to 0)
internal fun Bitmap.perimeterRectangleScaled(ratio: Float): Rectangle {
    val scaledWidth = (width * ratio).toInt()
    val scaledHeight = (height * ratio).toInt()
    val horizontalShift = (width - scaledWidth) / 2
    val verticalShift = (height - scaledHeight) / 2
    val p1 = horizontalShift to verticalShift
    val p2 = horizontalShift to scaledHeight + verticalShift
    val p3 = scaledWidth + horizontalShift to scaledHeight + verticalShift
    val p4 = scaledWidth + horizontalShift to verticalShift
    return Rectangle(p1, p2, p3, p4)
}

internal fun MatOfPoint2f.maxCosine(): Double = (2..4)
        .map { Math.abs(angle(toList()[it % 4], toList()[it - 2], toList()[it - 1])) }
        .max() ?: 0.0

internal fun MatOfPoint2f.approxPolyDP(epsilon: Double, closed: Boolean): MatOfPoint2f {
    val approx = MatOfPoint2f()
    Imgproc.approxPolyDP(this, approx, epsilon, closed)
    return approx
}

internal fun Mat.findContours(): ArrayList<MatOfPoint> {
    val contours = arrayListOf<MatOfPoint>()
    Imgproc.findContours(this, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
    return contours
}

internal fun angle(p1: Point, p2: Point, p0: Point): Double {
    val dx1 = p1.x - p0.x
    val dy1 = p1.y - p0.y
    val dx2 = p2.x - p0.x
    val dy2 = p2.y - p0.y
    return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
}