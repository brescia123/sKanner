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
}


/** Save the Bitmap to the given file. Return the saved Bitmap, null if there was a problem. */
fun Bitmap.saveImage(fileURI: URI, quality: Int = 100): Bitmap? {
    try {
        FileOutputStream(File(fileURI)).use {
            val compressSuccessful = compress(Bitmap.CompressFormat.JPEG, quality, it)
            if (compressSuccessful.not()) Log.e(TAG, "Error while trying to compress to file ($fileURI) a Bitmap")
        }
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

/** Load and return a Bitmap at full scale. */
internal fun loadBitmap(imageURI: URI): Bitmap? = BitmapFactory
        .decodeFile(imageURI.path)
        .also {
            if (it == null) Log.e(TAG, "Error while trying to load a scaled Bitmap from $imageURI")
        }

internal fun URI.loadScaledBitmap(targetWidth: Int): Bitmap? {
    val (srcWidth, _) = this.detectBitmapDimension() ?: return null

    return BitmapFactory.Options()
            .apply {
                inScaled = true
                inDensity = srcWidth
                inTargetDensity = targetWidth
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            .let { BitmapFactory.decodeFile(path, it) }
}

/** Convenient class to represent dimensions of a bitmap as width and height. */
internal data class BitmapDimensions(val width: Int, val height: Int) {
    fun isHorizontal() = width > height
    fun rotate() = height widthTo width
}

internal infix fun Int.widthTo(height: Int) = BitmapDimensions(this, height)

/** Load the image the given URI and detect its dimension. */
internal fun URI.detectBitmapDimension(): BitmapDimensions? {
    // Detect only bounds
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    if (options.outWidth < 0 || options.outHeight < 0) {
        logError("Impossible to detect dimensions of ${this.path}")
        return null
    }
    return options.outWidth widthTo options.outHeight
}

/** Detect Bitmap dimension. */
internal fun Bitmap.detectBitmapDimension(): BitmapDimensions = width widthTo height

internal inline fun <reified T> T.logError(msg: String) {
    Log.e(T::class.java.simpleName, msg)
}