package dev.nalamzap.comig.core.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import dev.nalamzap.comig.R
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    fun shareComicPage(
        context: Context,
        pageBitmap: Bitmap,
        caption: String?
    ) {
        val width = pageBitmap.width
        val density = context.resources.displayMetrics.density

        // User adjusted header, logo, and margin sizing
        val headerHeight = (width * 0.18f).coerceAtLeast(80f * density).toInt()
        val logoSize = (headerHeight * 0.45f).toInt()
        val logoMargin = (headerHeight * 0.22f).toInt()

        val totalContentWidth = width
        val totalContentHeight = headerHeight + pageBitmap.height

        // Calculate 2:3 aspect ratio canvas dimensions (Width : Height = 2 : 3 -> Ratio = 1.5)
        val targetRatio = 1.5f
        val currentRatio = totalContentHeight.toFloat() / totalContentWidth.toFloat()

        val canvasWidth: Int
        val canvasHeight: Int

        if (currentRatio < targetRatio) {
            // Content is wider than 2:3 -> add top/bottom padding
            canvasWidth = totalContentWidth
            canvasHeight = (totalContentWidth * targetRatio).toInt()
        } else {
            // Content is taller than 2:3 -> add left/right padding
            canvasHeight = totalContentHeight
            canvasWidth = (totalContentHeight / targetRatio).toInt()
        }

        val compositeBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)

        // 1. Fill Entire Canvas with Soothing Light Blue Background (#EBF4FA)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#EBF4FA")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        // Calculate centering offsets for content within the 2:3 canvas frame
        val offsetX = ((canvasWidth - totalContentWidth) / 2f).coerceAtLeast(0f)
        val offsetY = ((canvasHeight - totalContentHeight) / 2f).coerceAtLeast(0f)

        // 2. Draw Header Background (Soft Light Powder Blue #D8E6F5)
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#D8E6F5")
            style = Paint.Style.FILL
        }
        val headerTop = offsetY
        val headerBottom = offsetY + headerHeight
        canvas.drawRect(offsetX, headerTop, offsetX + totalContentWidth, headerBottom, headerBgPaint)

        // 3. Draw Accent Line at bottom of header (Soft Ocean Blue #3B82F6)
        val accentPaint = Paint().apply {
            color = Color.parseColor("#3B82F6")
            style = Paint.Style.FILL
        }
        canvas.drawRect(offsetX, headerBottom - 4f, offsetX + totalContentWidth, headerBottom, accentPaint)

        // 4. Draw App Logo at Top-Left of Header
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo)
        if (logoBitmap != null) {
            val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
            canvas.drawBitmap(scaledLogo, offsetX + logoMargin, headerTop + logoMargin, null)
        }

        // 5. Draw App Title "ComiG Manga" (Dark Slate Blue #0F172A)
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = (headerHeight * 0.22f).coerceAtLeast(16f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titleX = offsetX + logoMargin + logoSize + (logoMargin * 0.6f)
        val titleY = headerTop + logoMargin + (logoSize * 0.65f)
        canvas.drawText("ComiG Manga", titleX, titleY, titlePaint)

        // 6. Draw Optional Caption
        if (!caption.isNullOrBlank()) {
            val captionPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = (headerHeight * 0.16f).coerceAtLeast(12f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            val captionY = titleY + (headerHeight * 0.22f)
            val maxCaptionWidth = offsetX + totalContentWidth - titleX - logoMargin
            var displayCaption = caption.trim()
            if (captionPaint.measureText(displayCaption) > maxCaptionWidth) {
                while (displayCaption.length > 3 && captionPaint.measureText("$displayCaption...") > maxCaptionWidth) {
                    displayCaption = displayCaption.substring(0, displayCaption.length - 1)
                }
                displayCaption = "$displayCaption..."
            }
            canvas.drawText(displayCaption, titleX, captionY, captionPaint)
        }

        // 7. Draw Comic Page Bitmap centered below Header
        val pageTop = headerBottom
        canvas.drawBitmap(pageBitmap, offsetX, pageTop, null)

        // 8. Save to Cache and Launch Android Share Intent
        try {
            val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val shareFile = File(shareDir, "comic_page_${System.currentTimeMillis()}.jpg")
            FileOutputStream(shareFile).use { out ->
                compositeBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val imageUri = FileProvider.getUriForFile(context, authority, shareFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!caption.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share Comic Page").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
