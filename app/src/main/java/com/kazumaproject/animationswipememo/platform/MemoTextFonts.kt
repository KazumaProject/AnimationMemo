package com.kazumaproject.animationswipememo.platform

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.kazumaproject.animationswipememo.R
import com.kazumaproject.animationswipememo.domain.model.MemoFontFamily

private val mPlusRoundedComposeFont = FontFamily(Font(R.font.mplusrounded1c_regular))
private val zenOldMinchoComposeFont = FontFamily(Font(R.font.zenoldmincho_regular))

fun MemoFontFamily.toComposeFontFamily(): FontFamily {
    return when (this) {
        MemoFontFamily.SystemSerif -> FontFamily.Serif
        MemoFontFamily.MPlusRounded1c -> mPlusRoundedComposeFont
        MemoFontFamily.ZenOldMincho -> zenOldMinchoComposeFont
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
    }
}
