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

data class Skanner(val config: Config) {

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
         * following name scheme: SCAN_yyyyMMdd_HHmmss.jpg
         *
         * @param context a context reference.
         */
        fun createFullSizeImageFile(context: Context): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
            val imageFileName = "SCAN_$timeStamp"
            return createFile(context, imageFileName)
        }

        /**
         * Convenient function used to create a file inside [Environment.DIRECTORY_PICTURES] with the
         * following name scheme: [[originalImageURI]]_SCALED.jpg
         *
         * @param context a context reference.
         * @param originalImageURI the original image URI.
         */
        fun createScaledImageFile(originalImageURI: URI, context: Context): File =
                createFile(context, originalImageURI.fileNameWith(suffix = "_SCALED"))

        /**
         * Convenient function used to create a file inside [Environment.DIRECTORY_PICTURES] with the
         * following name scheme: [[originalImageURI]]_CORRECTED.jpg
         *
         * @param context a context reference.
         * @param originalImageURI the original image URI.
         */
        fun createCorrectedImageFile(originalImageURI: URI, context: Context): File =
                createFile(context, originalImageURI.fileNameWith(suffix = "_CORRECTED"))

        private fun createFile(context: Context, imageFileName: String): File {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val fullSizeImage = File.createTempFile(imageFileName, ".jpg", storageDir)
            return fullSizeImage
        }

        private fun URI.fileNameWith(suffix: String): String = with(File(this)) {
            return "$nameWithoutExtension$suffix"
        }
    }

    /**
     * Function that allows to scan a photo and detect a document inside it.
     *
     * @param originalImageURI the path of the image to be scanned.
     */
    fun scanDocument(originalImageURI: URI, originalScaledImageURI: URI): Scan {

        return loadScaledBitmap(originalImageURI)
                .saveImage(File(originalScaledImageURI))
                .detectRectangle()
                .buildScan(originalScaledImageURI)
    }

    /**
     * Function that allows to correct a [Scan] and obtain the transformed Bitmap.
     */
    fun correctPerspective(scan: Scan, correctedImageURI: URI): URI {
        loadScaledBitmap(scan.originalImageURI)
                .correctPerspective(scan.detectedRectangle)
                .saveImage(File(correctedImageURI))

        return correctedImageURI
    }


    private fun Bitmap.saveImage(file: File): Bitmap {
        val fos = FileOutputStream(file)
        compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.close()
        return this
    }

    private fun Rectangle.buildScan(imageURI: URI) = Scan(imageURI, this)

    private fun loadScaledBitmap(imageURI: URI): Bitmap =
            BitmapFactory.decodeFile(
                    imageURI.path,
                    BitmapFactory.Options().apply { inSampleSize = config.scaleFactor })

    /**
     * Configuration class for the Skanner
     *
     * @property scaleFactor Used to decode the original image file into a scaled Bitmap
     * ([BitmapFactory.Options.inSampleSize]) (default = 1).
     */
    data class Config(val scaleFactor: Int = 1) : Serializable
}