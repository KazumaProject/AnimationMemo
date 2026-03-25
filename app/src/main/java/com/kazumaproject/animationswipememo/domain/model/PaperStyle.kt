package com.kazumaproject.animationswipememo.domain.model

data class PaperPalette(
    val backdropArgb: Int,
    val paperArgb: Int,
    val lineArgb: Int,
    val shadowArgb: Int,
    val accentArgb: Int,
    val edgeArgb: Int
)

enum class PaperStyle(
    val displayName: String,
    private val lightPalette: PaperPalette,
    private val darkPalette: PaperPalette
) {
    WarmNote(
        displayName = "Warm Note",
        lightPalette = PaperPalette(
            backdropArgb = 0xFFF2E8D6.toInt(),
            paperArgb = 0xFFFFF8E7.toInt(),
            lineArgb = 0xFFE5D8B4.toInt(),
            shadowArgb = 0x33221B12,
            accentArgb = 0xFFF2C97D.toInt(),
            edgeArgb = 0xFFE7DAB9.toInt()
        ),
        darkPalette = PaperPalette(
            backdropArgb = 0xFF1D1815.toInt(),
            paperArgb = 0xFF2B241F.toInt(),
            lineArgb = 0xFF4A4036.toInt(),
            shadowArgb = 0x66000000,
            accentArgb = 0xFF8D6D3B.toInt(),
            edgeArgb = 0xFF5B4F43.toInt()
        )
    ),
    CloudBlue(
        displayName = "Cloud Blue",
        lightPalette = PaperPalette(
            backdropArgb = 0xFFE2EEF5.toInt(),
            paperArgb = 0xFFF9FDFF.toInt(),
            lineArgb = 0xFFD4E4F0.toInt(),
            shadowArgb = 0x33203340,
            accentArgb = 0xFF8BB6D9.toInt(),
            edgeArgb = 0xFFDCEBF6.toInt()
        ),
        darkPalette = PaperPalette(
            backdropArgb = 0xFF111B22.toInt(),
            paperArgb = 0xFF1E2A33.toInt(),
            lineArgb = 0xFF355061.toInt(),
            shadowArgb = 0x66000000,
            accentArgb = 0xFF4B7698.toInt(),
            edgeArgb = 0xFF415A69.toInt()
        )
    );

    fun palette(isDark: Boolean): PaperPalette {
        return if (isDark) darkPalette else lightPalette
    }
}
