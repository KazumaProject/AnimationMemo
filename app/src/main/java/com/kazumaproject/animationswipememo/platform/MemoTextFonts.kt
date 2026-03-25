package com.kazumaproject.animationswipememo.platform

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.res.ResourcesCompat
import com.kazumaproject.animationswipememo.R
import com.kazumaproject.animationswipememo.domain.model.MemoFontFamily
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting

private val mPlusRoundedComposeFont = FontFamily(Font(R.font.mplusrounded1c_regular))
private val zenOldMinchoComposeFont = FontFamily(Font(R.font.zenoldmincho_regular))
private val zenMaruGothicComposeFont = FontFamily(Font(R.font.zenmarugothic_regular))
private val kaiseiDecolComposeFont = FontFamily(Font(R.font.kaiseidecol_regular))
private val yomogiComposeFont = FontFamily(Font(R.font.yomogi_regular))

fun MemoFontFamily.toComposeFontFamily(): FontFamily {
    return when (this) {
        MemoFontFamily.SystemSerif -> FontFamily.Serif
        MemoFontFamily.MPlusRounded1c -> mPlusRoundedComposeFont
        MemoFontFamily.ZenOldMincho -> zenOldMinchoComposeFont
        MemoFontFamily.ZenMaruGothic -> zenMaruGothicComposeFont
        MemoFontFamily.KaiseiDecol -> kaiseiDecolComposeFont
        MemoFontFamily.Yomogi -> yomogiComposeFont
    }
}

fun MemoFontFamily.toTypeface(context: Context): Typeface {
    return when (this) {
        MemoFontFamily.SystemSerif -> Typeface.SERIF
        MemoFontFamily.MPlusRounded1c -> {
            ResourcesCompat.getFont(context, R.font.mplusrounded1c_regular) ?: Typeface.SANS_SERIF
        }

        MemoFontFamily.ZenOldMincho -> {
            ResourcesCompat.getFont(context, R.font.zenoldmincho_regular) ?: Typeface.SERIF
        }

        MemoFontFamily.ZenMaruGothic -> {
            ResourcesCompat.getFont(context, R.font.zenmarugothic_regular) ?: Typeface.SANS_SERIF
        }

        MemoFontFamily.KaiseiDecol -> {
            ResourcesCompat.getFont(context, R.font.kaiseidecol_regular) ?: Typeface.SERIF
        }

        MemoFontFamily.Yomogi -> {
            ResourcesCompat.getFont(context, R.font.yomogi_regular) ?: Typeface.SANS_SERIF
        }
    }
}

fun TextStyleSetting.composeFontWeight(): FontWeight {
    return if (isBold) FontWeight.Bold else FontWeight.Normal
}

fun TextStyleSetting.composeFontStyle(): FontStyle {
    return if (isItalic) FontStyle.Italic else FontStyle.Normal
}

fun TextStyleSetting.composeTextDecoration(): TextDecoration? {
    return if (isUnderline) TextDecoration.Underline else null
}

fun TextStyleSetting.toStyledTypeface(context: Context): Typeface {
    val baseTypeface = fontFamily.toTypeface(context)
    val style = when {
        isBold && isItalic -> Typeface.BOLD_ITALIC
        isBold -> Typeface.BOLD
        isItalic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
    return Typeface.create(baseTypeface, style)
}
