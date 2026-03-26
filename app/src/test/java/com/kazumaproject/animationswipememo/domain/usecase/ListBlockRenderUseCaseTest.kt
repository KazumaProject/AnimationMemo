package com.kazumaproject.animationswipememo.domain.usecase

import androidx.compose.ui.text.style.TextDecoration
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.ListAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItem
import com.kazumaproject.animationswipememo.domain.model.ListItemAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListBlockRenderUseCaseTest {
    private val useCase = ListBlockRenderUseCase()

    @Test
    fun renderItems_hidesItem_whenAnyAncestorIsCollapsed() {
        val block = listBlock(
            listItems = listOf(
                ListItem(id = "a", text = "A", indentLevel = 0, itemType = ListItemType.UNORDERED, isExpanded = false),
                ListItem(id = "a1", text = "A1", indentLevel = 1, itemType = ListItemType.UNORDERED, isExpanded = true),
                ListItem(id = "a1a", text = "A1a", indentLevel = 2, itemType = ListItemType.UNORDERED, isExpanded = true),
                ListItem(id = "b", text = "B", indentLevel = 0, itemType = ListItemType.UNORDERED, isExpanded = true),
                ListItem(id = "b1", text = "B1", indentLevel = 1, itemType = ListItemType.UNORDERED, isExpanded = true)
            )
        )

        val renderedById = useCase.renderItems(block).associateBy { it.itemId }

        assertTrue(renderedById.getValue("a").visible)
        assertFalse(renderedById.getValue("a1").visible)
        assertFalse(renderedById.getValue("a1a").visible)
        assertTrue(renderedById.getValue("b").visible)
        assertTrue(renderedById.getValue("b1").visible)
    }

    @Test
    fun renderItems_computesOrderedNumbers_perIndent_usingOnlyVisibleOrderedItems() {
        val block = listBlock(
            listItems = listOf(
                ListItem(id = "o1", text = "1", indentLevel = 0, itemType = ListItemType.ORDERED),
                ListItem(id = "c1", text = "check", indentLevel = 0, itemType = ListItemType.CHECKBOX),
                ListItem(id = "o2", text = "2", indentLevel = 0, itemType = ListItemType.ORDERED),
                ListItem(id = "u1", text = "bullet", indentLevel = 0, itemType = ListItemType.UNORDERED),
                ListItem(id = "o3", text = "3", indentLevel = 0, itemType = ListItemType.ORDERED),
                ListItem(id = "parent", text = "parent", indentLevel = 0, itemType = ListItemType.UNORDERED, isExpanded = true),
                ListItem(id = "o4", text = "child ordered", indentLevel = 1, itemType = ListItemType.ORDERED),
                ListItem(id = "x1", text = "child checkbox", indentLevel = 1, itemType = ListItemType.CHECKBOX),
                ListItem(id = "o5", text = "child ordered 2", indentLevel = 1, itemType = ListItemType.ORDERED),
                ListItem(id = "collapsed", text = "collapsed", indentLevel = 0, itemType = ListItemType.UNORDERED, isExpanded = false),
                ListItem(id = "hidden", text = "hidden ordered", indentLevel = 1, itemType = ListItemType.ORDERED),
                ListItem(id = "o6", text = "4", indentLevel = 0, itemType = ListItemType.ORDERED)
            )
        )

        val renderedById = useCase.renderItems(block).associateBy { it.itemId }

        assertEquals(1, renderedById.getValue("o1").orderedNumber)
        assertEquals(2, renderedById.getValue("o2").orderedNumber)
        assertEquals(3, renderedById.getValue("o3").orderedNumber)
        assertEquals(4, renderedById.getValue("o6").orderedNumber)

        assertEquals(1, renderedById.getValue("o4").orderedNumber)
        assertEquals(2, renderedById.getValue("o5").orderedNumber)

        assertFalse(renderedById.getValue("hidden").visible)
        assertNull(renderedById.getValue("hidden").orderedNumber)
    }

    @Test
    fun renderItems_setsHasChildrenAndCanExpand_onlyWhenImmediateNextIsDeeper() {
        val block = listBlock(
            listItems = listOf(
                ListItem(id = "p", text = "parent", indentLevel = 0),
                ListItem(id = "c", text = "child", indentLevel = 1),
                ListItem(id = "n", text = "next root", indentLevel = 0)
            )
        )

        val renderedById = useCase.renderItems(block).associateBy { it.itemId }

        assertTrue(renderedById.getValue("p").hasChildren)
        assertTrue(renderedById.getValue("p").canExpand)
        assertFalse(renderedById.getValue("c").hasChildren)
        assertFalse(renderedById.getValue("n").canExpand)
    }

    @Test
    fun renderItems_appliesStrikeThrough_onlyForCheckedCheckbox() {
        val block = listBlock(
            listItems = listOf(
                ListItem(id = "x-on", text = "done", itemType = ListItemType.CHECKBOX, checked = true),
                ListItem(id = "x-off", text = "todo", itemType = ListItemType.CHECKBOX, checked = false),
                ListItem(id = "u", text = "plain", itemType = ListItemType.UNORDERED, checked = true)
            )
        )

        val renderedById = useCase.renderItems(block).associateBy { it.itemId }

        assertEquals(TextDecoration.LineThrough, renderedById.getValue("x-on").textDecoration)
        assertNull(renderedById.getValue("x-off").textDecoration)
        assertNull(renderedById.getValue("u").textDecoration)
    }

    @Test
    fun renderItems_calculatesPaddingAndFontScale_withMinAndOverride() {
        val block = listBlock(
            listAppearance = ListAppearance(
                fontScale = 1.0f,
                levelScaleStep = 0.2f,
                minFontScale = 0.7f,
                indentStepDp = 20f,
                markerGapDp = 8f
            ),
            listItems = listOf(
                ListItem(id = "base", text = "base", indentLevel = 0),
                ListItem(id = "deep", text = "deep", indentLevel = 3),
                ListItem(
                    id = "override",
                    text = "override",
                    indentLevel = 2,
                    appearanceOverride = ListItemAppearance(fontScaleOverride = 1.4f)
                )
            )
        )

        val renderedById = useCase.renderItems(block).associateBy { it.itemId }

        assertEquals(0f, renderedById.getValue("base").startPaddingDp)
        assertEquals(60f, renderedById.getValue("deep").startPaddingDp)
        assertEquals(40f, renderedById.getValue("override").startPaddingDp)

        assertEquals(1.0f, renderedById.getValue("base").fontScale)
        assertEquals(0.7f, renderedById.getValue("deep").fontScale)
        assertEquals(1.4f, renderedById.getValue("override").fontScale)
    }

    private fun listBlock(
        listItems: List<ListItem>,
        listAppearance: ListAppearance = ListAppearance()
    ): MemoBlock {
        return MemoBlock.createList(defaultAnimation = AnimationStyle.None)
            .copy(listItems = listItems, listAppearance = listAppearance)
    }
}

