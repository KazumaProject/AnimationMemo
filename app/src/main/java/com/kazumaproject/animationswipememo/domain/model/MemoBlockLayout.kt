package com.kazumaproject.animationswipememo.domain.model

data class FittedContentSize(
    val width: Float,
    val height: Float
)

fun MemoBlock.resolvedContentAspectRatio(): Float {
    return contentAspectRatio?.takeIf { it.isFinite() && it > 0f }
        ?: ((widthFraction / heightFraction).takeIf { it.isFinite() && it > 0f } ?: 1f)
}

fun fitContentSize(
    boxWidth: Float,
    boxHeight: Float,
    aspectRatio: Float
): FittedContentSize {
    val safeAspectRatio = aspectRatio.coerceAtLeast(0.2f)
    val widthFromHeight = boxHeight * safeAspectRatio
    return if (widthFromHeight <= boxWidth) {
        FittedContentSize(
            width = widthFromHeight,
            height = boxHeight
        )
    } else {
        FittedContentSize(
            width = boxWidth,
            height = boxWidth / safeAspectRatio
        )
    }
}
