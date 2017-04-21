package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import org.opencv.android.OpenCVLoader
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

object Skanner {

    private const val TAG = "Skanner"

    /**
     * Function needed to init OpenCV.
     */
    fun init() {
        if (OpenCVLoader.initDebug()) Log.i("Skanner", "OpenCV initialize success")
        else Log.i("Skanner", "OpenCV initialize failed")
    }

    /**
     * Convenient function used to create a file inside [Environment.DIRECTORY_PICTURES] using the
     * given fileName. If the file name is not provided it create a file with the
     * following name scheme: SCAN_yyyyMMdd_HHmmss.jpg . It returns the URI of the created file,
     * null if there was a problem.
     *
     * @param context a context reference.
     * @param fileName the chosen file name, SCAN_yyyyMMdd_HHmmss.jpg otherwise.
     */
    fun createFullSizeImageFile(context: Context, fileName: String? = null): URI? =
            SkannerUtils.createJPGFile(
                    context = context,
                    imageFileName = fileName ?: "SCAN_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())}")

    /**
     * Scan a photo and detect a document inside it.
     * If the image is too big to be processed in memory it creates a scaled version and scans it.
     *
     * @param originalImageURI the URI of the image to be scanned.
     * @param context a [Context] reference.
     */
    fun scanDocument(originalImageURI: URI, context: Context): Scan? {
        Log.d("Skanner", "scanDocument: Start document scanning: ${originalImageURI.path}")

        val originalWidth = originalImageURI.detectBitmapDimension()?.width ?: return null

        val maxWidth = context.resources.displayMetrics.let { it.widthPixels * it.density } * 2

        val scan = if (originalWidth > maxWidth) {
            Log.d("Skanner", "scanDocument: The original bitmap is too big, using a scaled version (${maxWidth / originalWidth})")
            val targetURI = SkannerUtils.createJPGFile(context, File(originalImageURI).fileNameWith(suffix = "_SCALED")) ?: return null
            val scaledBitmap = originalImageURI.loadScaledBitmap(maxWidth.toInt())
            scaledBitmap?.saveImage(targetURI) to targetURI
        } else {
            Log.d("Skanner", "scanDocument: Using the original image")
            loadBitmap(originalImageURI) to originalImageURI
        }.let { (bitmap, uri): Pair<Bitmap?, URI> ->
            bitmap?.detectRectangle()?.buildScan(uri).also { bitmap?.recycle() }
        }
        Log.d("Skanner", "scanDocument: Scan done $scan")
        return scan
    }

    /**
     * Correct the perspective of a [Scan]. It returns the URI of the produced
     * image file or null if there was some problem.
     *
     * @param scan the source [Scan].
     * @param context a [Context] reference.
     */
    fun correctPerspective(scan: Scan): Bitmap? =
            loadBitmap(scan.scannedImageURI)?.correctPerspective(scan.detectedRectangle)

    /**
     * Add a grayScale filter to the image.
     */
    fun makeGrayScale(image: Bitmap): Bitmap? = image.grayScale()

    /**
     * Add a black and white filter to the image.
     */
    fun makeBlackAndWhite(image: Bitmap): Bitmap? = image.blackAndWhite()
}
