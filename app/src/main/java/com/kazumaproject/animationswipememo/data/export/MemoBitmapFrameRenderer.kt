package com.kazumaproject.animationswipememo.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.kazumaproject.animationswipememo.domain.animation.MemoAnimationEngine
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoTextAlign
import kotlin.math.max

class MemoBitmapFrameRenderer(
    private val context: Context
) {
    private val imageCache = mutableMapOf<String, Bitmap?>()

    fun renderFrame(
        memo: MemoDraft,
        width: Int,
        height: Int,
        progress: Float,
        darkTheme: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paper = memo.paperStyle.palette(darkTheme)

        canvas.drawColor(paper.backdropArgb)

        val cardMarginX = width * 0.05f
        val cardTop = height * 0.05f
        val cardBottom = height * 0.95f
        val cardRect = RectF(cardMarginX, cardTop, width - cardMarginX, cardBottom)
        val shadowRect = RectF(cardRect).apply {
            offset(width * 0.015f, height * 0.012f)
        }
        val radius = width * 0.045f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paper.shadowArgb }
        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paper.paperArgb }
        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paper.edgeArgb
            style = Paint.Style.STROKE
            strokeWidth = width * 0.004f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paper.lineArgb
            strokeWidth = max(1f, width * 0.0018f)
        }
        val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paper.accentArgb
            alpha = 170
        }

        canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)
        canvas.drawRoundRect(cardRect, radius, radius, paperPaint)
        canvas.drawRoundRect(cardRect, radius, radius, edgePaint)

        val tapeWidth = cardRect.width() * 0.28f
        val tapeHeight = cardRect.height() * 0.045f
        canvas.drawRoundRect(
            RectF(
                cardRect.centerX() - tapeWidth / 2f,
                cardRect.top + cardRect.height() * 0.015f,
                cardRect.centerX() + tapeWidth / 2f,
                cardRect.top + cardRect.height() * 0.015f + tapeHeight
            ),
            tapeHeight * 0.35f,
            tapeHeight * 0.35f,
            tapePaint
        )

        var y = cardRect.top + cardRect.height() * 0.16f
        val lineSpacing = cardRect.height() * 0.085f
        while (y < cardRect.bottom - cardRect.height() * 0.06f) {
            canvas.drawLine(
                cardRect.left + cardRect.width() * 0.08f,
                y,
                cardRect.right - cardRect.width() * 0.08f,
                y,
                linePaint
            )
            y += lineSpacing
        }

        memo.blocks.forEach { block ->
            when (block.type) {
                MemoBlockType.Text -> drawTextBlock(canvas, cardRect, block, progress, darkTheme)
                MemoBlockType.Image -> drawImageBlock(canvas, cardRect, block)
                MemoBlockType.Drawing -> drawDrawingBlock(canvas, cardRect, block)
            }
        }

        return bitmap
    }

    private fun drawTextBlock(
        canvas: Canvas,
        cardRect: RectF,
        block: MemoBlock,
        progress: Float,
        darkTheme: Boolean
    ) {
        val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
        val displayedText = frame.displayedText(block.text.ifBlank { "Text" })
        val availableWidth = (cardRect.width() * block.widthFraction).toInt().coerceAtLeast(120)
        val resolvedTextColor = block.textStyle.resolvedTextColor(darkTheme)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolvedTextColor
            alpha = (frame.alpha.coerceIn(0f, 1f) * 255f).toInt()
            textSize = (cardRect.width() * 0.067f) * (block.textStyle.fontSize / 28f)
            isSubpixelText = true
            if (frame.glowRadiusPx > 0f) {
                setShadowLayer(
                    frame.glowRadiusPx * (cardRect.width() / 480f),
                    0f,
                    0f,
                    resolvedTextColor
                )
            }
        }
        val alignment = when (block.textStyle.textAlign) {
            MemoTextAlign.Start -> Layout.Alignment.ALIGN_NORMAL
            MemoTextAlign.Center -> Layout.Alignment.ALIGN_CENTER
            MemoTextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val layout = StaticLayout.Builder
            .obtain(displayedText, 0, displayedText.length, textPaint, availableWidth)
            .setAlignment(alignment)
            .setLineSpacing(cardRect.width() * 0.008f, 1.08f)
            .setIncludePad(false)
            .build()

        val centerX = cardRect.left + block.normalizedX.coerceIn(0.08f, 0.92f) * cardRect.width()
        val centerY = cardRect.top + block.normalizedY.coerceIn(0.12f, 0.9f) * cardRect.height()
        val textX = centerX - (availableWidth / 2f)
        val textY = centerY - (layout.height / 2f)

        canvas.save()
        canvas.translate(textX + frame.offsetXPx, textY + frame.offsetYPx)
        canvas.scale(frame.scale, frame.scale, availableWidth / 2f, layout.height / 2f)
        canvas.rotate(frame.rotationDeg, availableWidth / 2f, layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            textPaint.clearShadowLayer()
        }
    }

    private fun drawImageBlock(
        canvas: Canvas,
        cardRect: RectF,
        block: MemoBlock
    ) {
        val imageUri = block.imageUri ?: return
        val bitmap = loadBitmap(imageUri) ?: return
        val width = cardRect.width() * block.widthFraction
        val height = cardRect.height() * block.heightFraction
        val cx = cardRect.left + block.normalizedX * cardRect.width()
        val cy = cardRect.top + block.normalizedY * cardRect.height()
        val dst = RectF(
            cx - width / 2f,
            cy - height / 2f,
            cx + width / 2f,
            cy + height / 2f
        )
        canvas.drawBitmap(bitmap, null, dst, null)
    }

    private fun drawDrawingBlock(
        canvas: Canvas,
        cardRect: RectF,
        block: MemoBlock
    ) {
        val left = cardRect.left + (block.normalizedX - block.widthFraction / 2f) * cardRect.width()
        val top = cardRect.top + (block.normalizedY - block.heightFraction / 2f) * cardRect.height()
        val width = cardRect.width() * block.widthFraction
        val height = cardRect.height() * block.heightFraction

        block.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                val x = left + point.x * width
                val y = top + point.y * height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.color
                style = Paint.Style.STROKE
                strokeWidth = stroke.width
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun loadBitmap(imageUri: String): Bitmap? {
        return imageCache.getOrPut(imageUri) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(imageUri)).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
        }
    }
}
