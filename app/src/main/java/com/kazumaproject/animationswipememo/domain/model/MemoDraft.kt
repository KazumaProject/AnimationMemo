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
                .plus(blocks.flatMap { block -> block.payload.searchableTextParts() })
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
                else -> it.payload.searchableTextParts().isNotEmpty()
            }
        }?.let {
            when (it.type) {
                MemoBlockType.List -> it.listItems.firstOrNull { item -> item.text.isNotBlank() }?.text
                else -> it.payload.searchableTextParts().firstOrNull() ?: it.text
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
                else -> block.payload.hasContent()
            }
        }

    val summaryLabel: String
        get() {
            val textCount = blocks.count { it.type == MemoBlockType.Text && it.text.isNotBlank() }
            val imageCount = blocks.count { it.type == MemoBlockType.Image && !it.imageUri.isNullOrBlank() }
            val drawingCount = blocks.count { it.type == MemoBlockType.Drawing && it.strokes.isNotEmpty() }
            val listCount = blocks.count { it.type == MemoBlockType.List && it.listItems.any { item -> item.text.isNotBlank() } }
            val headingCount = blocks.count { it.type == MemoBlockType.Heading && it.payload.hasContent() }
            val toggleCount = blocks.count { it.type == MemoBlockType.Toggle && it.payload.hasContent() }
            val quoteCount = blocks.count { it.type == MemoBlockType.Quote && it.payload.hasContent() }
            val codeCount = blocks.count { it.type == MemoBlockType.Code && it.payload.hasContent() }
            val dividerCount = blocks.count { it.type == MemoBlockType.Divider }
            val linkCardCount = blocks.count { it.type == MemoBlockType.LinkCard && it.payload.hasContent() }
            val tableCount = blocks.count { it.type == MemoBlockType.Table && it.payload.hasContent() }
            val conversationCount = blocks.count { it.type == MemoBlockType.Conversation && it.payload.hasContent() }
            val latexCount = blocks.count { it.type == MemoBlockType.Latex && it.payload.hasContent() }
            val labels = buildList {
                if (textCount > 0) add("$textCount text")
                if (imageCount > 0) add("$imageCount image")
                if (drawingCount > 0) add("$drawingCount drawing")
                if (listCount > 0) add("$listCount list")
                if (headingCount > 0) add("$headingCount heading")
                if (toggleCount > 0) add("$toggleCount toggle")
                if (quoteCount > 0) add("$quoteCount quote")
                if (codeCount > 0) add("$codeCount code")
                if (dividerCount > 0) add("$dividerCount divider")
                if (linkCardCount > 0) add("$linkCardCount link")
                if (tableCount > 0) add("$tableCount table")
                if (conversationCount > 0) add("$conversationCount conversation")
                if (latexCount > 0) add("$latexCount math")
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
