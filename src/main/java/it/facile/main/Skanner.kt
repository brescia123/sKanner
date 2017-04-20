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
     * given fileNmae. If the file name is not provided it create a file with the
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
     * Convenient function used to create a file inside [Environment.DIRECTORY_PICTURES] using the
     * given fileNmae. If the file name is not provided it create a file with the
     * following name scheme: DOCUMENT_yyyyMMdd_HHmmss.jpg . It returns the URI of the created file,
     * null if there was a problem.
     *
     * @param context a context reference.
     * @param fileName the chosen file name, DOCUMENT_yyyyMMdd_HHmmss.jpg otherwise.
     */
    fun createDocumentPdfFile(context: Context, fileName: String? = null): URI? =
            SkannerUtils.createPdfFile(
                    context = context,
                    pdfFileName = fileName ?: "DOCUMENT_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())}")

    /**
     * Scan a photo and detect a document inside it.
     *
     * @param originalImageURI the path of the image to be scanned.
     */
    fun scanDocument(originalImageURI: URI, context: Context): Scan? {
        val scaledImageURI: URI = SkannerUtils.createJPGFile(
                context = context,
                imageFileName = File(originalImageURI).fileNameWith(suffix = "_SCALED")) ?: return null

        val originalImageDimensions = originalImageURI.detectBitmapDimension() ?: return null
        Log.d(TAG, "Original image dimensions: $originalImageDimensions")
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
    fun makeGrayScale(imageURI: URI): URI? = loadBitmap(imageURI)?.let { bitmap ->

        val grayBitmap = bitmap.grayScale()
        grayBitmap.saveImage(imageURI)

        grayBitmap.recycle()
        bitmap.recycle()

        return imageURI
    }

    /**
     * Create a PDF.
     */
    fun createPdf(imageURI: URI, destURI: URI): URI? = loadBitmap(imageURI)?.let { bitmap ->
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        page.canvas.drawBitmap(bitmap, 0f, 0f, Paint())

        pdfDocument.finishPage(page)

        // Save to file
        FileOutputStream(File(destURI)).let { fos ->
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            bitmap.recycle()
            destURI
        }
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

/**
 * Create a pdf and save it at the given URI.
 */
fun URI.createPdf(destURI: URI): URI? = Skanner.createPdf(this, destURI)