package it.facile.skanner.main

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object Skanner {
    fun detectEdges(bitmap: Bitmap): Bitmap {
        val colorImage = Mat()
        Utils.bitmapToMat(bitmap, colorImage)

        val edges = Mat(colorImage.size(), CvType.CV_8UC1)
        Imgproc.cvtColor(colorImage, edges, Imgproc.COLOR_RGB2GRAY, 4)
        Imgproc.Canny(edges, edges, 80.0, 100.0)

        val resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(edges, resultBitmap)
        return resultBitmap
    }
}