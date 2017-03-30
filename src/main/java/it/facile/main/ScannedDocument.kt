package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*


data class ScannedDocument(val originalImagePath: String, val transformedImagePath: String, val detectedRectangle: Rectangle) : Serializable

fun scanDocument(originalImagePath: String, context: Context, sampleSize: Int = 2): ScannedDocument {

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

    return ScannedDocument(originalImagePath, transformedFile.absolutePath, detectedRectangle.scale( options.inSampleSize.toFloat()))
}

fun createOriginalImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
    val imageFileName = "SCAN_$timeStamp"
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val image = File.createTempFile(imageFileName, ".jpg", storageDir)
    return image
}
