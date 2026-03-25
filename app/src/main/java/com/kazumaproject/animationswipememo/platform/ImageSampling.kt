package com.kazumaproject.animationswipememo.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun Context.decodeSampledBitmap(
    uri: Uri,
    maxWidth: Int,
    maxHeight: Int
): Bitmap? {
    val safeMaxWidth = max(1, maxWidth)
    val safeMaxHeight = max(1, maxHeight)
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
    }.getOrNull()

    val sourceWidth = bounds.outWidth
    val sourceHeight = bounds.outHeight
    val decodeOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = calculateInSampleSize(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            requestedWidth = safeMaxWidth,
            requestedHeight = safeMaxHeight
        )
    }

    val bitmap = runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }.getOrNull() ?: return null

    return bitmap.downscaleToFit(
        maxWidth = safeMaxWidth,
        maxHeight = safeMaxHeight
    )
}

internal fun calculateInSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    requestedWidth: Int,
    requestedHeight: Int
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0) return 1
    if (sourceWidth <= requestedWidth && sourceHeight <= requestedHeight) return 1

    var inSampleSize = 1
    var halfWidth = sourceWidth / 2
    var halfHeight = sourceHeight / 2

    while (halfWidth / inSampleSize >= requestedWidth && halfHeight / inSampleSize >= requestedHeight) {
        inSampleSize *= 2
    }

    return max(1, inSampleSize)
}

private fun Bitmap.downscaleToFit(
    maxWidth: Int,
    maxHeight: Int
): Bitmap {
    if (width <= maxWidth && height <= maxHeight) return this

    val widthScale = maxWidth.toFloat() / width.toFloat()
    val heightScale = maxHeight.toFloat() / height.toFloat()
    val scale = min(widthScale, heightScale)
    val scaledWidth = max(1, (width * scale).roundToInt())
    val scaledHeight = max(1, (height * scale).roundToInt())

    val scaledBitmap = Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    if (scaledBitmap !== this) {
        recycle()
    }
    return scaledBitmap
}
