package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URI

private const val TAG = "SkannerUtils"

internal object SkannerUtils {

    /** Create a JPG file within Environment.DIRECTORY_PICTURES with the given name and return its URI,
     * null if there was a problem. */
    internal fun createJPGFile(context: Context, imageFileName: String): URI? {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir == null)
            Log.e(TAG, "Error while trying to getExternalFilesDir Environment.DIRECTORY_PICTURES")
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir).toURI()
        } catch (e: Exception) {
            Log.e(TAG, "Error while trying to create file $imageFileName within Environment.DIRECTORY_PICTURES", e)
            return null
        }
    }
    internal fun createPdfFile(context: Context, pdfFileName: String): URI? {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (storageDir == null)
            Log.e(TAG, "Error while trying to getExternalFilesDir Environment.DIRECTORY_PICTURES")
        try {
            return File.createTempFile(pdfFileName, ".pdf", storageDir).toURI()
        } catch (e: Exception) {
            Log.e(TAG, "Error while trying to create file $pdfFileName within Environment.DIRECTORY_PICTURES", e)
            return null
        }
    }
}


/** Save the Bitmap to the given file. Return the saved Bitmap, null if there was a problem. */
internal fun Bitmap.saveImage(fileURI: URI): Bitmap? {
    try {
        val fos = FileOutputStream(File(fileURI))
        val compressSuccessful = compress(Bitmap.CompressFormat.JPEG, 100, fos)
        if (compressSuccessful.not()) Log.e(TAG, "Error while trying to compress to file ($fileURI) a Bitmap")
        fos.close()
        return this
    } catch (e: Exception) {
        Log.e(TAG, "Error while trying to save to file ($fileURI) a Bitmap", e)
        return null
    }
}

/** Create a [Scan] from a [Rectangle]. */
internal fun Rectangle.buildScan(imageURI: URI) = Scan(imageURI, this)

/** Append the given suffix to the file name without extension. */
internal fun File.fileNameWith(suffix: String): String = with(this) {
    return "$nameWithoutExtension$suffix"
}


/** Load and return a Bitmap scaled using inSampleSize as an option, null if there was a problem. */
internal fun loadSampledBitmap(imageURI: URI, sampleSize: Int): Bitmap? = BitmapFactory
        .decodeFile(
                imageURI.path,
                BitmapFactory.Options().apply { inSampleSize = sampleSize })
        .also {
            if (it == null) Log.e(TAG, "Error while trying to load a scaled Bitmap from $imageURI")
        }

/** Load and return a Bitmap at full scale. */
internal fun loadBitmap(imageURI: URI): Bitmap? = BitmapFactory
        .decodeFile(imageURI.path)
        .also {
            if (it == null) Log.e(TAG, "Error while trying to load a scaled Bitmap from $imageURI")
        }

/** Convenient class to represent dimensions of a bitmap as width and height. */
internal data class BitmapDimensions(val width: Int, val height: Int) {
    fun isHorizontal() = width > height
    fun rotate() = height widthTo width
}

internal infix fun Int.widthTo(height: Int) = BitmapDimensions(this, height)

/** Create a the sample size (a positive int that is a power of 2) to be used to load a large Bitmap
 * into memory. */
internal fun calculateInSampleSize(srcDimensions: BitmapDimensions, reqDimensions: BitmapDimensions): Int {
    // Thanks to https://developer.android.com/topic/performance/graphics/load-bitmap.html

    val (reqHeight, reqWidth) = reqDimensions
    val (srcHeight, srcWidth) = srcDimensions
    var inSampleSize = 1

    if (srcHeight > reqHeight || srcWidth > reqWidth) {

        val halfHeight = srcHeight / 2
        val halfWidth = srcWidth / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // srcHeight and srcWidth larger than the requested srcHeight and srcWidth.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

/** Load the image the given URI and detect its dimension. */
internal fun URI.detectBitmapDimension(): BitmapDimensions {
    // Detect only bounds
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    return options.outWidth widthTo options.outHeight
}

/** Detect Bitmap dimension. */
internal fun Bitmap.detectBitmapDimension(): BitmapDimensions = width widthTo height
