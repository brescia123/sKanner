package it.facile.main

import android.content.Context
import android.os.Environment
import android.util.Log
import org.opencv.android.OpenCVLoader
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

object Skanner {

    /**
     * Function needed to init OpenCV.
     */
    fun init() {
        if (OpenCVLoader.initDebug()) Log.i("Skanner", "OpenCV initialize success")
        else Log.i("Skanner", "OpenCV initialize failed")
    }

    /** Pixel dimension of a PDF page at 72 dpi */
    private val pdfDimensions = 595 widthTo 842

    /**
     * Convenient function used to create a file inside [Environment.DIRECTORY_PICTURES] with the
     * following name scheme: SCAN_yyyyMMdd_HHmmss.jpg . It returns the URI of the created file,
     * null if there was a problem.
     *
     * @param context a context reference.
     */
    fun createFullSizeImageFile(context: Context): URI? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
        val imageFileName = "SCAN_$timeStamp"
        return SkannerUtils.createJPGFile(context, imageFileName)
    }

    /**
     * Function that allows to scan a photo and detect a document inside it.
     *
     * @param originalImageURI the path of the image to be scanned.
     */
    fun scanDocument(originalImageURI: URI, context: Context): Scan? {
        val scaledImageURI: URI = SkannerUtils.createJPGFile(
                context = context,
                imageFileName = File(originalImageURI).fileNameWith(suffix = "_SCALED")) ?: return null


        val originalImageDimensions = originalImageURI.detectBitmapDimension()
        val dstImageDimensions = if (originalImageDimensions.isHorizontal()) pdfDimensions.rotate() else pdfDimensions
        return loadScaledBitmap(
                imageURI = originalImageURI,
                sampleSize = calculateInSampleSize(originalImageDimensions, dstImageDimensions))
                ?.saveImage(scaledImageURI)
                ?.detectRectangle()
                ?.buildScan(scaledImageURI)
    }

    /**
     * Function that allows to correct the perspective of a [Scan]. It returns the URI of the produced
     * image file or null if there was some problem.
     *
     * @param scan the source [Scan].
     * @param context a [Context] reference.
     */
    fun correctPerspective(scan: Scan, context: Context): URI? {
        val correctedImageURI: URI = SkannerUtils.createJPGFile(
                context = context,
                imageFileName = File(scan.scannedImageURI).fileNameWith(suffix = "_CORRECTED")) ?: return null
        val savedBitmap = loadBitmap(scan.scannedImageURI)
                ?.correctPerspective(scan.detectedRectangle)
                ?.saveImage(correctedImageURI)

        return if (savedBitmap != null) correctedImageURI else null
    }
}