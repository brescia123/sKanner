package it.facile.main

import java.io.File
import java.io.FileNotFoundException
import java.io.Serializable
import java.net.URI

/**
 * Data class containing the rectangle detected within an image.
 *
 * @property scannedImageURI URI of the image of reference for the [detectedRectangle].
 * @property detectedRectangle detected document rectangle.
 */
data class Scan(val scannedImageURI: URI, val detectedRectangle: Rectangle) : Serializable {
    init {
        if (File(scannedImageURI).exists().not()) throw FileNotFoundException()
    }
}

