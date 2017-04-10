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
     * Scan a photo and detect a document inside it.
     *
     * @param originalImageURI the path of the image to be scanned.
     */
    fun scanDocument(originalImageURI: URI, context: Context): Scan? {
        val scaledImageURI: URI = SkannerUtils.createJPGFile(
                context = context,
                imageFileName = File(originalImageURI).fileNameWith(suffix = "_SCALED")) ?: return null


        val originalImageDimensions = originalImageURI.detectBitmapDimension()
        val dstImageDimensions = if (originalImageDimensions.isHorizontal()) pdfDimensions.rotate() else pdfDimensions
        val scaledBitmap = loadScaledBitmap(
                imageURI = originalImageURI,
                sampleSize = calculateInSampleSize(originalImageDimensions, dstImageDimensions))
        val scan = scaledBitmap?.saveImage(scaledImageURI)
                ?.detectRectangle()
                ?.buildScan(scaledImageURI)
        scaledBitmap?.recycle()
        return scan
    }

    /**
     * Correct the perspective of a [Scan]. It returns the URI of the produced
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

        val everythingOk = savedBitmap != null
        savedBitmap?.recycle()

        return if (everythingOk) correctedImageURI else null
    }

    /**
     * Add a grayScale filter to the image.
     */
    fun makeGrayScale(imageURI: URI): URI? {
        val bitmap = loadBitmap(imageURI) ?: return null

        val grayBitmap = bitmap.grayScaleCV()
        grayBitmap.saveImage(imageURI)

        grayBitmap.recycle()
        bitmap.recycle()

        return imageURI
    }
}

/**
 * Scan a photo and detect a document inside it.
 *
 * @param context context used to access android app folder.
 */
fun URI.scanDocument(context: Context): Scan? = Skanner.scanDocument(this, context)

/**
 * Correct the perspective of a [Scan]. It returns the URI of the produced
 * image file or null if there was some problem.
 *
 * @param context context used to access android app folder.
 */
fun Scan.correctPerspective(context: Context): URI? = Skanner.correctPerspective(this, context)

/**
 * Add a grayScale filter to the image.
 */
fun URI.makeGrayScale(): URI? = Skanner.makeGrayScale(this)
