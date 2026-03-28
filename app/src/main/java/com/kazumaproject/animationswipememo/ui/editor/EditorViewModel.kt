package com.kazumaproject.animationswipememo.ui.editor

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kazumaproject.animationswipememo.data.network.LinkPreviewMetadata
import com.kazumaproject.animationswipememo.data.network.LinkPreviewMetadataFetcher
import com.kazumaproject.animationswipememo.di.AppContainer
import com.kazumaproject.animationswipememo.domain.export.AnimationExporter
import com.kazumaproject.animationswipememo.domain.export.ExportRequest
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.ConversationItem
import com.kazumaproject.animationswipememo.domain.model.ConversationRole
import com.kazumaproject.animationswipememo.domain.model.HeadingLevel
import com.kazumaproject.animationswipememo.domain.model.ListAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockPayload
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoFontFamily
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.SavedDrawing
import com.kazumaproject.animationswipememo.domain.model.StrokeData
import com.kazumaproject.animationswipememo.domain.model.TableRow
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import com.kazumaproject.animationswipememo.domain.model.ToggleChildBlock
import com.kazumaproject.animationswipememo.domain.repository.MemoRepository
import com.kazumaproject.animationswipememo.domain.repository.SavedDrawingRepository
import com.kazumaproject.animationswipememo.domain.repository.SettingsRepository
import com.kazumaproject.animationswipememo.domain.usecase.ListBlockEditorUseCase
import com.kazumaproject.animationswipememo.ui.components.render.supportedCodeLanguages
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val settingsRepository: SettingsRepository,
    private val animationExporter: AnimationExporter,
    private val savedDrawingRepository: SavedDrawingRepository
) : ViewModel() {
    private val listBlockEditorUseCase = ListBlockEditorUseCase()
    private val linkPreviewMetadataFetcher = LinkPreviewMetadataFetcher()
    private val memoId: String? = savedStateHandle["memoId"]
    private val draftState = MutableStateFlow<MemoDraft?>(null)
    private val loadingState = MutableStateFlow(true)
    private val workingState = MutableStateFlow(false)
    private val existingMemoState = MutableStateFlow(false)
    private val selectedBlockIdState = MutableStateFlow<String?>(null)
    private val editorSheetVisibleState = MutableStateFlow(false)
    private val toolPaletteVisibleState = MutableStateFlow(false)
    private val fabVisibleState = MutableStateFlow(true)
    private val fabExpandedState = MutableStateFlow(true)
    private val drawingLibraryVisibleState = MutableStateFlow(false)
    private val drawingEditorVisibleState = MutableStateFlow(false)
    private val codeFullscreenBlockIdState = MutableStateFlow<String?>(null)
    private val linkMetadataLoadingBlockIdState = MutableStateFlow<String?>(null)
    private val effects = MutableSharedFlow<EditorEffect>()
    private var linkMetadataFetchJob: Job? = null

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
            combine(
                combine(
                    toolPaletteVisibleState,
                    fabVisibleState,
                    fabExpandedState,
                    drawingLibraryVisibleState,
                    drawingEditorVisibleState
                ) { isToolPaletteVisible, isFabVisible, isFabExpanded, isDrawingLibraryVisible, isDrawingEditorVisible ->
                    PartialEditorVisibility(
                        isToolPaletteVisible = isToolPaletteVisible,
                        isFabVisible = isFabVisible,
                        isFabExpanded = isFabExpanded,
                        isDrawingLibraryVisible = isDrawingLibraryVisible,
                        isDrawingEditorVisible = isDrawingEditorVisible,
                        codeFullscreenBlockId = null
                    )
                },
                codeFullscreenBlockIdState
            ) { visibility, codeFullscreenBlockId ->
                visibility.copy(codeFullscreenBlockId = codeFullscreenBlockId)
            }
        ) { partialA, visibility ->
            PartialEditorUiStateB(
                draft = partialA.draft,
                settings = partialA.settings,
                savedDrawings = partialA.savedDrawings,
                selectedBlockId = partialA.selectedBlockId,
                isEditorSheetVisible = partialA.isEditorSheetVisible,
                isToolPaletteVisible = visibility.isToolPaletteVisible,
                isFabVisible = visibility.isFabVisible,
                isFabExpanded = visibility.isFabExpanded,
                isDrawingLibraryVisible = visibility.isDrawingLibraryVisible,
                isDrawingEditorVisible = visibility.isDrawingEditorVisible,
                codeFullscreenBlockId = visibility.codeFullscreenBlockId
            )
        },
        loadingState,
        workingState,
        existingMemoState,
        linkMetadataLoadingBlockIdState
    ) { partial, isLoading, isWorking, isExisting, linkMetadataLoadingBlockId ->
        EditorUiState(
            draft = partial.draft,
            settings = partial.settings,
            savedDrawings = partial.savedDrawings,
            selectedBlockId = partial.selectedBlockId,
            isEditorSheetVisible = partial.isEditorSheetVisible,
            isToolPaletteVisible = partial.isToolPaletteVisible,
            isFabVisible = partial.isFabVisible,
            isFabExpanded = partial.isFabExpanded,
            isDrawingLibraryVisible = partial.isDrawingLibraryVisible,
            isDrawingEditorVisible = partial.isDrawingEditorVisible,
            codeFullscreenBlockId = partial.codeFullscreenBlockId,
            linkMetadataLoadingBlockId = linkMetadataLoadingBlockId,
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
            fabExpandedState.value = false
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
        toolPaletteVisibleState.value = false
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
        toolPaletteVisibleState.value = false
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
        toolPaletteVisibleState.value = false
    }

    fun addHeadingBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createHeading(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.2f + (draft.blocks.size * 0.09f)).coerceAtMost(0.76f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addToggleBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createToggle(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.24f + (draft.blocks.size * 0.08f)).coerceAtMost(0.8f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addQuoteBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createQuote(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.26f + (draft.blocks.size * 0.08f)).coerceAtMost(0.8f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addCodeBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createCode(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.28f + (draft.blocks.size * 0.08f)).coerceAtMost(0.82f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addDividerBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createDivider(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.32f + (draft.blocks.size * 0.06f)).coerceAtMost(0.84f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addLinkCardBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createLinkCard(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.34f + (draft.blocks.size * 0.06f)).coerceAtMost(0.84f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addTableBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createTable(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.34f + (draft.blocks.size * 0.07f)).coerceAtMost(0.84f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addConversationBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createConversation(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.34f + (draft.blocks.size * 0.07f)).coerceAtMost(0.84f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun addLatexBlock() {
        val draft = draftState.value ?: return
        val newBlock = MemoBlock.createLatex(
            defaultAnimation = uiState.value.settings.defaultAnimation,
            y = (0.3f + (draft.blocks.size * 0.07f)).coerceAtMost(0.82f)
        )
        insertBlockAndOpenEditor(draft = draft, newBlock = newBlock)
    }

    fun selectBlock(blockId: String) {
        selectedBlockIdState.value = blockId
    }

    fun openBlockEditor(blockId: String) {
        val exists = draftState.value?.blocks?.any { it.id == blockId } == true
        Log.d(
            EDITOR_VIEW_MODEL_TAG,
            "openBlockEditor start: blockId=$blockId, exists=$exists, selectedBefore=${selectedBlockIdState.value}, sheetVisibleBefore=${editorSheetVisibleState.value}"
        )
        selectedBlockIdState.value = blockId
        closeCodeFullscreen()
        editorSheetVisibleState.value = true
        toolPaletteVisibleState.value = false
        Log.d(
            EDITOR_VIEW_MODEL_TAG,
            "openBlockEditor end: selectedAfter=${selectedBlockIdState.value}, sheetVisibleAfter=${editorSheetVisibleState.value}, toolPaletteVisibleAfter=${toolPaletteVisibleState.value}"
        )
    }

    fun startBlockDrag(blockId: String) {
        selectedBlockIdState.value = blockId
        closeCodeFullscreen()
        editorSheetVisibleState.value = false
    }

    fun clearSelection() {
        selectedBlockIdState.value = null
        closeCodeFullscreen()
    }

    fun toggleToolPaletteVisibility() {
        toggleFabVisibility()
    }

    fun toggleFabVisibility() {
        fabVisibleState.value = !fabVisibleState.value
        if (fabVisibleState.value) {
            fabExpandedState.value = true
        } else {
            fabExpandedState.value = false
        }
    }

    fun toggleFabExpansion() {
        if (!fabVisibleState.value) {
            fabVisibleState.value = true
        }
        fabExpandedState.value = !fabExpandedState.value
    }

    fun collapseFabActions() {
        fabExpandedState.value = false
    }

    fun showToolPalette() {
        toolPaletteVisibleState.value = true
        fabExpandedState.value = false
        editorSheetVisibleState.value = false
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = false
        codeFullscreenBlockIdState.value = null
    }

    fun hideToolPalette() {
        toolPaletteVisibleState.value = false
    }

    fun openDrawingLibrary() {
        drawingLibraryVisibleState.value = true
        drawingEditorVisibleState.value = false
        editorSheetVisibleState.value = false
        toolPaletteVisibleState.value = false
        codeFullscreenBlockIdState.value = null
    }

    fun closeDrawingLibrary() {
        drawingLibraryVisibleState.value = false
        fabExpandedState.value = false
    }

    fun openDrawingEditor() {
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = true
        editorSheetVisibleState.value = false
        toolPaletteVisibleState.value = false
        codeFullscreenBlockIdState.value = null
    }

    fun closeDrawingEditor() {
        drawingEditorVisibleState.value = false
        fabExpandedState.value = false
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
        closeCodeFullscreen()
        if (selectedBlockIdState.value == null) {
            Log.d(EDITOR_VIEW_MODEL_TAG, "showEditorSheet: skipped because selectedBlockId is null")
            emitMessage("Select a block first.")
            return
        }
        Log.d(
            EDITOR_VIEW_MODEL_TAG,
            "showEditorSheet: opening editor for blockId=${selectedBlockIdState.value}"
        )
        editorSheetVisibleState.value = true
        toolPaletteVisibleState.value = false
        fabExpandedState.value = false
    }

    fun openEditorFromFab() {
        Log.d(
            EDITOR_VIEW_MODEL_TAG,
            "openEditorFromFab: selectedBlockId=${selectedBlockIdState.value}, blockCount=${draftState.value?.blocks?.size ?: 0}"
        )
        val currentSelected = selectedBlockIdState.value
        if (currentSelected != null) {
            showEditorSheet()
            return
        }
        val firstBlockId = draftState.value?.blocks?.firstOrNull()?.id
        if (firstBlockId == null) {
            Log.d(EDITOR_VIEW_MODEL_TAG, "openEditorFromFab: no blocks available, cannot open editor")
            emitMessage("No block to edit. Add a block first.")
            return
        }
        Log.d(EDITOR_VIEW_MODEL_TAG, "openEditorFromFab: fallback selecting first blockId=$firstBlockId")
        selectedBlockIdState.value = firstBlockId
        showEditorSheet()
    }

    fun hideEditorSheet() {
        Log.d(EDITOR_VIEW_MODEL_TAG, "hideEditorSheet: hiding editor sheet")
        editorSheetVisibleState.value = false
        fabExpandedState.value = false
    }

    fun openCodeFullscreen(blockId: String) {
        val block = draftState.value?.blocks?.firstOrNull { it.id == blockId } ?: return
        if (block.type != MemoBlockType.Code) return
        selectedBlockIdState.value = blockId
        editorSheetVisibleState.value = false
        toolPaletteVisibleState.value = false
        fabExpandedState.value = false
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = false
        codeFullscreenBlockIdState.value = blockId
    }

    fun closeCodeFullscreen() {
        codeFullscreenBlockIdState.value = null
    }

    fun updateSelectedBlockText(text: String) {
        updateSelectedBlock { block ->
            val maxLength = if (block.payload is MemoBlockPayload.Code) 4_000 else 400
            val sanitized = text.take(maxLength)
            val nextPayload = when (val payload = block.payload) {
                is MemoBlockPayload.Heading -> payload.copy(text = sanitized)
                is MemoBlockPayload.Quote -> payload.copy(text = sanitized)
                is MemoBlockPayload.Code -> payload.copy(code = sanitized)
                is MemoBlockPayload.Latex -> payload.copy(expression = sanitized)
                is MemoBlockPayload.Toggle -> payload.copy(title = sanitized)
                else -> payload
            }
            block.copy(text = sanitized, payload = nextPayload)
        }
    }

    fun updateHeadingLevel(level: HeadingLevel) {
        updateSelectedBlock { block ->
            val heading = block.payload as? MemoBlockPayload.Heading ?: return@updateSelectedBlock block
            block.copy(payload = heading.copy(level = level))
        }
    }

    fun updateToggleInitiallyExpanded(initiallyExpanded: Boolean) {
        updateSelectedBlock { block ->
            val toggle = block.payload as? MemoBlockPayload.Toggle ?: return@updateSelectedBlock block
            block.copy(payload = toggle.copy(initiallyExpanded = initiallyExpanded))
        }
    }

    fun addToggleChild() {
        updateSelectedBlock { block ->
            val toggle = block.payload as? MemoBlockPayload.Toggle ?: return@updateSelectedBlock block
            block.copy(payload = toggle.copy(childBlocks = toggle.childBlocks + ToggleChildBlock()))
        }
    }

    fun updateToggleChildText(childId: String, text: String) {
        updateSelectedBlock { block ->
            val toggle = block.payload as? MemoBlockPayload.Toggle ?: return@updateSelectedBlock block
            block.copy(
                payload = toggle.copy(
                    childBlocks = toggle.childBlocks.map { child ->
                        if (child.id == childId) child.copy(text = text.take(300)) else child
                    }
                )
            )
        }
    }

    fun removeToggleChild(childId: String) {
        updateSelectedBlock { block ->
            val toggle = block.payload as? MemoBlockPayload.Toggle ?: return@updateSelectedBlock block
            block.copy(payload = toggle.copy(childBlocks = toggle.childBlocks.filterNot { it.id == childId }))
        }
    }

    fun updateCodeLanguage(language: String) {
        var persistedLanguage = ""
        updateSelectedBlock { block ->
            val code = block.payload as? MemoBlockPayload.Code ?: return@updateSelectedBlock block
            val supported = supportedCodeLanguages()
            val normalized = supported.firstOrNull { it.equals(language.trim(), ignoreCase = true) }
                ?: language.take(40).ifBlank { "Plain Text" }
            persistedLanguage = normalized
            block.copy(payload = code.copy(language = normalized))
        }
        if (persistedLanguage.isNotBlank()) {
            val nextRecent = buildRecentCodeLanguages(
                selectedLanguage = persistedLanguage,
                currentRecent = uiState.value.settings.recentCodeLanguages
            )
            viewModelScope.launch {
                settingsRepository.updateRecentCodeLanguages(nextRecent)
            }
        }
    }

    private fun buildRecentCodeLanguages(
        selectedLanguage: String,
        currentRecent: List<String>
    ): List<String> {
        val supported = supportedCodeLanguages()
        val normalized = supported.firstOrNull { it.equals(selectedLanguage.trim(), ignoreCase = true) }
            ?: return currentRecent.take(5)
        return buildList {
            add(normalized)
            currentRecent.forEach { existing ->
                val resolved = supported.firstOrNull { it.equals(existing, ignoreCase = true) }
                if (resolved != null && !resolved.equals(normalized, ignoreCase = true)) {
                    add(resolved)
                }
            }
        }.distinctBy { it.lowercase() }.take(5)
    }

    fun updateLinkCard(
        url: String,
        title: String,
        description: String,
        imageUrl: String,
        faviconUrl: String
    ) {
        val sanitizedUrl = url.take(400)
        val sanitizedTitle = title.take(120)
        val sanitizedDescription = description.take(300)
        val sanitizedImageUrl = imageUrl.take(400)
        val sanitizedFaviconUrl = faviconUrl.take(400)

        var previousUrl = ""
        var selectedBlockId: String? = null
        updateSelectedBlock { block ->
            val link = block.payload as? MemoBlockPayload.LinkCard ?: return@updateSelectedBlock block
            previousUrl = link.url.trim()
            selectedBlockId = block.id
            block.copy(
                payload = link.copy(
                    url = sanitizedUrl,
                    title = sanitizedTitle,
                    description = sanitizedDescription,
                    imageUrl = sanitizedImageUrl,
                    faviconUrl = sanitizedFaviconUrl
                )
            )
        }

        val blockId = selectedBlockId ?: return
        val normalizedCurrentUrl = sanitizedUrl.trim()
        val hasAnyMissingMetadata = sanitizedTitle.isBlank() ||
            sanitizedDescription.isBlank() ||
            sanitizedImageUrl.isBlank() ||
            sanitizedFaviconUrl.isBlank()

        if (normalizedCurrentUrl.isBlank()) {
            cancelLinkMetadataFetch(clearForBlockId = blockId)
            return
        }

        val urlChanged = normalizedCurrentUrl != previousUrl
        if (urlChanged && hasAnyMissingMetadata) {
            scheduleLinkMetadataFetch(
                blockId = blockId,
                rawUrl = normalizedCurrentUrl
            )
        } else {
            cancelLinkMetadataFetch(clearForBlockId = blockId)
        }
    }

    private fun scheduleLinkMetadataFetch(blockId: String, rawUrl: String) {
        cancelLinkMetadataFetch()
        var launchedJob: Job? = null
        launchedJob = viewModelScope.launch {
            linkMetadataLoadingBlockIdState.value = blockId
            try {
                delay(LINK_METADATA_FETCH_DEBOUNCE_MS)
                val normalizedUrl = LinkPreviewMetadataFetcher.normalizeUrl(rawUrl) ?: return@launch
                val metadata = linkPreviewMetadataFetcher.fetch(normalizedUrl) ?: return@launch
                applyFetchedLinkMetadata(
                    blockId = blockId,
                    requestedUrl = normalizedUrl,
                    metadata = metadata
                )
            } finally {
                if (linkMetadataFetchJob == launchedJob) {
                    linkMetadataLoadingBlockIdState.value = null
                }
            }
        }
        linkMetadataFetchJob = launchedJob
    }

    private fun cancelLinkMetadataFetch(clearForBlockId: String? = null) {
        linkMetadataFetchJob?.cancel()
        linkMetadataFetchJob = null
        if (clearForBlockId == null || linkMetadataLoadingBlockIdState.value == clearForBlockId) {
            linkMetadataLoadingBlockIdState.value = null
        }
    }

    private fun applyFetchedLinkMetadata(
        blockId: String,
        requestedUrl: String,
        metadata: LinkPreviewMetadata
    ) {
        val draft = draftState.value ?: return
        val nextBlocks = draft.blocks.map { block ->
            if (block.id != blockId || block.type != MemoBlockType.LinkCard) {
                block
            } else {
                val payload = block.payload as? MemoBlockPayload.LinkCard ?: return@map block
                val activeUrl = LinkPreviewMetadataFetcher.normalizeUrl(payload.url) ?: payload.url.trim()
                if (!activeUrl.equals(requestedUrl, ignoreCase = true)) {
                    return@map block
                }

                payload.copy(
                    title = payload.title.ifBlank { metadata.title.take(120) },
                    description = payload.description.ifBlank { metadata.description.take(300) },
                    imageUrl = payload.imageUrl.ifBlank { metadata.imageUrl.take(400) },
                    faviconUrl = payload.faviconUrl.ifBlank { metadata.faviconUrl.take(400) }
                ).let { updatedPayload ->
                    if (updatedPayload == payload) block else block.copy(payload = updatedPayload)
                }
            }
        }

        if (nextBlocks != draft.blocks) {
            draftState.value = draft.copy(
                blocks = nextBlocks,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun updateTableCell(rowId: String, columnIndex: Int, value: String) {
        updateSelectedBlock { block ->
            val table = block.payload as? MemoBlockPayload.Table ?: return@updateSelectedBlock block
            block.copy(
                payload = table.copy(
                    rows = table.rows.map { row ->
                        if (row.id != rowId) {
                            row
                        } else {
                            row.copy(
                                cells = row.cells.mapIndexed { index, cell ->
                                    if (index == columnIndex) value.take(120) else cell
                                }
                            )
                        }
                    }
                )
            )
        }
    }

    fun addTableRow() {
        updateSelectedBlock { block ->
            val table = block.payload as? MemoBlockPayload.Table ?: return@updateSelectedBlock block
            val columnCount = table.rows.maxOfOrNull { it.cells.size } ?: 2
            block.copy(payload = table.copy(rows = table.rows + TableRow(cells = List(columnCount) { "" })))
        }
    }

    fun removeTableRow(rowId: String) {
        updateSelectedBlock { block ->
            val table = block.payload as? MemoBlockPayload.Table ?: return@updateSelectedBlock block
            val nextRows = table.rows.filterNot { it.id == rowId }
            block.copy(payload = table.copy(rows = if (nextRows.isEmpty()) table.rows else nextRows))
        }
    }

    fun addTableColumn() {
        updateSelectedBlock { block ->
            val table = block.payload as? MemoBlockPayload.Table ?: return@updateSelectedBlock block
            block.copy(
                payload = table.copy(rows = table.rows.map { row -> row.copy(cells = row.cells + "") })
            )
        }
    }

    fun removeTableColumn(columnIndex: Int) {
        updateSelectedBlock { block ->
            val table = block.payload as? MemoBlockPayload.Table ?: return@updateSelectedBlock block
            val hasAnyRemovable = table.rows.any { it.cells.size > 1 && columnIndex in it.cells.indices }
            if (!hasAnyRemovable) return@updateSelectedBlock block
            block.copy(
                payload = table.copy(
                    rows = table.rows.map { row ->
                        if (row.cells.size <= 1 || columnIndex !in row.cells.indices) {
                            row
                        } else {
                            row.copy(cells = row.cells.filterIndexed { index, _ -> index != columnIndex })
                        }
                    }
                )
            )
        }
    }

    fun updateTableHasHeaderRow(enabled: Boolean) {
        updateSelectedBlock { block ->
            val table = block.payload as? MemoBlockPayload.Table ?: return@updateSelectedBlock block
            block.copy(payload = table.copy(hasHeaderRow = enabled))
        }
    }

    fun updateTableHasHeaderColumn(enabled: Boolean) {
        updateSelectedBlock { block ->
            val table = block.payload as? MemoBlockPayload.Table ?: return@updateSelectedBlock block
            block.copy(payload = table.copy(hasHeaderColumn = enabled))
        }
    }

    fun addConversationItem() {
        updateSelectedBlock { block ->
            val conversation = block.payload as? MemoBlockPayload.Conversation ?: return@updateSelectedBlock block
            block.copy(payload = conversation.copy(items = conversation.items + ConversationItem()))
        }
    }

    fun updateConversationItem(
        itemId: String,
        speaker: String,
        text: String,
        role: ConversationRole
    ) {
        updateSelectedBlock { block ->
            val conversation = block.payload as? MemoBlockPayload.Conversation ?: return@updateSelectedBlock block
            block.copy(
                payload = conversation.copy(
                    items = conversation.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(
                                speaker = speaker.take(60),
                                text = text.take(240),
                                role = role
                            )
                        } else {
                            item
                        }
                    }
                )
            )
        }
    }

    fun removeConversationItem(itemId: String) {
        updateSelectedBlock { block ->
            val conversation = block.payload as? MemoBlockPayload.Conversation ?: return@updateSelectedBlock block
            val nextItems = conversation.items.filterNot { it.id == itemId }
            block.copy(payload = conversation.copy(items = if (nextItems.isEmpty()) conversation.items else nextItems))
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

    fun toggleToggleExpandedFromCanvas(blockId: String) {
        val draft = draftState.value ?: return
        draftState.value = draft.copy(
            blocks = draft.blocks.map { block ->
                if (block.id != blockId || block.type != MemoBlockType.Toggle) {
                    block
                } else {
                    val toggle = block.payload as? MemoBlockPayload.Toggle ?: MemoBlockPayload.Toggle()
                    block.copy(payload = toggle.copy(initiallyExpanded = !toggle.initiallyExpanded))
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
            emitMessage("Add at least one non-empty block before saving.")
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
            codeFullscreenBlockIdState.value = null
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
        toolPaletteVisibleState.value = false
        fabExpandedState.value = false
        drawingLibraryVisibleState.value = false
        drawingEditorVisibleState.value = false
        codeFullscreenBlockIdState.value = null
    }

    fun exportGif(darkTheme: Boolean) {
        val draft = draftState.value ?: return
        if (!draft.hasContent) {
            emitMessage("Add at least one non-empty block before exporting.")
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
            emitMessage("Add at least one non-empty block before exporting.")
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

    override fun onCleared() {
        cancelLinkMetadataFetch()
        super.onCleared()
    }

    private fun minimumBlockYFor(paperStyle: PaperStyle): Float {
        return if (paperStyle.supportsTopAlignedBlocks) 0.02f else 0.14f
    }

    private fun insertBlockAndOpenEditor(draft: MemoDraft, newBlock: MemoBlock) {
        draftState.value = draft.copy(
            blocks = draft.blocks + newBlock,
            updatedAt = System.currentTimeMillis()
        )
        selectedBlockIdState.value = newBlock.id
        editorSheetVisibleState.value = true
        toolPaletteVisibleState.value = false
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
        toolPaletteVisibleState.value = false
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
        if (supportsTextSizing) {
            add(TextStyleSetting.MIN_FONT_SIZE / safeFontSize)
        }
    }.maxOrNull() ?: 1f
    val maxScale = buildList {
        add(MAX_BLOCK_FRACTION / safeWidth)
        add(MAX_BLOCK_FRACTION / safeHeight)
        if (supportsTextSizing) {
            add(TextStyleSetting.MAX_FONT_SIZE / safeFontSize)
        }
    }.minOrNull() ?: 1f
    val appliedScale = safeScale.coerceIn(minScale, maxScale)
    val scaledWidth = widthFraction * appliedScale
    val scaledHeight = heightFraction * appliedScale

    val adjustedY = if (supportsTextSizing) {
        val centerY = normalizedY + (heightFraction / 2f)
        (centerY - (scaledHeight / 2f)).coerceIn(minimumY, 0.9f)
    } else {
        normalizedY
    }

    return copy(
        normalizedY = adjustedY,
        widthFraction = scaledWidth,
        heightFraction = scaledHeight,
        textStyle = if (supportsTextSizing) {
            textStyle.copy(fontSize = textStyle.fontSize * appliedScale)
        } else {
            textStyle
        }
    )
}

private const val MIN_BLOCK_FRACTION = 0.12f
private const val MAX_BLOCK_FRACTION = 0.9f
private const val LINK_METADATA_FETCH_DEBOUNCE_MS = 600L

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
    val isFabVisible: Boolean,
    val isFabExpanded: Boolean,
    val isDrawingLibraryVisible: Boolean,
    val isDrawingEditorVisible: Boolean,
    val codeFullscreenBlockId: String?
)

private data class PartialEditorVisibility(
    val isToolPaletteVisible: Boolean,
    val isFabVisible: Boolean,
    val isFabExpanded: Boolean,
    val isDrawingLibraryVisible: Boolean,
    val isDrawingEditorVisible: Boolean,
    val codeFullscreenBlockId: String?
)

private const val EDITOR_VIEW_MODEL_TAG = "EditorViewModel"

