package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

enum class MemoBlockType {
    Text,
    Image,
    Drawing,
    List,
    Heading,
    Toggle,
    Quote,
    Code,
    Divider,
    LinkCard,
    Table,
    Conversation,
    Latex,
    Unknown
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
    val listAppearance: ListAppearance?,
    val payload: MemoBlockPayload = MemoBlockPayload.None
) {
    val isText: Boolean
        get() = type == MemoBlockType.Text

    val isImage: Boolean
        get() = type == MemoBlockType.Image

    val isDrawing: Boolean
        get() = type == MemoBlockType.Drawing

    val isList: Boolean
        get() = type == MemoBlockType.List

    val supportsTextSizing: Boolean
        get() = type in setOf(
            MemoBlockType.Text,
            MemoBlockType.List,
            MemoBlockType.Heading,
            MemoBlockType.Quote,
            MemoBlockType.Code,
            MemoBlockType.Latex,
            MemoBlockType.Conversation,
            MemoBlockType.Toggle
        )

    val supportsTextStyleControls: Boolean
        get() = type in setOf(
            MemoBlockType.Text,
            MemoBlockType.Heading,
            MemoBlockType.Quote,
            MemoBlockType.Code,
            MemoBlockType.Latex
        )

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
                listAppearance = null,
                payload = MemoBlockPayload.None
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
                listAppearance = null,
                payload = MemoBlockPayload.None
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
                listAppearance = null,
                payload = MemoBlockPayload.None
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
                listAppearance = ListAppearance(),
                payload = MemoBlockPayload.None
            )
        }

        fun createHeading(
            defaultAnimation: AnimationStyle,
            level: HeadingLevel = HeadingLevel.H1,
            text: String = "",
            x: Float = 0.5f,
            y: Float = 0.3f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Heading,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.64f,
                heightFraction = 0.14f,
                contentAspectRatio = null,
                text = text,
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 36f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Heading(level = level, text = text)
            )
        }

        fun createToggle(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.32f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Toggle,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.68f,
                heightFraction = 0.22f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 24f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Toggle()
            )
        }

        fun createQuote(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.3f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Quote,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.66f,
                heightFraction = 0.16f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 26f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Quote()
            )
        }

        fun createCode(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.34f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Code,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.72f,
                heightFraction = 0.22f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 20f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Code()
            )
        }

        fun createDivider(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.4f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Divider,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.72f,
                heightFraction = 0.06f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 18f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Divider
            )
        }

        fun createLinkCard(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.36f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.LinkCard,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.72f,
                heightFraction = 0.18f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 20f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.LinkCard()
            )
        }

        fun createTable(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.38f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Table,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.74f,
                heightFraction = 0.24f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 18f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Table()
            )
        }

        fun createConversation(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.35f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Conversation,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.72f,
                heightFraction = 0.24f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 20f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Conversation()
            )
        }

        fun createLatex(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.34f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                type = MemoBlockType.Latex,
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.7f,
                heightFraction = 0.16f,
                contentAspectRatio = null,
                text = "",
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting(fontSize = 24f),
                imageUri = null,
                strokes = emptyList(),
                listItems = emptyList(),
                listAppearance = null,
                payload = MemoBlockPayload.Latex()
            )
        }
    }
}
