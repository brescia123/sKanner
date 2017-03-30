@file:JvmName("SKanner")

package it.facile.main

import android.graphics.Bitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.Serializable


typealias Pt = Pair<Int, Int>
data class Rectangle(val p1: Pt, val p2: Pt, val p3: Pt, val p4: Pt): Serializable {
    fun asList() = listOf(p1, p2, p3, p4)
}

fun detectRectangle(image: Bitmap): Rectangle = (0..2)
        .map { image.toMat().detectContours(it) } // Find all the contours ad different channel mixes
        .reduce { acc, arrayList -> acc.addAll(arrayList); acc } // Accumulate all the contours into a list
        .filter { Imgproc.contourArea(it) > (image.width * image.height) / 10 } // Ignore contours too small
        .sortedByDescending(Imgproc::contourArea)
        .map { it.toMatOfPoint2f() }
        .map { it.approxPolyDP(Imgproc.arcLength(it, true) * 0.02, true) } // Approximate the contour to a polygon
        .filter { it.toList().size == 4 } // Select only quadrilaterals
        .filter { it -> it.maxCosine() < 0.3 } // Select the ones that have a doc shape
        .map { it.toRectangle() }
        .firstOrNull() // Take the first (max area)
        ?: image.perimeterRectangleScaled(0.8f) // if no rectangle is found return the bitmap perimeter contour

fun transformPerspective(image: Bitmap, rect: Rectangle): Bitmap {

    val orderedRectangle = rect.order()

    val tl = orderedRectangle.p1.toPoint()
    val tr = orderedRectangle.p4.toPoint()
    val br = orderedRectangle.p3.toPoint()
    val bl = orderedRectangle.p2.toPoint()

    val widthA = Math.sqrt(Math.pow(br.x - bl.x, 2.0) + Math.pow(br.y - bl.y, 2.0))
    val widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2.0) + Math.pow(tr.y - tl.y, 2.0))

    val maxWidth = maxOf(widthA, widthB)

    val heightA = Math.sqrt(Math.pow(tr.x - br.x, 2.0) + Math.pow(tr.y - br.y, 2.0))
    val heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2.0) + Math.pow(tl.y - bl.y, 2.0))

    val maxHeight = maxOf(heightA, heightB)

    val doc = Mat(maxHeight.toInt(), maxWidth.toInt(), CvType.CV_8UC4)

    val src_mat = Mat(4, 1, CvType.CV_32FC2)
    val dst_mat = Mat(4, 1, CvType.CV_32FC2)

    src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
    dst_mat.put(0, 0, 0.0, 0.0, maxWidth, 0.0, maxWidth, maxHeight, 0.0, maxHeight)

    val m = Imgproc.getPerspectiveTransform(src_mat, dst_mat)

    Imgproc.warpPerspective(image.toMat(), doc, m, doc.size())
    return doc.toBitmap(Bitmap.Config.ARGB_8888)
}

private fun Rectangle.order(): Rectangle = with(asList()) {
    val tl = findMinWith(Pt::sum)
    val tr = findMinWith(Pt::diff)
    val br = findMaxWith(Pt::sum)
    val bl = findMaxWith(Pt::diff)
    return Rectangle(tl, tr, br, bl)
}

private fun Pt.sum(): Int = first + second
private fun Pt.diff(): Int = first - second

private fun List<Pt>.findMaxWith(func: Pt.() -> Int): Pt = with(map(func)) { this@findMaxWith[indexOf(max())] }
private fun List<Pt>.findMinWith(func: Pt.() -> Int): Pt = with(map(func)) { this@findMinWith[indexOf(min())] }

internal fun Mat.detectContours(ch: Int): ArrayList<MatOfPoint> {
    val temp = Mat()
    Imgproc.cvtColor(this, temp, Imgproc.COLOR_BGR2RGB) // Change colors to BGR
    val tempBlurred = Mat()
    Imgproc.medianBlur(temp, tempBlurred, 9) // Blur the image
    val tempGray = Mat(tempBlurred.size(), CvType.CV_8U)
    Core.mixChannels(arrayListOf(tempBlurred), arrayListOf(tempGray), MatOfInt(*intArrayOf(ch, 0))) // Mix channels
    Imgproc.Canny(tempGray, temp, 10.0, 20.0, 3, true) // Apply Canny algorithm
    Imgproc.dilate(temp, temp, Mat(), Point(-1.0, -1.0), 1) // Apply dilate function to increase contour thickness
    return temp.findContours()
}
