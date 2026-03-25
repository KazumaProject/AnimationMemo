package com.kazumaproject.animationswipememo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kazumaproject.animationswipememo.di.AppContainer
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.AppSettings
import com.kazumaproject.animationswipememo.domain.model.GifQuality
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.ThemeMode
import com.kazumaproject.animationswipememo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val uiState: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = settingsRepository.currentValue()
    )

    fun updateDefaultAnimation(style: AnimationStyle) {
        viewModelScope.launch {
            settingsRepository.updateDefaultAnimation(style)
        }
    }

    fun updateGifQuality(quality: GifQuality) {
        viewModelScope.launch {
            settingsRepository.updateGifQuality(quality)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun updateEditorSheetOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepository.updateEditorSheetOpacity(opacity)
        }
    }

    fun updateDefaultPaperStyle(paperStyle: PaperStyle) {
        viewModelScope.launch {
            settingsRepository.updateDefaultPaperStyle(paperStyle)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(container.settingsRepository)
            }
        }
    }
}
