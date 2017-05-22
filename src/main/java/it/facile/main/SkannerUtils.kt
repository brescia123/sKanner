package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES31
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

fun Bitmap.rotate90(): Bitmap? {
    val matrix = Matrix()
    matrix.preRotate(90f)
    val rotatedBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    this.recycle()
    Log.d("Rotate90", "${rotatedBitmap.detectBitmapDimension()}")
    return rotatedBitmap
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

internal fun Rectangle.rotate90(): Rectangle = copy(p1 = p4, p2 = p1, p3 = p2, p4 = p3)

enum class Rotation(val degree: Int) {
    DEGREE_0(0),
    DEGREE_90(90),
    DEGREE_180(180),
    DEGREE_270(270)
}

fun Scan.rotate90(): Scan {
    loadBitmap(scannedImageURI)?.rotate90()?.saveImage(scannedImageURI)?.recycle()
    return copy(detectedRectangle = detectedRectangle.rotate90())
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

internal fun Context.calculateMaxDeviceMaxWidth(): Int {
    // Calculate OpenGL max texture size

    val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    val vers = IntArray(2)
    EGL14.eglInitialize(dpy, vers, 0, vers, 1)
    val configAttr = intArrayOf(EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER, EGL14.EGL_LEVEL, 0, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE)
    val configs = arrayOfNulls<EGLConfig>(1)
    val numConfig = IntArray(1)
    EGL14.eglChooseConfig(dpy, configAttr, 0,
            configs, 0, 1, numConfig, 0)
    if (numConfig[0] == 0) {
        return (resources.displayMetrics.let { it.widthPixels * it.density } * 2).toInt()
    }
    val config = configs[0]
    val surfAttr = intArrayOf(EGL14.EGL_WIDTH, 64, EGL14.EGL_HEIGHT, 64, EGL14.EGL_NONE)
    val surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0)
    val ctxAttrib = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
    val ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0)
    EGL14.eglMakeCurrent(dpy, surf, surf, ctx)

    val maxSizes = IntArray(1)
    GLES31.glGetIntegerv(GLES31.GL_MAX_TEXTURE_SIZE, maxSizes, 0)
    EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT)
    EGL14.eglDestroySurface(dpy, surf)
    EGL14.eglDestroyContext(dpy, ctx)
    EGL14.eglTerminate(dpy)
    val maxSize = maxSizes[0]

    return if (maxSize != 0) maxSize
    else (resources.displayMetrics.let { it.widthPixels * it.density } * 2).toInt()
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