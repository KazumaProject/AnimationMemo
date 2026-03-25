package com.kazumaproject.animationswipememo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.kazumaproject.animationswipememo.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = WarmBrown,
    secondary = NoteBlue,
    tertiary = AccentGold,
    background = CanvasSand,
    surface = SoftEdge,
    onPrimary = SoftEdge,
    onSecondary = SoftEdge,
    onTertiary = PaperInk,
    onBackground = PaperInk,
    onSurface = PaperInk
)

private val DarkColors = darkColorScheme(
    primary = AccentGold,
    secondary = NoteBlue,
    tertiary = SoftEdge,
    background = ColorNight,
    surface = SurfaceNight,
    onPrimary = ColorNight,
    onSecondary = SoftEdge,
    onTertiary = PaperInk,
    onBackground = SoftEdge,
    onSurface = SoftEdge
)

@Composable
fun AnimationSwipeMemoTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
