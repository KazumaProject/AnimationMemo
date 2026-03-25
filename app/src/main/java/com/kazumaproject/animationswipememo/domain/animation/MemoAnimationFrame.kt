package com.kazumaproject.animationswipememo.domain.animation

data class MemoAnimationFrame(
    val alpha: Float = 1f,
    val offsetXPx: Float = 0f,
    val offsetYPx: Float = 0f,
    val rotationDeg: Float = 0f,
    val scale: Float = 1f,
    val glowRadiusPx: Float = 0f,
    val visibleCharacters: Int = Int.MAX_VALUE
) {
    fun displayedText(text: String): String {
        return if (visibleCharacters >= text.length) {
            text
        } else {
            text.take(visibleCharacters.coerceAtLeast(0))
        }
    }
}
