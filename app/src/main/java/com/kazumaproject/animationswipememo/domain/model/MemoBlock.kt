package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

enum class MemoBlockType {
    Text,
    Image,
    Drawing,
    List
}

enum class ListItemType {
    ORDERED,
    UNORDERED,
    CHECKBOX
}

data class ListItemAppearance(
    val fontScaleOverride: Float? = null
)

data class ListAppearance(
    val fontScale: Float = 1f,
    val levelScaleStep: Float = 0.04f,
    val minFontScale: Float = 0.72f,
    val indentStepDp: Float = 16f,
    val markerGapDp: Float = 8f
)

data class ListItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val indentLevel: Int = 0,
    val itemType: ListItemType = ListItemType.UNORDERED,
    val checked: Boolean = false,
    val isExpanded: Boolean = true,
    val appearanceOverride: ListItemAppearance? = null
)

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
    val strokes: List<StrokeData>,
    val listItems: List<ListItem>,
    val listAppearance: ListAppearance?
) {
    val isText: Boolean
        get() = type == MemoBlockType.Text

    val isImage: Boolean
        get() = type == MemoBlockType.Image

    val isDrawing: Boolean
        get() = type == MemoBlockType.Drawing

    val isList: Boolean
        get() = type == MemoBlockType.List

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
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null
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
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null
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
                strokes = strokes,
                listItems = emptyList(),
                listAppearance = null
            )
        }

        fun createList(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.3f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.List,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.62f,
                heightFraction = 0.28f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(),
                imageUri = null,
                strokes = emptyList(),
                listItems = listOf(ListItem(text = "")),
                listAppearance = ListAppearance()
            )
        }
    }
}
