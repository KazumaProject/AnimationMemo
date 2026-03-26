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
    Glow("Glow", "Luminous pulse"),
    Pulse("Pulse", "Breathing scale pulse"),
    Zoom("Zoom", "Slow zoom in and out"),
    Wiggle("Wiggle", "Tiny rotating wobble"),
    Flicker("Flicker", "Quick flickering visibility"),
    Stamp("Stamp", "Press-in and settle"),
    Slide("Slide", "Sideways drifting loop"),
    Spin("Spin", "Continuous rotation"),
    Wave("Wave", "Gentle swaying wave"),
    Pop("Pop", "Quick pop and settle");

    fun supports(blockType: MemoBlockType): Boolean {
        return when (blockType) {
            MemoBlockType.Text -> true
            MemoBlockType.Image -> this != Typewriter
            MemoBlockType.Drawing -> this != Typewriter
            MemoBlockType.List -> this != Typewriter
            MemoBlockType.Heading -> this != Typewriter
            MemoBlockType.Toggle -> this != Typewriter
            MemoBlockType.Quote -> this != Typewriter
            MemoBlockType.Code -> this != Typewriter
            MemoBlockType.Divider -> this == None
            MemoBlockType.LinkCard -> this != Typewriter
            MemoBlockType.Table -> this != Typewriter
            MemoBlockType.Conversation -> this != Typewriter
            MemoBlockType.Latex -> this != Typewriter
            MemoBlockType.Unknown -> this == None
            else -> this != Typewriter
        }
    }

    companion object {
        fun availableFor(blockType: MemoBlockType): List<AnimationStyle> {
            return entries.filter { it.supports(blockType) }
        }
    }
}
