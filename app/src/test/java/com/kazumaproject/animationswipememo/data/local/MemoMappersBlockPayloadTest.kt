package com.kazumaproject.animationswipememo.data.local

import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.ConversationItem
import com.kazumaproject.animationswipememo.domain.model.ConversationRole
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockPayload
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoMappersBlockPayloadTest {
    @Test
    fun draftSearchableText_includesPayloadFields() {
        val blocks = listOf(
            MemoBlock.createHeading(defaultAnimation = AnimationStyle.None)
                .copy(payload = MemoBlockPayload.Heading(text = "Project Plan")),
            MemoBlock.createCode(defaultAnimation = AnimationStyle.None)
                .copy(payload = MemoBlockPayload.Code(language = "kotlin", code = "println(\"hi\")")),
            MemoBlock.createLinkCard(defaultAnimation = AnimationStyle.None)
                .copy(payload = MemoBlockPayload.LinkCard(url = "https://example.com", title = "Example"))
        )
        val draft = MemoDraft(
            id = "memo",
            title = "Weekly",
            paperStyle = PaperStyle.WarmNote,
            blocks = blocks,
            createdAt = 1L,
            updatedAt = 1L
        )

        assertTrue(draft.searchableText.contains("Project Plan"))
        assertTrue(draft.searchableText.contains("println"))
        assertTrue(draft.searchableText.contains("example.com"))
    }

    @Test
    fun draftHasContent_trueWhenPayloadHasContent() {
        val conversation = MemoBlock.createConversation(defaultAnimation = AnimationStyle.None)
            .copy(
                payload = MemoBlockPayload.Conversation(
                    items = listOf(ConversationItem(speaker = "A", text = "Hello", role = ConversationRole.Left))
                )
            )
        val draft = MemoDraft(
            id = "memo",
            title = "",
            paperStyle = PaperStyle.WarmNote,
            blocks = listOf(conversation),
            createdAt = 1L,
            updatedAt = 1L
        )

        assertTrue(draft.hasContent)
        assertTrue(draft.summaryLabel.contains("conversation"))
    }
}

