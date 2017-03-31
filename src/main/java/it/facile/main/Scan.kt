package it.facile.main

import java.io.Serializable
import java.net.URI

/**
 * Data class containing the path of the original and the detected document rectangle.
 *
 * @property originalImageURI URI of the original image.
 * @property detectedRectangle detected document rectangle.
 */
data class Scan(val originalImageURI: URI, val detectedRectangle: Rectangle) : Serializable

