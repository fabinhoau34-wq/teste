package com.example.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object WatermarkUtil {

    fun processAndSaveWatermarkedImage(
        context: Context,
        sourceUri: Uri,
        location: String
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // Create mutable bitmap
            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val width = mutableBitmap.width
            val height = mutableBitmap.height

            // Calculate scaled sizes relative to image dimensions
            val baseSize = width.coerceAtMost(height)
            val textSize = (baseSize * 0.035f).coerceAtLeast(24f)
            val padding = (baseSize * 0.02f).coerceAtLeast(16f)

            val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setTextSize(textSize)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val currentDateTime = dateFormat.format(Date())

            val locText = "Localidade: ${location.ifBlank { "Não informada" }}"
            val dateText = "Data/Hora: $currentDateTime"

            // Measure bounds for background box
            val boundsLoc = Rect()
            val boundsDate = Rect()
            paintText.getTextBounds(locText, 0, locText.length, boundsLoc)
            paintText.getTextBounds(dateText, 0, dateText.length, boundsDate)

            val maxTextWidth = boundsLoc.width().coerceAtLeast(boundsDate.width())
            val lineSpacing = textSize * 0.4f
            val totalTextHeight = boundsLoc.height() + boundsDate.height() + lineSpacing

            val boxRect = RectF(
                padding,
                height - padding - totalTextHeight - padding * 2,
                padding + maxTextWidth + padding * 2,
                height - padding
            )

            // Draw dark semi-transparent rectangle behind text
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CC121214") // Photoshop dark banner
                style = Paint.Style.FILL
            }

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFFF6B00") // Orange border accent
                style = Paint.Style.STROKE
                strokeWidth = (baseSize * 0.005f).coerceAtLeast(3f)
            }

            canvas.drawRoundRect(boxRect, 16f, 16f, bgPaint)
            canvas.drawRoundRect(boxRect, 16f, 16f, strokePaint)

            // Draw text lines
            val x = boxRect.left + padding
            val yLine1 = boxRect.top + padding + boundsLoc.height()
            val yLine2 = yLine1 + lineSpacing + boundsDate.height()

            canvas.drawText(locText, x, yLine1, paintText)
            canvas.drawText(dateText, x, yLine2, paintText)

            // Save to internal storage
            val photosDir = File(context.filesDir, "inspection_photos").apply { if (!exists()) mkdirs() }
            val outputFile = File(photosDir, "IMG_${System.currentTimeMillis()}.jpg")

            FileOutputStream(outputFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
