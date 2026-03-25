package com.kazumaproject.animationswipememo.data.repository

import com.kazumaproject.animationswipememo.data.preferences.SettingsPreferencesStorage
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.AppSettings
import com.kazumaproject.animationswipememo.domain.model.GifQuality
import com.kazumaproject.animationswipememo.domain.model.ThemeMode
import com.kazumaproject.animationswipememo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class DefaultSettingsRepository(
    private val storage: SettingsPreferencesStorage
) : SettingsRepository {
    override val settings: Flow<AppSettings> = storage.settings

    override suspend fun updateDefaultAnimation(animationStyle: AnimationStyle) {
        storage.updateDefaultAnimation(animationStyle)
    }

    override suspend fun updateGifQuality(gifQuality: GifQuality) {
        storage.updateGifQuality(gifQuality)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        storage.updateThemeMode(themeMode)
    }

    override fun currentValue(): AppSettings = AppSettings()
}
