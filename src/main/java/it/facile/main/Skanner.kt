@file:JvmName("SKanner")

package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Function that allow to scan a photo and detect a document inside it.
 *
 * @param originalImagePath the path of the image to be scanned.
 * @param context a context reference.
 * @param sampleSize the sample size used to decode the file into a Bitmap (default = 2).
 */
fun scanDocument(originalImagePath: String, context: Context, sampleSize: Int = 4): ScannedDocument {

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val originalBitmap = BitmapFactory.decodeFile(originalImagePath, options)
    val detectedRectangle = detectRectangle(originalBitmap)
    val transformedBitmap = transformPerspective(originalBitmap, detectedRectangle)

    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val originalFileName = File(originalImagePath).nameWithoutExtension
    val transformedFile = File.createTempFile("${originalFileName}_TRANSFORMED", ".jpg", storageDir)

    val fos = FileOutputStream(transformedFile)
    transformedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
    fos.close()

    return ScannedDocument(originalImagePath, transformedFile.absolutePath, detectedRectangle.scale(options.inSampleSize.toFloat()))
}

/**
 * Convenient function used to create a file inside [Environment.DIRECTORY_PICTURES] with the
 * following name scheme: SCAN_yyyyMMdd_HHmmss.jpg
 *
 * @param context a context reference.
 */
fun createOriginalImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
    val imageFileName = "SCAN_$timeStamp"
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val image = File.createTempFile(imageFileName, ".jpg", storageDir)
    return image
}


/**
 * Apply an algorithm to detect the bigger, doc-shaped rectangle within a Bitmap.
 */
internal fun detectRectangle(image: Bitmap): Rectangle = (0..2)
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

/**
 * Apply an algorithm transform the perspective of a given [Rectangle] within a Bitmap.
 */
internal fun transformPerspective(image: Bitmap, rect: Rectangle): Bitmap {

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

/**
 * Order the points of a [Rectangle] as following:
 * - p1 = Top-Left
 * - p2 = Top-Right
 * - p3 = Bottom-Right
 * - p4 = Bottom-Left
 */
private fun Rectangle.order(): Rectangle = with(asList()) {
    fun List<Pt>.findMaxWith(func: Pt.() -> Int): Pt = with(map(func)) { this@findMaxWith[indexOf(max())] }
    fun List<Pt>.findMinWith(func: Pt.() -> Int): Pt = with(map(func)) { this@findMinWith[indexOf(min())] }
    val sum: Pt.() -> Int = { first + second }
    val diff: Pt.() -> Int = { first - second }
    val tl = findMinWith(sum)
    val tr = findMinWith(diff)
    val br = findMaxWith(sum)
    val bl = findMaxWith(diff)
    return Rectangle(tl, tr, br, bl)
}

/**
 * Apply an algorithm to detect a list of contours within a Mat.
 */
private fun Mat.detectContours(ch: Int): ArrayList<MatOfPoint> {
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
