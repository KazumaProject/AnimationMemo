package com.kazumaproject.animationswipememo.domain.model

enum class AnimationStyle(
    val displayName: String,
    val description: String
) {
    None("None", "Static text"),
    Fade("Fade", "Soft opacity loop"),
    Typewriter("Typewriter", "Characters appear over time"),
    Float("Float", "Gentle vertical drift"),
    Shake("Shake", "Playful side-to-side shake"),
    Bounce("Bounce", "Springy vertical bounce"),
    Glow("Glow", "Luminous pulse")
}
