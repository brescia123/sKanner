package it.facile.main

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
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

    /** Pixel dimension of a PDF page at 144 dpi */
    private val pdfDimensions = 1190 widthTo 1684

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
     *
     * @param originalImageURI the path of the image to be scanned.
     */
    fun scanDocument(originalImageURI: URI, context: Context): Scan? {
        val scaledImageURI: URI? = SkannerUtils.createJPGFile(
                context = context,
                imageFileName = File(originalImageURI).fileNameWith(suffix = "_SCALED"))

        if (scaledImageURI == null) {
            logError("Impossible to create 'scaledImageURI'")
            return null
        }

        val originalImageDimensions = originalImageURI.detectBitmapDimension() ?: return null


        val dstImageDimensions = if (originalImageDimensions.isHorizontal()) pdfDimensions.rotate() else pdfDimensions
        val scaledBitmap = loadSampledBitmap(
                imageURI = originalImageURI,
                sampleSize = calculateInSampleSize(originalImageDimensions, dstImageDimensions))
        val scan = scaledBitmap?.saveImage(scaledImageURI)
                ?.detectRectangle()
                ?.buildScan(scaledImageURI)
        scaledBitmap?.recycle()

        Log.d(TAG, "scan: $scan")
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
