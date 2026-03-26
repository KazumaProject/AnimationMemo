package com.kazumaproject.animationswipememo.domain.usecase

import androidx.compose.ui.text.style.TextDecoration
import com.kazumaproject.animationswipememo.domain.model.ListItem
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoBlock

data class RenderListItem(
    val itemId: String,
    val text: String,
    val indentLevel: Int,
    val visible: Boolean,
    val markerText: String,
    val orderedNumber: Int?,
    val hasChildren: Boolean,
    val canExpand: Boolean,
    val isExpanded: Boolean,
    val checked: Boolean,
    val textDecoration: TextDecoration?,
    val startPaddingDp: Float,
    val fontScale: Float
)

class ListBlockRenderUseCase {
    fun renderItems(block: MemoBlock): List<RenderListItem> {
        val appearance = block.listAppearance
        val items = block.listItems
        if (items.isEmpty()) return emptyList()

        val visibleFlags = computeVisibleFlags(items)
        val hasChildrenFlags = computeHasChildrenFlags(items)
        val orderCounters = mutableMapOf<Int, Int>()

        return items.mapIndexed { index, item ->
            val isVisible = visibleFlags[index]
            val hasChildren = hasChildrenFlags[index]
            val orderedNumber = if (item.itemType == ListItemType.ORDERED && isVisible) {
                val next = (orderCounters[item.indentLevel] ?: 0) + 1
                orderCounters[item.indentLevel] = next
                orderCounters.keys.filter { it > item.indentLevel }.toList().forEach(orderCounters::remove)
                next
            } else {
                null
            }
            val markerText = when (item.itemType) {
                ListItemType.ORDERED -> "${orderedNumber ?: 1}."
                ListItemType.UNORDERED -> "•"
                ListItemType.CHECKBOX -> if (item.checked) "☑" else "☐"
            }

            RenderListItem(
                itemId = item.id,
                text = item.text,
                indentLevel = item.indentLevel,
                visible = isVisible,
                markerText = markerText,
                orderedNumber = orderedNumber,
                hasChildren = hasChildren,
                canExpand = hasChildren,
                isExpanded = item.isExpanded,
                checked = item.checked,
                textDecoration = if (item.itemType == ListItemType.CHECKBOX && item.checked) {
                    TextDecoration.LineThrough
                } else {
                    null
                },
                startPaddingDp = item.indentLevel * (appearance?.indentStepDp ?: 16f),
                fontScale = resolveFontScale(
                    baseScale = appearance?.fontScale ?: 1f,
                    minScale = appearance?.minFontScale ?: 0.72f,
                    levelScaleStep = appearance?.levelScaleStep ?: 0.04f,
                    item = item
                )
            )
        }
    }

    fun visibleItems(block: MemoBlock): List<RenderListItem> {
        return renderItems(block).filter { it.visible }
    }

    private fun computeVisibleFlags(items: List<ListItem>): List<Boolean> {
        val visible = MutableList(items.size) { true }
        for (index in items.indices) {
            val current = items[index]
            var expectedAncestorLevel = current.indentLevel - 1
            var cursor = index - 1
            var isVisible = true

            while (cursor >= 0 && expectedAncestorLevel >= 0) {
                val candidate = items[cursor]
                if (candidate.indentLevel == expectedAncestorLevel) {
                    if (!candidate.isExpanded) {
                        isVisible = false
                        break
                    }
                    expectedAncestorLevel -= 1
                }
                cursor -= 1
            }
            visible[index] = isVisible
        }
        return visible
    }

    private fun computeHasChildrenFlags(items: List<ListItem>): List<Boolean> {
        return items.mapIndexed { index, item ->
            val next = items.getOrNull(index + 1)
            next != null && next.indentLevel > item.indentLevel
        }
    }

    private fun resolveFontScale(
        baseScale: Float,
        minScale: Float,
        levelScaleStep: Float,
        item: ListItem
    ): Float {
        val overrideScale = item.appearanceOverride?.fontScaleOverride
        val levelScale = baseScale - (item.indentLevel * levelScaleStep)
        return (overrideScale ?: levelScale).coerceIn(minScale, 2f)
    }
}

