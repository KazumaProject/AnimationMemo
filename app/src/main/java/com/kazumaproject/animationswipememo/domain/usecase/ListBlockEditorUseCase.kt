package com.kazumaproject.animationswipememo.domain.usecase

import com.kazumaproject.animationswipememo.domain.model.ListAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItem
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoBlock

class ListBlockEditorUseCase {
    fun addListItem(
        block: MemoBlock,
        text: String = "",
        index: Int = block.listItems.size,
        indentLevel: Int? = null
    ): MemoBlock {
        val items = block.listItems
        val safeIndex = index.coerceIn(0, items.size)
        val inferredIndent = indentLevel ?: items.getOrNull(safeIndex - 1)?.indentLevel ?: 0
        val newItem = ListItem(text = text, indentLevel = inferredIndent.coerceAtLeast(0))
        val nextItems = items.toMutableList().apply { add(safeIndex, newItem) }
        return block.copy(listItems = nextItems)
    }

    fun updateListItemText(block: MemoBlock, itemId: String, text: String): MemoBlock {
        return block.copy(
            listItems = block.listItems.map { item ->
                if (item.id == itemId) item.copy(text = text) else item
            }
        )
    }

    fun removeListItem(block: MemoBlock, itemId: String): MemoBlock {
        val targetIndex = block.listItems.indexOfFirst { it.id == itemId }
        if (targetIndex < 0) return block
        val rootLevel = block.listItems[targetIndex].indentLevel
        val endExclusive = findSubtreeEnd(block.listItems, targetIndex, rootLevel)
        val nextItems = block.listItems.toMutableList().apply {
            subList(targetIndex, endExclusive).clear()
        }
        return block.copy(listItems = nextItems)
    }

    fun updateListItemType(block: MemoBlock, itemId: String, type: ListItemType): MemoBlock {
        return block.copy(
            listItems = block.listItems.map { item ->
                if (item.id != itemId) {
                    item
                } else {
                    item.copy(itemType = type, checked = if (type == ListItemType.CHECKBOX) item.checked else false)
                }
            }
        )
    }

    fun toggleListItemChecked(block: MemoBlock, itemId: String): MemoBlock {
        return block.copy(
            listItems = block.listItems.map { item ->
                if (item.id == itemId && item.itemType == ListItemType.CHECKBOX) {
                    item.copy(checked = !item.checked)
                } else {
                    item
                }
            }
        )
    }

    fun increaseIndent(block: MemoBlock, itemId: String): MemoBlock {
        val idx = block.listItems.indexOfFirst { it.id == itemId }
        if (idx <= 0) return block
        val previousLevel = block.listItems[idx - 1].indentLevel
        val nextLevel = (block.listItems[idx].indentLevel + 1).coerceAtMost(previousLevel + 1)
        return updateIndent(block, itemId, nextLevel)
    }

    fun decreaseIndent(block: MemoBlock, itemId: String): MemoBlock {
        val item = block.listItems.firstOrNull { it.id == itemId } ?: return block
        return updateIndent(block, itemId, (item.indentLevel - 1).coerceAtLeast(0))
    }

    fun moveListItem(block: MemoBlock, fromIndex: Int, toIndex: Int): MemoBlock {
        val items = block.listItems
        if (fromIndex !in items.indices) return block
        val safeToIndex = toIndex.coerceIn(0, items.lastIndex)
        if (fromIndex == safeToIndex) return block

        val rootLevel = items[fromIndex].indentLevel
        val fromEnd = findSubtreeEnd(items, fromIndex, rootLevel)
        val moving = items.subList(fromIndex, fromEnd)

        val remained = items.toMutableList().apply { subList(fromIndex, fromEnd).clear() }
        val insertAt = if (safeToIndex > fromIndex) {
            (safeToIndex - (fromEnd - fromIndex) + 1).coerceIn(0, remained.size)
        } else {
            safeToIndex
        }
        remained.addAll(insertAt, moving)
        return block.copy(listItems = remained)
    }

    fun toggleListItemExpanded(block: MemoBlock, itemId: String): MemoBlock {
        return block.copy(
            listItems = block.listItems.map { item ->
                if (item.id == itemId) item.copy(isExpanded = !item.isExpanded) else item
            }
        )
    }

    fun updateListAppearance(block: MemoBlock, appearance: ListAppearance): MemoBlock {
        return block.copy(listAppearance = appearance)
    }

    private fun updateIndent(block: MemoBlock, itemId: String, indent: Int): MemoBlock {
        return block.copy(
            listItems = block.listItems.map { item ->
                if (item.id == itemId) item.copy(indentLevel = indent.coerceAtLeast(0)) else item
            }
        )
    }

    private fun findSubtreeEnd(items: List<ListItem>, rootIndex: Int, rootLevel: Int): Int {
        var i = rootIndex + 1
        while (i < items.size && items[i].indentLevel > rootLevel) {
            i++
        }
        return i
    }
}

