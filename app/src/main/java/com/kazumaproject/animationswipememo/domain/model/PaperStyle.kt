package com.kazumaproject.animationswipememo.domain.model

data class PaperPalette(
    val backdropArgb: Int,
    val paperArgb: Int,
    val lineArgb: Int,
    val shadowArgb: Int,
    val accentArgb: Int,
    val edgeArgb: Int
)

enum class PaperPattern {
    Lined,
    Blank,
    Grid,
    DotGrid,
    Margin,
    Music
}

enum class PaperStyle(
    val displayName: String,
    val pattern: PaperPattern,
    val showTape: Boolean,
    private val lightPalette: PaperPalette,
    private val darkPalette: PaperPalette
) {
    WarmNote(
        displayName = "Warm Note",
        pattern = PaperPattern.Lined,
        showTape = true,
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
    PlainWhite(
        displayName = "Plain White",
        pattern = PaperPattern.Blank,
        showTape = false,
        lightPalette = PaperPalette(
            backdropArgb = 0xFFF1F3F5.toInt(),
            paperArgb = 0xFFFFFFFF.toInt(),
            lineArgb = 0x00000000,
            shadowArgb = 0x1A111827,
            accentArgb = 0xFFD6DCE5.toInt(),
            edgeArgb = 0xFFE2E8F0.toInt()
        ),
        darkPalette = PaperPalette(
            backdropArgb = 0xFF111827.toInt(),
            paperArgb = 0xFF1F2937.toInt(),
            lineArgb = 0x00000000,
            shadowArgb = 0x66000000,
            accentArgb = 0xFF475569.toInt(),
            edgeArgb = 0xFF334155.toInt()
        )
    ),
    GraphPaper(
        displayName = "Graph Paper",
        pattern = PaperPattern.Grid,
        showTape = false,
        lightPalette = PaperPalette(
            backdropArgb = 0xFFE7F0EC.toInt(),
            paperArgb = 0xFFF8FFFC.toInt(),
            lineArgb = 0xFFB8D6C5.toInt(),
            shadowArgb = 0x1F21403A,
            accentArgb = 0xFF76A98E.toInt(),
            edgeArgb = 0xFFD8EADF.toInt()
        ),
        darkPalette = PaperPalette(
            backdropArgb = 0xFF0F1715.toInt(),
            paperArgb = 0xFF182320.toInt(),
            lineArgb = 0xFF35574B.toInt(),
            shadowArgb = 0x66000000,
            accentArgb = 0xFF4E7A68.toInt(),
            edgeArgb = 0xFF294038.toInt()
        )
    ),
    DotGrid(
        displayName = "Dot Grid",
        pattern = PaperPattern.DotGrid,
        showTape = false,
        lightPalette = PaperPalette(
            backdropArgb = 0xFFF4EAF4.toInt(),
            paperArgb = 0xFFFFFBFF.toInt(),
            lineArgb = 0xFFE2D2E4.toInt(),
            shadowArgb = 0x1F30213A,
            accentArgb = 0xFFB78CBC.toInt(),
            edgeArgb = 0xFFEBDFF0.toInt()
        ),
        darkPalette = PaperPalette(
            backdropArgb = 0xFF171118.toInt(),
            paperArgb = 0xFF241B26.toInt(),
            lineArgb = 0xFF48354A.toInt(),
            shadowArgb = 0x66000000,
            accentArgb = 0xFF8E6791.toInt(),
            edgeArgb = 0xFF382A3A.toInt()
        )
    ),
    MarginNote(
        displayName = "Margin Note",
        pattern = PaperPattern.Margin,
        showTape = false,
        lightPalette = PaperPalette(
            backdropArgb = 0xFFF7EFE5.toInt(),
            paperArgb = 0xFFFFFCF8.toInt(),
            lineArgb = 0xFFE6D9C7.toInt(),
            shadowArgb = 0x1F402E1E,
            accentArgb = 0xFFD77A7A.toInt(),
            edgeArgb = 0xFFF0E3D2.toInt()
        ),
        darkPalette = PaperPalette(
            backdropArgb = 0xFF1B1713.toInt(),
            paperArgb = 0xFF2B2520.toInt(),
            lineArgb = 0xFF4C4035.toInt(),
            shadowArgb = 0x66000000,
            accentArgb = 0xFF9C5A5A.toInt(),
            edgeArgb = 0xFF3B312A.toInt()
        )
    ),
    MusicSheet(
        displayName = "Music Sheet",
        pattern = PaperPattern.Music,
        showTape = false,
        lightPalette = PaperPalette(
            backdropArgb = 0xFFECEAD9.toInt(),
            paperArgb = 0xFFFFFDF2.toInt(),
            lineArgb = 0xFFD9D2B5.toInt(),
            shadowArgb = 0x1F353019,
            accentArgb = 0xFF8D7B43.toInt(),
            edgeArgb = 0xFFE8E0C2.toInt()
        ),
        darkPalette = PaperPalette(
            backdropArgb = 0xFF17160F.toInt(),
            paperArgb = 0xFF252318.toInt(),
            lineArgb = 0xFF4A452E.toInt(),
            shadowArgb = 0x66000000,
            accentArgb = 0xFF8A7B4D.toInt(),
            edgeArgb = 0xFF3A3726.toInt()
        )
    );

    fun palette(isDark: Boolean): PaperPalette {
        return if (isDark) darkPalette else lightPalette
    }

    companion object {
        fun fromName(name: String?): PaperStyle {
            return entries.firstOrNull { it.name == name } ?: WarmNote
        }
    }
}
