@file:JvmName("SKanner")

package it.facile.main

import android.graphics.Bitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

typealias Pt = Pair<Int, Int>
data class Rectangle(val p1: Pt, val p2: Pt, val p3: Pt, val p4: Pt)

fun detectRectangle(image: Bitmap): Rectangle = (0..2)
        .map { image.toMat().detectContours(it) } // Find all the contours ad different channel mixes
        .reduce { acc, arrayList -> acc.addAll(arrayList); acc } // Accumulate all the contours into a list
        .sortedByDescending(Imgproc::contourArea)
        .map { it.toMatOfPoint2f() }
        .map { it.approxPolyDP(Imgproc.arcLength(it, true) * 0.02, true) } // Approximate the contour to a polygon
        .filter { it.toList().size == 4 } // Select only quadrilaterals
        .filter { it -> it.maxCosine() < 0.3 } // Select the ones that have a doc shape
        .map { it.toRectangle() }
        .firstOrNull() // Take the first (max area)
        ?: image.perimeterRectangle() // if no rectangle is found return the bitmap perimeter contour

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
