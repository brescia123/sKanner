package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import java.net.URI

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
fun Scan.correctPerspective(): Bitmap? = Skanner.correctPerspective(this)

/**
 * Add a grayScale filter to the image.
 */
fun Bitmap.makeGrayScale(): Bitmap? = Skanner.makeGrayScale(this)

/**
 * Add a black and white filter to the image.
 */
fun Bitmap.makeBlackAndWhite(): Bitmap? = Skanner.makeBlackAndWhite(this)