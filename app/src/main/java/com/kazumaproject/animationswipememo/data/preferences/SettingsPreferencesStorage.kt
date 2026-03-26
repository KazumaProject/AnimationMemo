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
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.ThemeMode
import com.kazumaproject.animationswipememo.ui.components.render.supportedCodeLanguages
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

    suspend fun updateDefaultPaperStyle(paperStyle: PaperStyle) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_PAPER_STYLE] = paperStyle.name
        }
    }

    suspend fun updateRecentCodeLanguages(languages: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.RECENT_CODE_LANGUAGES] = languages.joinToString(separator = "\n")
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
            editorSheetOpacity = this[Keys.EDITOR_SHEET_OPACITY] ?: 0.88f,
            defaultPaperStyle = PaperStyle.fromName(this[Keys.DEFAULT_PAPER_STYLE]),
            recentCodeLanguages = decodeRecentCodeLanguages(this[Keys.RECENT_CODE_LANGUAGES])
        )
    }

    private fun decodeRecentCodeLanguages(rawValue: String?): List<String> {
        if (rawValue.isNullOrBlank()) return emptyList()
        val supported = supportedCodeLanguages()
        return rawValue
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { value -> supported.firstOrNull { it.equals(value, ignoreCase = true) } }
            .distinctBy { it.lowercase() }
            .take(5)
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
        val DEFAULT_PAPER_STYLE = stringPreferencesKey("default_paper_style")
        val RECENT_CODE_LANGUAGES = stringPreferencesKey("recent_code_languages")
    }
}
