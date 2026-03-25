package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

enum class MemoBlockType {
    Text,
    Image,
    Drawing
}

data class StrokePoint(
    val x: Float,
    val y: Float
)

data class StrokeData(
    val points: List<StrokePoint>,
    val color: Int = TextStyleSetting.DEFAULT_LIGHT_TEXT_COLOR,
    val width: Float = 4f
)

data class MemoBlock(
    val id: String,
    val type: MemoBlockType,
    val normalizedX: Float,
    val normalizedY: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val contentAspectRatio: Float?,
    val text: String,
    val animationStyle: AnimationStyle,
    val textStyle: TextStyleSetting,
    val imageUri: String?,
    val strokes: List<StrokeData>
) {
    val isText: Boolean
        get() = type == MemoBlockType.Text

    val isImage: Boolean
        get() = type == MemoBlockType.Image

    val isDrawing: Boolean
        get() = type == MemoBlockType.Drawing

    companion object {
        fun createText(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.35f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Text,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.52f,
                heightFraction = 0.12f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(),
                imageUri = null,
                strokes = emptyList()
            )
        }

        fun createImage(
            imageUri: String,
            contentAspectRatio: Float,
            animationStyle: AnimationStyle = AnimationStyle.None,
            x: Float = 0.5f,
            y: Float = 0.45f
        ): MemoBlock {
            val safeAspectRatio = contentAspectRatio.coerceAtLeast(0.2f)
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Image,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.42f,
                heightFraction = 0.24f,
                contentAspectRatio = safeAspectRatio,
                text = "",
                animationStyle = animationStyle,
                textStyle = TextStyleSetting(),
                imageUri = imageUri,
                strokes = emptyList()
            )
        }

        fun createDrawing(
            x: Float,
            y: Float,
            widthFraction: Float,
            heightFraction: Float,
            strokes: List<StrokeData>,
            animationStyle: AnimationStyle = AnimationStyle.None
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Drawing,
                normalizedX = x,
                normalizedY = y,
                widthFraction = widthFraction.coerceIn(0.08f, 0.8f),
                heightFraction = heightFraction.coerceIn(0.08f, 0.8f),
                contentAspectRatio = (widthFraction / heightFraction).takeIf { it.isFinite() && it > 0f }
                    ?: 1f,
                text = "",
                animationStyle = animationStyle,
                textStyle = TextStyleSetting(),
                imageUri = null,
                strokes = strokes
            )
        }
    }
}
