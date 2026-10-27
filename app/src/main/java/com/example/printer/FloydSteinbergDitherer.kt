package com.example.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object FloydSteinbergDitherer {

    const val DEFAULT_PRINTER_WIDTH_DOTS = 384 // Standard 58mm thermal printer width

    /**
     * Applies Floyd-Steinberg error diffusion dithering to a source bitmap and returns
     * an ESC/POS raster bit image command payload (GS v 0).
     */
    fun processBitmapToEscPosRaster(
        sourceBitmap: Bitmap,
        printerWidthDots: Int = DEFAULT_PRINTER_WIDTH_DOTS
    ): ByteArray {
        val scaledAndCentered = prepareCenteredBitmap(sourceBitmap, printerWidthDots)
        val width = scaledAndCentered.width
        val height = scaledAndCentered.height

        val gray = Array(height) { FloatArray(width) }
        val pixels = IntArray(width * height)
        scaledAndCentered.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixels[y * width + x]
                val alpha = (color shr 24) and 0xFF
                if (alpha < 128) {
                    // Transparent pixels treated as white
                    gray[y][x] = 255f
                } else {
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    gray[y][x] = (0.299f * r + 0.587f * g + 0.114f * b)
                }
            }
        }

        // Apply Floyd-Steinberg Dithering
        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldPixel = gray[y][x].coerceIn(0f, 255f)
                val newPixel = if (oldPixel < 128f) 0f else 255f
                gray[y][x] = newPixel
                val err = oldPixel - newPixel

                if (x + 1 < width) {
                    gray[y][x + 1] += err * (7f / 16f)
                }
                if (y + 1 < height) {
                    if (x - 1 >= 0) {
                        gray[y + 1][x - 1] += err * (3f / 16f)
                    }
                    gray[y + 1][x] += err * (5f / 16f)
                    if (x + 1 < width) {
                        gray[y + 1][x + 1] += err * (1f / 16f)
                    }
                }
            }
        }

        // Convert 0/255 dithered array into ESC/POS 1-bit raster data
        val widthBytes = ceil(width / 8.0).toInt()
        val bitmapBytes = ByteArray(widthBytes * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (gray[y][x] == 0f) { // 0 = Black dot (print)
                    val byteIndex = y * widthBytes + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    bitmapBytes[byteIndex] = (bitmapBytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }

        val out = ByteArrayOutputStream()
        // ESC/POS GS v 0 command: 0x1D 0x76 0x30 0x00 xL xH yL yH
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (height and 0xFF).toByte()
        val yH = ((height shr 8) and 0xFF).toByte()

        out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))
        out.write(bitmapBytes)
        return out.toByteArray()
    }

    /**
     * Generates a preview bitmap showing the exact Floyd-Steinberg dithered monochrome result.
     */
    fun createDitheredPreview(
        sourceBitmap: Bitmap,
        previewWidth: Int = 384
    ): Bitmap {
        val scaledAndCentered = prepareCenteredBitmap(sourceBitmap, previewWidth)
        val width = scaledAndCentered.width
        val height = scaledAndCentered.height

        val gray = Array(height) { FloatArray(width) }
        val pixels = IntArray(width * height)
        scaledAndCentered.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixels[y * width + x]
                val alpha = (color shr 24) and 0xFF
                if (alpha < 128) {
                    gray[y][x] = 255f
                } else {
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    gray[y][x] = (0.299f * r + 0.587f * g + 0.114f * b)
                }
            }
        }

        val resultPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldPixel = gray[y][x].coerceIn(0f, 255f)
                val newPixel = if (oldPixel < 128f) 0f else 255f
                gray[y][x] = newPixel
                val err = oldPixel - newPixel

                resultPixels[y * width + x] = if (newPixel == 0f) Color.BLACK else Color.WHITE

                if (x + 1 < width) gray[y][x + 1] += err * (7f / 16f)
                if (y + 1 < height) {
                    if (x - 1 >= 0) gray[y + 1][x - 1] += err * (3f / 16f)
                    gray[y + 1][x] += err * (5f / 16f)
                    if (x + 1 < width) gray[y + 1][x + 1] += err * (1f / 16f)
                }
            }
        }

        val preview = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        preview.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return preview
    }

    private fun prepareCenteredBitmap(source: Bitmap, targetWidth: Int): Bitmap {
        var srcW = source.width
        var srcH = source.height

        val maxAllowedHeight = 180
        val scale = min(targetWidth.toFloat() / srcW.toFloat(), maxAllowedHeight.toFloat() / srcH.toFloat())
        val finalW = (srcW * scale).toInt().coerceAtLeast(1)
        val finalH = (srcH * scale).toInt().coerceAtLeast(1)

        val output = Bitmap.createBitmap(targetWidth, finalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)

        val xOffset = (targetWidth - finalW) / 2
        val srcRect = Rect(0, 0, srcW, srcH)
        val dstRect = Rect(xOffset, 0, xOffset + finalW, finalH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, srcRect, dstRect, paint)

        return output
    }
}
