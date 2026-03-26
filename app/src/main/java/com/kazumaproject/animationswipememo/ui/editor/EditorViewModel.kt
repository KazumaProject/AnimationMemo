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
import com.kazumaproject.animationswipememo.domain.model.ListAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoFontFamily
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.SavedDrawing
import com.kazumaproject.animationswipememo.domain.model.StrokeData
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import com.kazumaproject.animationswipememo.domain.repository.MemoRepository
import com.kazumaproject.animationswipememo.domain.repository.SavedDrawingRepository
import com.kazumaproject.animationswipememo.domain.repository.SettingsRepository
import com.kazumaproject.animationswipememo.domain.usecase.ListBlockEditorUseCase
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
    private val animationExporter: AnimationExporter,
    private val savedDrawingRepository: SavedDrawingRepository
) : ViewModel() {
    private val listBlockEditorUseCase = ListBlockEditorUseCase()
    private val memoId: String? = savedStateHandle["memoId"]
    private val draftState = MutableStateFlow<MemoDraft?>(null)
    private val loadingState = MutableStateFlow(true)
    private val workingState = MutableStateFlow(false)
    private val existingMemoState = MutableStateFlow(false)
    private val selectedBlockIdState = MutableStateFlow<String?>(null)
    private val editorSheetVisibleState = MutableStateFlow(false)
    private val toolPaletteVisibleState = MutableStateFlow(true)
    private val drawingLibraryVisibleState = MutableStateFlow(false)
    private val drawingEditorVisibleState = MutableStateFlow(false)
    private val effects = MutableSharedFlow<EditorEffect>()

    val uiState: StateFlow<EditorUiState> = combine(
        combine(
            combine(
                draftState,
                settingsRepository.settings,
                savedDrawingRepository.observeSavedDrawings(),
                selectedBlockIdState,
                editorSheetVisibleState
            ) { draft, settings, savedDrawings, selectedBlockId, isEditorSheetVisible ->
                PartialEditorUiStateA(
                    draft = draft,
                    settings = settings,
                    savedDrawings = savedDrawings,
                    selectedBlockId = selectedBlockId,
                    isEditorSheetVisible = isEditorSheetVisible
                )
            },
            toolPaletteVisibleState,
            drawingLibraryVisibleState,
            drawingEditorVisibleState
        ) { partialA, isToolPaletteVisible, isDrawingLibraryVisible, isDrawingEditorVisible ->
            PartialEditorUiStateB(
                draft = partialA.draft,
                settings = partialA.settings,
                savedDrawings = partialA.savedDrawings,
                selectedBlockId = partialA.selectedBlockId,
                isEditorSheetVisible = partialA.isEditorSheetVisible,
                isToolPaletteVisible = isToolPaletteVisible,
                isDrawingLibraryVisible = isDrawingLibraryVisible,
                isDrawingEditorVisible = isDrawingEditorVisible
            )
        },
        loadingState,
        workingState,
        existingMemoState
    ) { partial, isLoading, isWorking, isExisting ->
        EditorUiState(
            draft = partial.draft,
            settings = partial.settings,
            savedDrawings = partial.savedDrawings,
            selectedBlockId = partial.selectedBlockId,
            isEditorSheetVisible = partial.isEditorSheetVisible,
            isToolPaletteVisible = partial.isToolPaletteVisible,
            isDrawingLibraryVisible = partial.isDrawingLibraryVisible,
            isDrawingEditorVisible = partial.isDrawingEditorVisible,
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
            settingsRepository.settings.collect { settings ->
                val draft = draftState.value ?: return@collect
                if (!existingMemoState.value && draft.paperStyle != settings.defaultPaperStyle) {
                    draftState.value = draft.copy(paperStyle = settings.defaultPaperStyle)
                }
            }
        }
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val loadedMemo = memoId?.let { memoRepository.getMemoById(it) }
            val draft = loadedMemo ?: MemoDraft.create(
                paperStyle = settings.defaultPaperStyle
            )
            existingMemoState.value = loadedMemo != null
            draftState.value = draft
            selectedBlockIdState.value = draft.blocks.firstOrNull()?.id
            editorSheetVisibleState.value = false
            loadingState.value = false
        }
    }

    fun effects() = effects.asSharedFlow()

    fun addTextBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createText(
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

    fun addImageBlock(imageUri: String, contentAspectRatio: Float) {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createImage(
            imageUri = imageUri,
            contentAspectRatio = contentAspectRatio,
            animationStyle = uiState.value.settings.defaultAnimation
        )
        draftState.value = draft.copy(
            blocks = draft.blocks + newBlock,
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = newBlock.id
        editorSheetVisibleState.value = true
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = false
    }

    fun addListBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createList(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.24f + (draft.blocks.size * 0.1f)).coerceAtMost(0.78f)
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

    fun toggleToolPaletteVisibility() {
        toolPaletteVisibleState.value = !toolPaletteVisibleState.value
    }

    fun openDrawingLibrary() {
        drawingLibraryVisibleState.value = true
        drawingEditorVisibleState.value = false
        editorSheetVisibleState.value = false
    }

    fun closeDrawingLibrary() {
        drawingLibraryVisibleState.value = false
    }

    fun openDrawingEditor() {
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = true
        editorSheetVisibleState.value = false
    }

    fun closeDrawingEditor() {
        drawingEditorVisibleState.value = false
    }

    fun insertSavedDrawing(drawing: SavedDrawing) {
        executeAction {
            insertDrawingBlock(
                strokes = drawing.strokes,
                widthFraction = drawing.widthFraction,
                heightFraction = drawing.heightFraction,
                saveMessage = "\"${drawing.name}\" inserted."
            )
        }
    }

    fun saveNewDrawingFromEditor(
        strokes: List<StrokeData>,
        widthFraction: Float,
        heightFraction: Float,
        saveToLibrary: Boolean,
        name: String
    ) {
        if (strokes.none { it.points.size > 1 }) {
            emitMessage("Draw something before saving.")
            return
        }

        executeAction {
            if (saveToLibrary) {
                val timestamp = System.currentTimeMillis()
                savedDrawingRepository.saveDrawing(
                    SavedDrawing(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name.ifBlank { "Drawing ${uiState.value.savedDrawings.size + 1}" },
                        strokes = strokes,
                        widthFraction = widthFraction,
                        heightFraction = heightFraction,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                )
            }
            insertDrawingBlock(
                strokes = strokes,
                widthFraction = widthFraction,
                heightFraction = heightFraction,
                saveMessage = if (saveToLibrary) {
                    "Drawing saved to memo and library."
                } else {
                    "Drawing inserted."
                }
            )
        }
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

    fun addListItem() {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.addListItem(block)
        }
    }

    fun updateListItemText(itemId: String, text: String) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.updateListItemText(block, itemId, text.take(160))
        }
    }

    fun removeListItem(itemId: String) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.removeListItem(block, itemId)
        }
    }

    fun toggleListItemChecked(itemId: String) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.toggleListItemChecked(block, itemId)
        }
    }

    fun toggleListItemCheckedFromCanvas(blockId: String, itemId: String) {
        val draft = draftState.value ?: return
        draftState.value = draft.copy(
            blocks = draft.blocks.map { block ->
                if (block.id == blockId && block.type == MemoBlockType.List) {
                    listBlockEditorUseCase.toggleListItemChecked(block, itemId)
                } else {
                    block
                }
            },
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = blockId
    }

    fun toggleListItemExpandedFromCanvas(blockId: String, itemId: String) {
        val draft = draftState.value ?: return
        draftState.value = draft.copy(
            blocks = draft.blocks.map { block ->
                if (block.id == blockId && block.type == MemoBlockType.List) {
                    listBlockEditorUseCase.toggleListItemExpanded(block, itemId)
                } else {
                    block
                }
            },
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = blockId
    }

    fun updateListItemType(itemId: String, type: ListItemType) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.updateListItemType(block, itemId, type)
        }
    }

    fun increaseListItemIndent(itemId: String) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.increaseIndent(block, itemId)
        }
    }

    fun decreaseListItemIndent(itemId: String) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.decreaseIndent(block, itemId)
        }
    }

    fun moveListItemUp(itemId: String) {
        updateSelectedListBlock { block ->
            val index = block.listItems.indexOfFirst { it.id == itemId }
            if (index <= 0) block else listBlockEditorUseCase.moveListItem(block, index, index - 1)
        }
    }

    fun moveListItemDown(itemId: String) {
        updateSelectedListBlock { block ->
            val index = block.listItems.indexOfFirst { it.id == itemId }
            if (index < 0 || index >= block.listItems.lastIndex) {
                block
            } else {
                listBlockEditorUseCase.moveListItem(block, index, index + 1)
            }
        }
    }

    fun toggleListItemExpanded(itemId: String) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.toggleListItemExpanded(block, itemId)
        }
    }

    fun updateSelectedListAppearance(appearance: ListAppearance) {
        updateSelectedListBlock { block ->
            listBlockEditorUseCase.updateListAppearance(block, appearance)
        }
    }

    fun updateMemoTitle(title: String) {
        val draft = draftState.value ?: return
        val sanitizedTitle = title
            .replace('\n', ' ')
            .trimStart()
            .take(60)
        draftState.value = draft.copy(
            title = sanitizedTitle,
            updatedAt = System.currentTimeMillis()
        )
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

    fun updateSelectedBlockFontFamily(fontFamily: MemoFontFamily) {
        updateSelectedBlock { block ->
            block.copy(textStyle = block.textStyle.copy(fontFamily = fontFamily))
        }
    }

    fun toggleSelectedBlockBold() {
        updateSelectedBlock { block ->
            block.copy(textStyle = block.textStyle.copy(isBold = !block.textStyle.isBold))
        }
    }

    fun toggleSelectedBlockItalic() {
        updateSelectedBlock { block ->
            block.copy(textStyle = block.textStyle.copy(isItalic = !block.textStyle.isItalic))
        }
    }

    fun toggleSelectedBlockUnderline() {
        updateSelectedBlock { block ->
            block.copy(textStyle = block.textStyle.copy(isUnderline = !block.textStyle.isUnderline))
        }
    }

    fun updateSelectedBlockWidth(widthFraction: Float) {
        updateSelectedBlock { block ->
            block.copy(widthFraction = widthFraction.coerceIn(0.12f, 0.9f))
        }
    }

    fun updateSelectedBlockHeight(heightFraction: Float) {
        updateSelectedBlock { block ->
            block.copy(heightFraction = heightFraction.coerceIn(0.12f, 0.9f))
        }
    }

    fun moveBlock(blockId: String, deltaXNormalized: Float, deltaYNormalized: Float) {
        val draft = draftState.value ?: return
        val minY = minimumBlockYFor(draft.paperStyle)
        draftState.value = draft.copy(
            blocks = draft.blocks.map { block ->
                if (block.id != blockId) {
                    block
                } else {
                    block.copy(
                        normalizedX = (block.normalizedX + deltaXNormalized).coerceIn(0.1f, 0.9f),
                        normalizedY = (block.normalizedY + deltaYNormalized).coerceIn(minY, 0.9f)
                    )
                }
            },
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = blockId
    }

    fun scaleBlock(blockId: String, scaleFactor: Float) {
        val draft = draftState.value ?: return
        val minY = minimumBlockYFor(draft.paperStyle)
        draftState.value = draft.copy(
            blocks = draft.blocks.map { block ->
                if (block.id != blockId) {
                    block
                } else {
                    block.scaledBy(scaleFactor = scaleFactor, minimumY = minY)
                }
            },
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = blockId
    }

    fun moveSelectedBlock(deltaXNormalized: Float, deltaYNormalized: Float) {
        val minY = minimumBlockYFor(draftState.value?.paperStyle ?: return)
        updateSelectedBlock { block ->
            block.copy(
                normalizedX = (block.normalizedX + deltaXNormalized).coerceIn(0.1f, 0.9f),
                normalizedY = (block.normalizedY + deltaYNormalized).coerceIn(minY, 0.9f)
            )
        }
    }

    fun deleteSelectedBlock() {
        val draft = draftState.value ?: return
        val selectedId = selectedBlockIdState.value ?: return
        val remaining = draft.blocks.filterNot { it.id == selectedId }
        val shouldKeepEditorOpen = editorSheetVisibleState.value && remaining.isNotEmpty()
        draftState.value = draft.copy(
            blocks = remaining,
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = remaining.firstOrNull()?.id
        editorSheetVisibleState.value = shouldKeepEditorOpen
    }

    fun saveMemo() {
        val draft = draftState.value ?: return
        if (!draft.hasContent) {
            emitMessage("Add text, image, list, or handwriting before saving.")
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
            val freshDraft = MemoDraft.create(
                paperStyle = uiState.value.settings.defaultPaperStyle
            )
            draftState.value = freshDraft
            existingMemoState.value = false
            selectedBlockIdState.value = freshDraft.blocks.firstOrNull()?.id
            editorSheetVisibleState.value = false
            drawingLibraryVisibleState.value = false
            drawingEditorVisibleState.value = false
            effects.emit(EditorEffect.PerformHaptic)
            effects.emit(EditorEffect.ShowMessage("Memo discarded."))
        }
    }

    fun createNewMemo() {
        val freshDraft = MemoDraft.create(
            paperStyle = uiState.value.settings.defaultPaperStyle
        )
        draftState.value = freshDraft
        existingMemoState.value = false
        selectedBlockIdState.value = freshDraft.blocks.firstOrNull()?.id
        editorSheetVisibleState.value = false
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = false
    }

    fun exportGif(darkTheme: Boolean) {
        val draft = draftState.value ?: return
        if (!draft.hasContent) {
            emitMessage("Add text, image, list, or handwriting before exporting.")
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
            emitMessage("Add text, image, list, or handwriting before exporting.")
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

    private fun updateSelectedListBlock(transform: (MemoBlock) -> MemoBlock) {
        updateSelectedBlock { block ->
            if (block.type == MemoBlockType.List) {
                transform(block)
            } else {
                block
            }
        }
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

    private fun minimumBlockYFor(paperStyle: PaperStyle): Float {
        return if (paperStyle.supportsTopAlignedBlocks) 0.02f else 0.14f
    }

    private suspend fun insertDrawingBlock(
        strokes: List<StrokeData>,
        widthFraction: Float,
        heightFraction: Float,
        saveMessage: String
    ) {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createDrawing(
            x = 0.5f,
            y = 0.48f,
            widthFraction = widthFraction,
            heightFraction = heightFraction,
            strokes = strokes,
            animationStyle = uiState.value.settings.defaultAnimation
        )
        draftState.value = draft.copy(
            blocks = draft.blocks + newBlock,
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = newBlock.id
        editorSheetVisibleState.value = true
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = false
        effects.emit(EditorEffect.PerformHaptic)
        effects.emit(EditorEffect.ShowMessage(saveMessage))
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    memoRepository = container.memoRepository,
                    settingsRepository = container.settingsRepository,
                    animationExporter = container.animationExporter,
                    savedDrawingRepository = container.savedDrawingRepository
                )
            }
        }
    }
}

private fun MemoBlock.scaledBy(
    scaleFactor: Float,
    minimumY: Float
): MemoBlock {
    val safeScale = scaleFactor.takeIf { it.isFinite() && it > 0f } ?: return this
    val safeWidth = widthFraction.coerceAtLeast(0.0001f)
    val safeHeight = heightFraction.coerceAtLeast(0.0001f)
    val safeFontSize = textStyle.fontSize.coerceAtLeast(0.0001f)
    val minScale = buildList {
        add(MIN_BLOCK_FRACTION / safeWidth)
        add(MIN_BLOCK_FRACTION / safeHeight)
        if (isText || isList) {
            add(TextStyleSetting.MIN_FONT_SIZE / safeFontSize)
        }
    }.maxOrNull() ?: 1f
    val maxScale = buildList {
        add(MAX_BLOCK_FRACTION / safeWidth)
        add(MAX_BLOCK_FRACTION / safeHeight)
        if (isText || isList) {
            add(TextStyleSetting.MAX_FONT_SIZE / safeFontSize)
        }
    }.minOrNull() ?: 1f
    val appliedScale = safeScale.coerceIn(minScale, maxScale)
    val scaledWidth = widthFraction * appliedScale
    val scaledHeight = heightFraction * appliedScale

    val adjustedY = if (isText || isList) {
        val centerY = normalizedY + (heightFraction / 2f)
        (centerY - (scaledHeight / 2f)).coerceIn(minimumY, 0.9f)
    } else {
        normalizedY
    }

    return copy(
        normalizedY = adjustedY,
        widthFraction = scaledWidth,
        heightFraction = scaledHeight,
        textStyle = if (isText || isList) {
            textStyle.copy(fontSize = textStyle.fontSize * appliedScale)
        } else {
            textStyle
        }
    )
}

private const val MIN_BLOCK_FRACTION = 0.12f
private const val MAX_BLOCK_FRACTION = 0.9f

private data class PartialEditorUiStateA(
    val draft: MemoDraft?,
    val settings: com.kazumaproject.animationswipememo.domain.model.AppSettings,
    val savedDrawings: List<SavedDrawing>,
    val selectedBlockId: String?,
    val isEditorSheetVisible: Boolean
)

private data class PartialEditorUiStateB(
    val draft: MemoDraft?,
    val settings: com.kazumaproject.animationswipememo.domain.model.AppSettings,
    val savedDrawings: List<SavedDrawing>,
    val selectedBlockId: String?,
    val isEditorSheetVisible: Boolean,
    val isToolPaletteVisible: Boolean,
    val isDrawingLibraryVisible: Boolean,
    val isDrawingEditorVisible: Boolean
)
