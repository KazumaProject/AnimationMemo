package com.kazumaproject.animationswipememo.ui.editor

import com.kazumaproject.animationswipememo.domain.model.AppSettings
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.SavedDrawing

data class EditorUiState(
    val draft: MemoDraft? = null,
    val settings: AppSettings = AppSettings(),
    val savedDrawings: List<SavedDrawing> = emptyList(),
    val selectedBlockId: String? = null,
    val isEditorSheetVisible: Boolean = false,
    val isToolPaletteVisible: Boolean = true,
    val isDrawingLibraryVisible: Boolean = false,
    val isDrawingEditorVisible: Boolean = false,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val isExistingMemo: Boolean = false
) {
    val selectedBlock: MemoBlock?
        get() = draft?.blocks?.firstOrNull { it.id == selectedBlockId }
}

sealed interface EditorEffect {
    data class ShowMessage(val message: String) : EditorEffect
    data object PerformHaptic : EditorEffect
}
