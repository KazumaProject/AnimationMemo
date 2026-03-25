package com.kazumaproject.animationswipememo.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kazumaproject.animationswipememo.di.AppContainer
import com.kazumaproject.animationswipememo.domain.export.AnimationExporter
import com.kazumaproject.animationswipememo.domain.export.ExportRequest
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import com.kazumaproject.animationswipememo.domain.repository.MemoRepository
import com.kazumaproject.animationswipememo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val settingsRepository: SettingsRepository,
    private val animationExporter: AnimationExporter
) : ViewModel() {
    private val memoId: String? = savedStateHandle["memoId"]
    private val draftState = MutableStateFlow<MemoDraft?>(null)
    private val loadingState = MutableStateFlow(true)
    private val workingState = MutableStateFlow(false)
    private val existingMemoState = MutableStateFlow(false)
    private val selectedBlockIdState = MutableStateFlow<String?>(null)
    private val editorSheetVisibleState = MutableStateFlow(false)
    private val effects = MutableSharedFlow<EditorEffect>()

    val uiState: StateFlow<EditorUiState> = combine(
        combine(
            draftState,
            settingsRepository.settings,
            selectedBlockIdState,
            editorSheetVisibleState
        ) { draft, settings, selectedBlockId, isEditorSheetVisible ->
            PartialEditorUiState(
                draft = draft,
                settings = settings,
                selectedBlockId = selectedBlockId,
                isEditorSheetVisible = isEditorSheetVisible
            )
        },
        loadingState,
        workingState,
        existingMemoState
    ) { partial, isLoading, isWorking, isExisting ->
        EditorUiState(
            draft = partial.draft,
            settings = partial.settings,
            selectedBlockId = partial.selectedBlockId,
            isEditorSheetVisible = partial.isEditorSheetVisible,
            isLoading = isLoading,
            isWorking = isWorking,
            isExistingMemo = isExisting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditorUiState()
    )

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val loadedMemo = memoId?.let { memoRepository.getMemoById(it) }
            val draft = loadedMemo ?: MemoDraft.create(defaultAnimation = settings.defaultAnimation)
            draftState.value = draft
            existingMemoState.value = loadedMemo != null
            selectedBlockIdState.value = draft.blocks.firstOrNull()?.id
            editorSheetVisibleState.value = loadedMemo == null
            loadingState.value = false
        }
    }

    fun effects() = effects.asSharedFlow()

    fun addBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.create(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.28f + (draft.blocks.size * 0.12f)).coerceAtMost(0.8f)
        )
        draftState.value = draft.copy(
            blocks = draft.blocks + newBlock,
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = newBlock.id
        editorSheetVisibleState.value = true
    }

    fun selectBlock(blockId: String) {
        selectedBlockIdState.value = blockId
    }

    fun openBlockEditor(blockId: String) {
        selectedBlockIdState.value = blockId
        editorSheetVisibleState.value = true
    }

    fun startBlockDrag(blockId: String) {
        selectedBlockIdState.value = blockId
        editorSheetVisibleState.value = false
    }

    fun clearSelection() {
        selectedBlockIdState.value = null
    }

    fun showEditorSheet() {
        if (selectedBlockIdState.value == null) {
            emitMessage("Select a block first.")
            return
        }
        editorSheetVisibleState.value = true
    }

    fun hideEditorSheet() {
        editorSheetVisibleState.value = false
    }

    fun updateSelectedBlockText(text: String) {
        updateSelectedBlock { block ->
            block.copy(text = text.take(160))
        }
    }

    fun updateSelectedBlockAnimation(style: AnimationStyle) {
        updateSelectedBlock { block ->
            block.copy(animationStyle = style)
        }
    }

    fun updateSelectedBlockFontSize(fontSize: Float) {
        updateSelectedBlock { block ->
            block.copy(
                textStyle = block.textStyle.copy(
                    fontSize = fontSize.coerceIn(
                        TextStyleSetting.MIN_FONT_SIZE,
                        TextStyleSetting.MAX_FONT_SIZE
                    )
                )
            )
        }
    }

    fun moveSelectedBlock(deltaXNormalized: Float, deltaYNormalized: Float) {
        updateSelectedBlock { block ->
            block.copy(
                normalizedX = (block.normalizedX + deltaXNormalized).coerceIn(0.1f, 0.9f),
                normalizedY = (block.normalizedY + deltaYNormalized).coerceIn(0.14f, 0.9f)
            )
        }
    }

    fun deleteSelectedBlock() {
        val draft = draftState.value ?: return
        val selectedId = selectedBlockIdState.value ?: return
        val remaining = draft.blocks.filterNot { it.id == selectedId }
        val fallback = remaining.ifEmpty {
            listOf(MemoBlock.create(defaultAnimation = uiState.value.settings.defaultAnimation))
        }
        draftState.value = draft.copy(
            blocks = fallback,
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = fallback.firstOrNull()?.id
        editorSheetVisibleState.value = fallback.size == 1 && fallback.first().text.isBlank()
    }

    fun saveMemo() {
        val draft = draftState.value ?: return
        if (!draft.hasContent) {
            emitMessage("Add at least one text block before saving.")
            return
        }
        executeAction {
            memoRepository.upsertMemo(draft.copy(updatedAt = System.currentTimeMillis()))
            existingMemoState.value = true
            effects.emit(EditorEffect.PerformHaptic)
            effects.emit(EditorEffect.ShowMessage("Memo saved."))
        }
    }

    fun discardMemo() {
        val draft = draftState.value ?: return
        executeAction {
            if (existingMemoState.value) {
                memoRepository.deleteMemo(draft.id)
            }
            val freshDraft = MemoDraft.create(defaultAnimation = uiState.value.settings.defaultAnimation)
            draftState.value = freshDraft
            existingMemoState.value = false
            selectedBlockIdState.value = freshDraft.blocks.firstOrNull()?.id
            editorSheetVisibleState.value = true
            effects.emit(EditorEffect.PerformHaptic)
            effects.emit(EditorEffect.ShowMessage("Memo discarded."))
        }
    }

    fun exportGif(darkTheme: Boolean) {
        val draft = draftState.value ?: return
        if (!draft.hasContent) {
            emitMessage("Add at least one text block before exporting.")
            return
        }
        executeAction {
            val result = animationExporter.exportGif(
                ExportRequest(
                    memo = draft,
                    quality = uiState.value.settings.gifQuality,
                    darkTheme = darkTheme
                )
            )
            effects.emit(EditorEffect.PerformHaptic)
            effects.emit(EditorEffect.ShowMessage("GIF exported: ${result.displayName}"))
        }
    }

    fun exportPng(darkTheme: Boolean) {
        val draft = draftState.value ?: return
        if (!draft.hasContent) {
            emitMessage("Add at least one text block before exporting.")
            return
        }
        executeAction {
            val result = animationExporter.exportPng(
                ExportRequest(
                    memo = draft,
                    quality = uiState.value.settings.gifQuality,
                    darkTheme = darkTheme
                )
            )
            effects.emit(EditorEffect.PerformHaptic)
            effects.emit(EditorEffect.ShowMessage("PNG exported: ${result.displayName}"))
        }
    }

    private fun updateSelectedBlock(transform: (MemoBlock) -> MemoBlock) {
        val draft = draftState.value ?: return
        val selectedId = selectedBlockIdState.value ?: return
        draftState.value = draft.copy(
            blocks = draft.blocks.map { block ->
                if (block.id == selectedId) transform(block) else block
            },
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun executeAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            workingState.value = true
            runCatching { block() }
                .onFailure { throwable ->
                    effects.emit(EditorEffect.ShowMessage(throwable.message ?: "Something went wrong."))
                }
            workingState.value = false
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            effects.emit(EditorEffect.ShowMessage(message))
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    memoRepository = container.memoRepository,
                    settingsRepository = container.settingsRepository,
                    animationExporter = container.animationExporter
                )
            }
        }
    }
}

private data class PartialEditorUiState(
    val draft: MemoDraft?,
    val settings: com.kazumaproject.animationswipememo.domain.model.AppSettings,
    val selectedBlockId: String?,
    val isEditorSheetVisible: Boolean
)
