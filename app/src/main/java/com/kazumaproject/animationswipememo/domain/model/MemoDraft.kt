package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

data class MemoDraft(
    val id: String,
    val paperStyle: PaperStyle,
    val blocks: List<MemoBlock>,
    val createdAt: Long,
    val updatedAt: Long
) {
    val previewText: String
        get() = blocks.firstOrNull { it.text.isNotBlank() }?.text ?: ""

    val hasContent: Boolean
        get() = blocks.any { it.text.isNotBlank() }

    companion object {
        fun create(
            defaultAnimation: AnimationStyle,
            paperStyle: PaperStyle = PaperStyle.WarmNote,
            timestamp: Long = System.currentTimeMillis()
        ): MemoDraft {
            return MemoDraft(
                id = UUID.randomUUID().toString(),
                paperStyle = paperStyle,
                blocks = listOf(MemoBlock.create(defaultAnimation = defaultAnimation)),
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}
