package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

data class MemoDraft(
    val id: String,
    val paperStyle: PaperStyle,
    val blocks: List<MemoBlock>,
    val createdAt: Long,
    val updatedAt: Long
) {
    val searchableText: String
        get() = blocks
            .filter { it.type == MemoBlockType.Text }
            .mapNotNull { it.text.trim().takeIf(String::isNotBlank) }
            .joinToString(" ")

    val previewText: String
        get() = blocks.firstOrNull { it.type == MemoBlockType.Text && it.text.isNotBlank() }?.text
            ?: summaryLabel

    val displayPreviewText: String
        get() = previewText.ifBlank { "(empty memo)" }

    val hasContent: Boolean
        get() = blocks.any { block ->
            when (block.type) {
                MemoBlockType.Text -> block.text.isNotBlank()
                MemoBlockType.Image -> !block.imageUri.isNullOrBlank()
                MemoBlockType.Drawing -> block.strokes.any { it.points.size > 1 }
            }
        }

    val summaryLabel: String
        get() {
            val textCount = blocks.count { it.type == MemoBlockType.Text && it.text.isNotBlank() }
            val imageCount = blocks.count { it.type == MemoBlockType.Image && !it.imageUri.isNullOrBlank() }
            val drawingCount = blocks.count { it.type == MemoBlockType.Drawing && it.strokes.isNotEmpty() }
            val labels = buildList {
                if (textCount > 0) add("$textCount text")
                if (imageCount > 0) add("$imageCount image")
                if (drawingCount > 0) add("$drawingCount drawing")
            }
            return if (labels.isEmpty()) "(empty memo)" else labels.joinToString(" · ")
        }

    companion object {
        fun create(
            defaultAnimation: AnimationStyle,
            paperStyle: PaperStyle = PaperStyle.WarmNote,
            timestamp: Long = System.currentTimeMillis()
        ): MemoDraft {
            return MemoDraft(
                id = UUID.randomUUID().toString(),
                paperStyle = paperStyle,
                blocks = listOf(MemoBlock.createText(defaultAnimation = defaultAnimation)),
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}
