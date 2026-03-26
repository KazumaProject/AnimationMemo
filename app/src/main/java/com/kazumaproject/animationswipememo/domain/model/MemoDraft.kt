package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

data class MemoDraft(
    val id: String,
    val title: String,
    val paperStyle: PaperStyle,
    val blocks: List<MemoBlock>,
    val createdAt: Long,
    val updatedAt: Long
) {
    val displayTitle: String
        get() = title.trim().ifBlank { "Untitled memo" }

    val searchableText: String
        get() = listOfNotNull(
            title.trim().takeIf(String::isNotBlank),
            blocks
                .filter { it.type == MemoBlockType.Text || it.type == MemoBlockType.List }
                .mapNotNull { it.text.trim().takeIf(String::isNotBlank) }
                .plus(
                    blocks
                        .filter { it.type == MemoBlockType.List }
                        .flatMap { block -> block.listItems }
                        .mapNotNull { item -> item.text.trim().takeIf(String::isNotBlank) }
                )
                .joinToString(" ")
                .takeIf(String::isNotBlank)
        )
            .joinToString(" ")

    val previewText: String
        get() = blocks.firstOrNull {
            when (it.type) {
                MemoBlockType.Text -> it.text.isNotBlank()
                MemoBlockType.List -> it.listItems.any { item -> item.text.isNotBlank() }
                MemoBlockType.Image,
                MemoBlockType.Drawing -> false
            }
        }?.let {
            if (it.type == MemoBlockType.List) {
                it.listItems.firstOrNull { item -> item.text.isNotBlank() }?.text
            } else {
                it.text
            }
        }
            ?: summaryLabel

    val displayPreviewText: String
        get() = previewText.ifBlank { "(empty memo)" }

    val hasContent: Boolean
        get() = blocks.any { block ->
            when (block.type) {
                MemoBlockType.Text -> block.text.isNotBlank()
                MemoBlockType.Image -> !block.imageUri.isNullOrBlank()
                MemoBlockType.Drawing -> block.strokes.any { it.points.size > 1 }
                MemoBlockType.List -> block.listItems.any { it.text.isNotBlank() }
            }
        }

    val summaryLabel: String
        get() {
            val textCount = blocks.count { it.type == MemoBlockType.Text && it.text.isNotBlank() }
            val imageCount = blocks.count { it.type == MemoBlockType.Image && !it.imageUri.isNullOrBlank() }
            val drawingCount = blocks.count { it.type == MemoBlockType.Drawing && it.strokes.isNotEmpty() }
            val listCount = blocks.count { it.type == MemoBlockType.List && it.listItems.any { item -> item.text.isNotBlank() } }
            val labels = buildList {
                if (textCount > 0) add("$textCount text")
                if (imageCount > 0) add("$imageCount image")
                if (drawingCount > 0) add("$drawingCount drawing")
                if (listCount > 0) add("$listCount list")
            }
            return if (labels.isEmpty()) "(empty memo)" else labels.joinToString(" · ")
        }

    companion object {
        fun create(
            paperStyle: PaperStyle = PaperStyle.WarmNote,
            timestamp: Long = System.currentTimeMillis()
        ): MemoDraft {
            return MemoDraft(
                id = UUID.randomUUID().toString(),
                title = "",
                paperStyle = paperStyle,
                blocks = emptyList(),
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}
