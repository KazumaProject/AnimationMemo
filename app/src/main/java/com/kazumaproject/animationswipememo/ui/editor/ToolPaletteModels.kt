package com.kazumaproject.animationswipememo.ui.editor

data class PaletteItemDefinition(
    val id: String,
    val title: String,
    val shortTitle: String,
    val icon: PaletteIcon,
    val group: PaletteGroup,
    val action: PaletteAction,
    val defaultOrder: Int
)

data class PaletteItemState(
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val visible: Boolean = true,
    val usageCount: Int = 0,
    val badge: String? = null
)

sealed class PaletteUiModel {
    data class Header(
        val id: String,
        val title: String,
        val order: Int
    ) : PaletteUiModel()

    data class Item(
        val id: String,
        val title: String,
        val shortTitle: String,
        val icon: PaletteIcon,
        val enabled: Boolean,
        val selected: Boolean,
        val badge: String?,
        val group: PaletteGroup,
        val action: PaletteAction
    ) : PaletteUiModel()
}

sealed class PaletteAction {
    data object AddText : PaletteAction()
    data object AddImage : PaletteAction()
    data object AddList : PaletteAction()
    data object OpenDrawingLibrary : PaletteAction()
    data object EditSelected : PaletteAction()
    data object ExportGif : PaletteAction()
    data object ExportPng : PaletteAction()
    data object DiscardMemo : PaletteAction()
}

enum class PaletteIcon {
    AddText,
    AddImage,
    AddList,
    Handwriting,
    Edit,
    Export,
    ExportPng,
    Discard
}

enum class PaletteGroup(val headerTitle: String, val order: Int) {
    Frequent(headerTitle = "Frequent", order = 0),
    Insert(headerTitle = "Insert", order = 1),
    Edit(headerTitle = "Edit", order = 2),
    Export(headerTitle = "Export", order = 3),
    Danger(headerTitle = "Danger", order = 4)
}

object ToolPaletteCatalog {
    private val definitions = listOf(
        PaletteItemDefinition(
            id = "add_text",
            title = "Add text block",
            shortTitle = "Text",
            icon = PaletteIcon.AddText,
            group = PaletteGroup.Insert,
            action = PaletteAction.AddText,
            defaultOrder = 0
        ),
        PaletteItemDefinition(
            id = "add_image",
            title = "Insert image",
            shortTitle = "Image",
            icon = PaletteIcon.AddImage,
            group = PaletteGroup.Insert,
            action = PaletteAction.AddImage,
            defaultOrder = 1
        ),
        PaletteItemDefinition(
            id = "add_list",
            title = "Insert list",
            shortTitle = "List",
            icon = PaletteIcon.AddList,
            group = PaletteGroup.Insert,
            action = PaletteAction.AddList,
            defaultOrder = 2
        ),
        PaletteItemDefinition(
            id = "handwriting",
            title = "Insert handwriting",
            shortTitle = "Handwriting",
            icon = PaletteIcon.Handwriting,
            group = PaletteGroup.Insert,
            action = PaletteAction.OpenDrawingLibrary,
            defaultOrder = 3
        ),
        PaletteItemDefinition(
            id = "edit_selected",
            title = "Edit selected block",
            shortTitle = "Edit",
            icon = PaletteIcon.Edit,
            group = PaletteGroup.Edit,
            action = PaletteAction.EditSelected,
            defaultOrder = 0
        ),
        PaletteItemDefinition(
            id = "export_gif",
            title = "Export GIF",
            shortTitle = "GIF",
            icon = PaletteIcon.Export,
            group = PaletteGroup.Export,
            action = PaletteAction.ExportGif,
            defaultOrder = 0
        ),
        PaletteItemDefinition(
            id = "export_png",
            title = "Export PNG",
            shortTitle = "PNG",
            icon = PaletteIcon.ExportPng,
            group = PaletteGroup.Export,
            action = PaletteAction.ExportPng,
            defaultOrder = 1
        ),
        PaletteItemDefinition(
            id = "discard",
            title = "Discard current memo",
            shortTitle = "Discard",
            icon = PaletteIcon.Discard,
            group = PaletteGroup.Danger,
            action = PaletteAction.DiscardMemo,
            defaultOrder = 0
        )
    )

    private val frequentPreset = setOf(
        PaletteAction.AddText,
        PaletteAction.AddList,
        PaletteAction.EditSelected,
        PaletteAction.ExportGif
    )

    fun buildUiModels(
        selectedBlockId: String?,
        hasContent: Boolean,
        usageCountByAction: Map<PaletteAction, Int> = emptyMap()
    ): List<PaletteUiModel> {
        val stateByAction = buildStateByAction(
            selectedBlockId = selectedBlockId,
            hasContent = hasContent,
            usageCountByAction = usageCountByAction
        )

        val visibleItems = definitions.mapNotNull { definition ->
            val state = stateByAction[definition.action] ?: PaletteItemState()
            if (!state.visible) {
                null
            } else {
                PaletteUiModel.Item(
                    id = definition.id,
                    title = definition.title,
                    shortTitle = definition.shortTitle,
                    icon = definition.icon,
                    enabled = state.enabled,
                    selected = state.selected,
                    badge = state.badge,
                    group = definition.group,
                    action = definition.action
                )
            }
        }

        val frequentItems = visibleItems
            .filter { it.action in frequentPreset }
            .sortedByDescending { usageCountByAction[it.action] ?: 0 }
            .take(4)
            .takeIf { it.any { item -> (usageCountByAction[item.action] ?: 0) > 0 } }
            .orEmpty()

        val grouped = visibleItems
            .groupBy { it.group }
            .toSortedMap(compareBy<PaletteGroup> { it.order })

        val result = mutableListOf<PaletteUiModel>()
        if (frequentItems.isNotEmpty()) {
            result += PaletteUiModel.Header(id = "header_frequent", title = PaletteGroup.Frequent.headerTitle, order = 0)
            result += frequentItems
        }

        grouped.forEach { (group, items) ->
            if (items.isEmpty()) return@forEach
            result += PaletteUiModel.Header(
                id = "header_${group.name.lowercase()}",
                title = group.headerTitle,
                order = group.order
            )
            result += items.sortedBy { definitionFor(it.action).defaultOrder }
        }

        return result
    }

    private fun buildStateByAction(
        selectedBlockId: String?,
        hasContent: Boolean,
        usageCountByAction: Map<PaletteAction, Int>
    ): Map<PaletteAction, PaletteItemState> {
        return definitions.associate { definition ->
            val enabled = when (definition.action) {
                PaletteAction.EditSelected -> selectedBlockId != null
                PaletteAction.ExportGif,
                PaletteAction.ExportPng,
                PaletteAction.DiscardMemo -> hasContent
                else -> true
            }
            val selected = definition.action == PaletteAction.EditSelected && selectedBlockId != null
            definition.action to PaletteItemState(
                enabled = enabled,
                selected = selected,
                usageCount = usageCountByAction[definition.action] ?: 0
            )
        }
    }

    private fun definitionFor(action: PaletteAction): PaletteItemDefinition {
        return definitions.first { it.action == action }
    }
}

