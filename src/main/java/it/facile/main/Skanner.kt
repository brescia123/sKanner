package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

class Skanner(val config: Config) {

    init {
        if (OpenCVLoader.initDebug()) {
            Log.i("Skanner", "OpenCV initialize success")
        } else {
            Log.i("Skanner", "OpenCV initialize failed")
        }
    }

    companion object {

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
            return createJPGFile(context, imageFileName)
        }

        /** Create a JPG file within Environment.DIRECTORY_PICTURES with the given name and return its URI,
         * null if there was a problem. */
        private fun createJPGFile(context: Context, imageFileName: String): URI? {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir == null)
                Log.e(this::class.java.simpleName, "Error while trying to getExternalFilesDir Environment.DIRECTORY_PICTURES")
            try {
                return File.createTempFile(imageFileName, ".jpg", storageDir).toURI()
            } catch (e: Exception) {
                Log.e(this::class.java.simpleName, "Error while trying to create file $imageFileName within Environment.DIRECTORY_PICTURES", e)
                return null
            }
        }
    }

    /**
     * Function that allows to scan a photo and detect a document inside it.
     *
     * @param originalImageURI the path of the image to be scanned.
     */
    fun scanDocument(originalImageURI: URI, context: Context): Scan? {
        val scaledImageURI: URI = createJPGFile(
                context = context,
                imageFileName = File(originalImageURI).fileNameWith(suffix = "_SCALED")) ?: return null

        return loadScaledBitmap(originalImageURI)
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
        val correctedImageURI: URI = createJPGFile(
                context = context,
                imageFileName = File(scan.originalImageURI).fileNameWith(suffix = "_CORRECTED")) ?: return null
        val savedBitmap = loadScaledBitmap(scan.originalImageURI)
                ?.correctPerspective(scan.detectedRectangle)
                ?.saveImage(correctedImageURI)

        return if (savedBitmap != null) correctedImageURI else null
    }

    /** Save the Bitmap to the given file. Return the saved Bitmap, null if there was a problem. */
    private fun Bitmap.saveImage(fileURI: URI): Bitmap? {
        try {
            val fos = FileOutputStream(File(fileURI))
            val compressSuccessful = compress(Bitmap.CompressFormat.JPEG, 100, fos)
            if (compressSuccessful.not()) Log.e(this::class.java.simpleName, "Error while trying to compress to file ($fileURI) a Bitmap")
            fos.close()
            return this
        } catch (e: Exception) {
            Log.e(this::class.java.simpleName, "Error while trying to save to file ($fileURI) a Bitmap", e)
            return null
        }
    }


    private fun Rectangle.buildScan(imageURI: URI) = Scan(imageURI, this)

    /** Load and return a Bitmap scaled using [Config.scaleFactor], null if there was a problem. */
    private fun loadScaledBitmap(imageURI: URI): Bitmap? = BitmapFactory
            .decodeFile(
                    imageURI.path,
                    BitmapFactory.Options().apply { inSampleSize = config.scaleFactor })
            .also {
                if (it == null) Log.e(this::class.java.simpleName, "Error while trying to load a scaled Bitmap from $imageURI")
            }


    /** Append the given suffix to the file name without extension. */
    private fun File.fileNameWith(suffix: String): String = with(this) {
        return "$nameWithoutExtension$suffix"
    }

    /**
     * Configuration class for the Skanner
     *
     * @property scaleFactor Used to decode the original image file into a scaled Bitmap
     * ([BitmapFactory.Options.inSampleSize]) (default = 1).
     */
    data class Config(val scaleFactor: IntPowerOf2 = IntPowerOf2(1)) : Serializable

    data class IntPowerOf2(val value: Int){
        init {
            if ((value and value - 1 == 0).not() || value <= 0)
                throw IllegalArgumentException("value should be a power of 2 and positive")
        }
    }
}