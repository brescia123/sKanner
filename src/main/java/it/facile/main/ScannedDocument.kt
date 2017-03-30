package it.facile.main

import java.io.Serializable

/**
 * Data class containing the path of the original and the transformed images and the detected
 * document rectangle.
 *
 * @property originalImagePath absolute path of the original image.
 * @property transformedImagePath absolute path of the transformed image.
 * @property detectedRectangle detected document rectangle.
 */
data class ScannedDocument(val originalImagePath: String, val transformedImagePath: String, val detectedRectangle: Rectangle) : Serializable