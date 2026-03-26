package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

enum class HeadingLevel {
    H1,
    H2,
    H3
}

enum class ConversationRole {
    Left,
    Right,
    Neutral
}

data class ToggleChildBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: MemoBlockType = MemoBlockType.Text,
    val text: String = ""
)

data class TableRow(
    val id: String = UUID.randomUUID().toString(),
    val cells: List<String>
)

data class ConversationItem(
    val id: String = UUID.randomUUID().toString(),
    val speaker: String = "",
    val text: String = "",
    val role: ConversationRole = ConversationRole.Left
)

sealed interface MemoBlockPayload {
    data object None : MemoBlockPayload

    data class Heading(
        val level: HeadingLevel = HeadingLevel.H1,
        val text: String = ""
    ) : MemoBlockPayload

    data class Toggle(
        val title: String = "",
        val initiallyExpanded: Boolean = true,
        val childBlocks: List<ToggleChildBlock> = emptyList()
    ) : MemoBlockPayload

    data class Quote(
        val text: String = ""
    ) : MemoBlockPayload

    data class Code(
        val language: String = "Plain Text",
        val code: String = ""
    ) : MemoBlockPayload

    data object Divider : MemoBlockPayload

    data class LinkCard(
        val url: String = "",
        val title: String = "",
        val description: String = "",
        val imageUrl: String = "",
        val faviconUrl: String = ""
    ) : MemoBlockPayload

    data class Table(
        val rows: List<TableRow> = listOf(
            TableRow(cells = listOf("", "")),
            TableRow(cells = listOf("", ""))
        ),
        val hasHeaderRow: Boolean = false,
        val hasHeaderColumn: Boolean = false
    ) : MemoBlockPayload

    data class Conversation(
        val items: List<ConversationItem> = listOf(
            ConversationItem(speaker = "A", text = "", role = ConversationRole.Left)
        )
    ) : MemoBlockPayload

    data class Latex(
        val expression: String = ""
    ) : MemoBlockPayload

    data class Unknown(
        val rawType: String,
        val rawPayloadJson: String
    ) : MemoBlockPayload
}

fun MemoBlockPayload.searchableTextParts(): List<String> {
    return when (this) {
        MemoBlockPayload.None,
        MemoBlockPayload.Divider -> emptyList()
        is MemoBlockPayload.Heading -> listOf(text)
        is MemoBlockPayload.Toggle -> listOf(title) + childBlocks.map { it.text }
        is MemoBlockPayload.Quote -> listOf(text)
        is MemoBlockPayload.Code -> listOf(code)
        is MemoBlockPayload.LinkCard -> listOf(url, title, description)
        is MemoBlockPayload.Table -> rows.flatMap { row -> row.cells }
        is MemoBlockPayload.Conversation -> items.flatMap { item -> listOf(item.speaker, item.text) }
        is MemoBlockPayload.Latex -> listOf(expression)
        is MemoBlockPayload.Unknown -> emptyList()
    }.map { it.trim() }.filter { it.isNotBlank() }
}

fun MemoBlockPayload.hasContent(): Boolean {
    return when (this) {
        MemoBlockPayload.None,
        MemoBlockPayload.Divider -> false
        is MemoBlockPayload.Heading -> text.isNotBlank()
        is MemoBlockPayload.Toggle -> title.isNotBlank() || childBlocks.any { it.text.isNotBlank() }
        is MemoBlockPayload.Quote -> text.isNotBlank()
        is MemoBlockPayload.Code -> code.isNotBlank()
        is MemoBlockPayload.LinkCard -> url.isNotBlank() || title.isNotBlank() || description.isNotBlank()
        is MemoBlockPayload.Table -> rows.any { row -> row.cells.any { it.isNotBlank() } }
        is MemoBlockPayload.Conversation -> items.any { it.speaker.isNotBlank() || it.text.isNotBlank() }
        is MemoBlockPayload.Latex -> expression.isNotBlank()
        is MemoBlockPayload.Unknown -> true
    }
}

