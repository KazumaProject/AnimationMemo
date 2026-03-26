package com.kazumaproject.animationswipememo.domain.model

enum class GifQuality(
    val displayName: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val durationMillis: Int
) {
    Standard(
        displayName = "Standard",
        width = 360,
        height = 480,
        fps = 12,
        durationMillis = 2400
    ),
    High(
        displayName = "High",
        width = 720,
        height = 960,
        fps = 18,
        durationMillis = 3200
    )
}

enum class ThemeMode(val displayName: String) {
    System("System"),
    Light("Light"),
    Dark("Dark")
}

data class AppSettings(
    val defaultAnimation: AnimationStyle = AnimationStyle.Fade,
    val gifQuality: GifQuality = GifQuality.Standard,
    val themeMode: ThemeMode = ThemeMode.System,
    val editorSheetOpacity: Float = 0.88f,
    val defaultPaperStyle: PaperStyle = PaperStyle.WarmNote,
    val recentCodeLanguages: List<String> = emptyList()
)
