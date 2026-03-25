package com.kazumaproject.animationswipememo.domain.model

enum class MemoTextAlign(val displayName: String) {
    Start("Start"),
    Center("Center"),
    End("End")
}

enum class MemoFontFamily(val displayName: String) {
    SystemSerif("System Serif"),
    MPlusRounded1c("M PLUS Rounded 1c"),
    ZenOldMincho("Zen Old Mincho"),
    ZenMaruGothic("Zen Maru Gothic"),
    KaiseiDecol("Kaisei Decol"),
    Yomogi("Yomogi")
}

data class TextStyleSetting(
    val fontSize: Float = 28f,
    val textColor: Int = DEFAULT_LIGHT_TEXT_COLOR,
    val textAlign: MemoTextAlign = MemoTextAlign.Center,
    val fontFamily: MemoFontFamily = MemoFontFamily.SystemSerif,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
) {
    companion object {
        const val MIN_FONT_SIZE = 16f
        const val MAX_FONT_SIZE = 64f
        const val DEFAULT_LIGHT_TEXT_COLOR = 0xFF2D241C.toInt()
        const val DEFAULT_DARK_TEXT_COLOR = 0xFFF7F0E2.toInt()
    }

    fun resolvedTextColor(darkTheme: Boolean): Int {
        return if (darkTheme && textColor == DEFAULT_LIGHT_TEXT_COLOR) {
            DEFAULT_DARK_TEXT_COLOR
        } else {
            textColor
        }
    }
}
