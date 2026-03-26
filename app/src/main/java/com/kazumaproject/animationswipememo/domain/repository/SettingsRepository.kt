package com.kazumaproject.animationswipememo.domain.repository

import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.AppSettings
import com.kazumaproject.animationswipememo.domain.model.GifQuality
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateDefaultAnimation(animationStyle: AnimationStyle)
    suspend fun updateGifQuality(gifQuality: GifQuality)
    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateEditorSheetOpacity(opacity: Float)
    suspend fun updateDefaultPaperStyle(paperStyle: PaperStyle)
    suspend fun updateRecentCodeLanguages(languages: List<String>)
    fun currentValue(): AppSettings
}
