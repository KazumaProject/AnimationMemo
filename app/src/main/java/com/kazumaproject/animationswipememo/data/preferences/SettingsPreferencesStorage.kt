package com.kazumaproject.animationswipememo.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.AppSettings
import com.kazumaproject.animationswipememo.domain.model.GifQuality
import com.kazumaproject.animationswipememo.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "animation_swipe_memo_settings")

class SettingsPreferencesStorage(private val context: Context) {
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        preferences.toAppSettings()
    }

    suspend fun updateDefaultAnimation(animationStyle: AnimationStyle) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_ANIMATION] = animationStyle.name
        }
    }

    suspend fun updateGifQuality(gifQuality: GifQuality) {
        context.dataStore.edit { preferences ->
            preferences[Keys.GIF_QUALITY] = gifQuality.name
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateEditorSheetOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.EDITOR_SHEET_OPACITY] = opacity
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        return AppSettings(
            defaultAnimation = enumValueOfOrDefault(
                this[Keys.DEFAULT_ANIMATION],
                AnimationStyle.Fade
            ),
            gifQuality = enumValueOfOrDefault(
                this[Keys.GIF_QUALITY],
                GifQuality.Standard
            ),
            themeMode = enumValueOfOrDefault(
                this[Keys.THEME_MODE],
                ThemeMode.System
            ),
            editorSheetOpacity = this[Keys.EDITOR_SHEET_OPACITY] ?: 0.88f
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOfOrDefault(
        value: String?,
        default: T
    ): T {
        return value?.let {
            runCatching { enumValueOf<T>(it) }.getOrDefault(default)
        } ?: default
    }

    private object Keys {
        val DEFAULT_ANIMATION = stringPreferencesKey("default_animation")
        val GIF_QUALITY = stringPreferencesKey("gif_quality")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EDITOR_SHEET_OPACITY = floatPreferencesKey("editor_sheet_opacity")
    }
}
