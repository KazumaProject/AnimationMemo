package com.kazumaproject.animationswipememo.ui.editor

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.ListAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoFontFamily
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import com.kazumaproject.animationswipememo.domain.model.ThemeMode
import com.kazumaproject.animationswipememo.ui.components.AnimationStyleChips
import com.kazumaproject.animationswipememo.ui.components.DrawingEditorOverlay
import com.kazumaproject.animationswipememo.ui.components.PaperMemoCanvas
import com.kazumaproject.animationswipememo.ui.components.SavedDrawingLibrarySheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isTitleDialogVisible by rememberSaveable { mutableStateOf(false) }
    var pendingTitle by rememberSaveable { mutableStateOf("") }
    var isDiscardConfirmDialogVisible by rememberSaveable { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (uiState.settings.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addImageBlock(
                imageUri = uri.toString(),
                contentAspectRatio = readImageAspectRatio(context, uri)
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects().collect { effect ->
            when (effect) {
                is EditorEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                EditorEffect.PerformHaptic -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val sheetOpacity = uiState.settings.editorSheetOpacity.coerceIn(0.45f, 1f)
    val sheetColor = MaterialTheme.colorScheme.surface.copy(alpha = sheetOpacity)
    val scrimAlpha = ((1f - sheetOpacity) * 0.22f) + 0.04f

    fun dismissTransientInput() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    if (isTitleDialogVisible && uiState.draft != null) {
        AlertDialog(
            onDismissRequest = { isTitleDialogVisible = false },
            title = { Text("Memo title") },
            text = {
                OutlinedTextField(
                    value = pendingTitle,
                    onValueChange = { pendingTitle = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    placeholder = { Text("Untitled memo") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMemoTitle(pendingTitle)
                        isTitleDialogVisible = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isTitleDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isDiscardConfirmDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDiscardConfirmDialogVisible = false },
            title = { Text("Discard memo?") },
            text = {
                Text("This clears the current memo content. You cannot undo this action.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDiscardConfirmDialogVisible = false
                        viewModel.discardMemo()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDiscardConfirmDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.isEditorSheetVisible && uiState.selectedBlock != null) {
        ModalBottomSheet(
            onDismissRequest = {
                dismissTransientInput()
                viewModel.hideEditorSheet()
            },
            modifier = Modifier.imePadding(),
            containerColor = sheetColor,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha)
        ) {
            BlockEditorSheet(
                uiState = uiState,
                onTextChange = viewModel::updateSelectedBlockText,
                onAnimationChange = viewModel::updateSelectedBlockAnimation,
                onFontSizeChange = viewModel::updateSelectedBlockFontSize,
                onFontFamilyChange = viewModel::updateSelectedBlockFontFamily,
                onToggleBold = viewModel::toggleSelectedBlockBold,
                onToggleItalic = viewModel::toggleSelectedBlockItalic,
                onToggleUnderline = viewModel::toggleSelectedBlockUnderline,
                onWidthChange = viewModel::updateSelectedBlockWidth,
                onHeightChange = viewModel::updateSelectedBlockHeight,
                onAddListItem = viewModel::addListItem,
                onUpdateListItemText = viewModel::updateListItemText,
                onUpdateListItemType = viewModel::updateListItemType,
                onRemoveListItem = viewModel::removeListItem,
                onToggleListItemChecked = viewModel::toggleListItemChecked,
                onIncreaseIndent = viewModel::increaseListItemIndent,
                onDecreaseIndent = viewModel::decreaseListItemIndent,
                onMoveListItemUp = viewModel::moveListItemUp,
                onMoveListItemDown = viewModel::moveListItemDown,
                onListAppearanceChange = viewModel::updateSelectedListAppearance,
                onDeleteBlock = viewModel::deleteSelectedBlock,
                onClose = {
                    dismissTransientInput()
                    viewModel.hideEditorSheet()
                }
            )
        }
    }

    if (uiState.isDrawingLibraryVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeDrawingLibrary,
            containerColor = sheetColor,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha)
        ) {
            SavedDrawingLibrarySheet(
                savedDrawings = uiState.savedDrawings,
                onNewDrawing = viewModel::openDrawingEditor,
                onInsertDrawing = viewModel::insertSavedDrawing
            )
        }
    }

    if (
        uiState.isToolPaletteVisible &&
        !uiState.isEditorSheetVisible &&
        !uiState.isDrawingLibraryVisible &&
        !uiState.isDrawingEditorVisible
    ) {
        val paletteUiModels = ToolPaletteCatalog.buildUiModels(
            selectedBlockId = uiState.selectedBlockId,
            hasContent = uiState.draft?.hasContent == true
        )
        ModalBottomSheet(
            onDismissRequest = viewModel::hideToolPalette,
            containerColor = sheetColor,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha)
        ) {
            ToolPaletteSheet(
                models = paletteUiModels,
                onAction = { action ->
                    viewModel.hideToolPalette()
                    when (action) {
                        PaletteAction.AddText -> viewModel.addTextBlock()
                        PaletteAction.AddImage -> imagePicker.launch(arrayOf("image/*"))
                        PaletteAction.AddList -> viewModel.addListBlock()
                        PaletteAction.OpenDrawingLibrary -> viewModel.openDrawingLibrary()
                        PaletteAction.EditSelected -> {
                            Log.d(EDITOR_SCREEN_TAG, "Palette Edit tapped")
                            viewModel.openEditorFromFab()
                        }
                        PaletteAction.ExportGif -> viewModel.exportGif(isDarkTheme)
                        PaletteAction.ExportPng -> viewModel.exportPng(isDarkTheme)
                        PaletteAction.DiscardMemo -> {
                            isDiscardConfirmDialogVisible = true
                        }
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(
                uiState.selectedBlockId,
                uiState.isEditorSheetVisible,
                uiState.isFabExpanded,
                uiState.isToolPaletteVisible,
                uiState.isDrawingLibraryVisible,
                uiState.isDrawingEditorVisible
            ) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    if (up != null && !up.isConsumed) {
                        if (
                            uiState.isFabExpanded ||
                            uiState.isToolPaletteVisible ||
                            uiState.isDrawingLibraryVisible ||
                            uiState.isDrawingEditorVisible
                        ) {
                            Log.d(
                                EDITOR_SCREEN_TAG,
                                "Global tap-dismiss skipped. fabExpanded=${uiState.isFabExpanded}, toolPalette=${uiState.isToolPaletteVisible}, drawingLibrary=${uiState.isDrawingLibraryVisible}, drawingEditor=${uiState.isDrawingEditorVisible}"
                            )
                            return@awaitEachGesture
                        }
                        Log.d(EDITOR_SCREEN_TAG, "Global tap-dismiss triggered for editor sheet")
                        dismissTransientInput()
                        viewModel.clearSelection()
                        viewModel.hideEditorSheet()
                    }
                }
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.clickable(enabled = uiState.draft != null) {
                                pendingTitle = uiState.draft?.title.orEmpty()
                                isTitleDialogVisible = true
                            }
                        ) {
                            Text(
                                uiState.draft?.displayTitle
                                    ?: if (uiState.isExistingMemo) "Memo" else "New memo"
                            )
                            Text(
                                text = "Tap to rename",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenList) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.List,
                                contentDescription = "Open memo list"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::saveMemo) {
                            Icon(Icons.Outlined.Save, contentDescription = "Save memo")
                        }
                        IconButton(onClick = viewModel::toggleFabVisibility) {
                            Icon(
                                imageVector = if (uiState.isFabVisible) {
                                    Icons.Outlined.Visibility
                                } else {
                                    Icons.Outlined.VisibilityOff
                                },
                                contentDescription = "Toggle floating actions"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Open settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (uiState.isFabVisible) {
                    ExpandableFabMenu(
                        expanded = uiState.isFabExpanded,
                        actions = editorFabActions(
                            canDiscard = uiState.draft?.hasContent == true
                        ),
                        onToggleExpanded = viewModel::toggleFabExpansion,
                        onActionClick = { action ->
                            when (action.id) {
                                EditorFabActionId.CreateNewMemo -> viewModel.createNewMemo()
                                EditorFabActionId.OpenInsertPalette -> {
                                    Log.d(EDITOR_SCREEN_TAG, "FAB Insert tapped")
                                    viewModel.showToolPalette()
                                }
                                EditorFabActionId.DiscardMemo -> {
                                    isDiscardConfirmDialogVisible = true
                                }
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            if (uiState.isLoading || uiState.draft == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
                }
            } else {
                EditorCanvasContent(
                    uiState = uiState,
                    darkTheme = isDarkTheme,
                    modifier = Modifier.padding(innerPadding),
                    onOpenBlockEditor = viewModel::openBlockEditor,
                    onCanvasTap = {
                        dismissTransientInput()
                        viewModel.clearSelection()
                        viewModel.hideEditorSheet()
                    },
                    onBlockDragStart = { blockId ->
                        dismissTransientInput()
                        viewModel.startBlockDrag(blockId)
                    },
                    onBlockDrag = viewModel::moveBlock,
                    onBlockScale = viewModel::scaleBlock,
                    onToggleListItemCheckedOnCanvas = viewModel::toggleListItemCheckedFromCanvas,
                    onToggleListItemExpandedOnCanvas = viewModel::toggleListItemExpandedFromCanvas,
                )
            }
        }

        if (uiState.isDrawingEditorVisible) {
            DrawingEditorOverlay(
                darkTheme = isDarkTheme,
                onClose = viewModel::closeDrawingEditor,
                onSave = { strokes, widthFraction, heightFraction, saveToLibrary, name ->
                    viewModel.saveNewDrawingFromEditor(
                        strokes = strokes,
                        widthFraction = widthFraction,
                        heightFraction = heightFraction,
                        saveToLibrary = saveToLibrary,
                        name = name
                    )
                }
            )
        }
    }
}

@Composable
private fun EditorCanvasContent(
    uiState: EditorUiState,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onOpenBlockEditor: (String) -> Unit,
    onCanvasTap: () -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit,
    onToggleListItemCheckedOnCanvas: (String, String) -> Unit,
    onToggleListItemExpandedOnCanvas: (String, String) -> Unit
) {
    val draft = uiState.draft ?: return
    val transition = rememberInfiniteTransition(label = "canvasAnimation")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = uiState.settings.gifQuality.durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "canvasProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PaperMemoCanvas(
            memo = draft,
            selectedBlockId = uiState.selectedBlockId,
            progress = progress,
            darkTheme = darkTheme,
            modifier = Modifier.fillMaxSize(),
            onBlockTap = onOpenBlockEditor,
            onCanvasTap = onCanvasTap,
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag,
            onBlockScale = onBlockScale,
            onToggleListItemChecked = onToggleListItemCheckedOnCanvas,
            onToggleListItemExpanded = onToggleListItemExpandedOnCanvas
        )
    }
}

@Composable
private fun ToolPaletteSheet(
    models: List<PaletteUiModel>,
    onAction: (PaletteAction) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.62f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 24.dp
        )
    ) {
        val headers = models.filterIsInstance<PaletteUiModel.Header>()
        headers.forEach { header ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = header.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                )
            }
            val sectionItems = models
                .filterIsInstance<PaletteUiModel.Item>()
                .filter { it.group.headerTitle == header.title }
            items(sectionItems, key = { it.id }) { item ->
                ToolPaletteCell(item = item, onClick = { onAction(item.action) })
            }
        }
    }
}

@Composable
private fun ToolPaletteCell(
    item: PaletteUiModel.Item,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val backgroundColor = when {
        !item.enabled -> colors.surfaceVariant.copy(alpha = 0.5f)
        item.selected -> colors.primaryContainer
        else -> colors.surfaceVariant
    }
    val contentColor = when {
        !item.enabled -> colors.onSurface.copy(alpha = 0.38f)
        item.selected -> colors.onPrimaryContainer
        else -> colors.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp),
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick,
        enabled = item.enabled,
        border = if (item.selected) {
            androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                        imageVector = paletteIcon(item.icon),
                contentDescription = item.title,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                text = item.shortTitle,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.badge != null) {
                Text(
                    text = item.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private enum class EditorFabActionId {
    CreateNewMemo,
    OpenInsertPalette,
    DiscardMemo
}

private enum class FabActionPosition {
    VerticalNear,
    VerticalFar,
    Left
}

private data class EditorFabAction(
    val id: EditorFabActionId,
    val label: String,
    val icon: ImageVector,
    val position: FabActionPosition,
    val enabled: Boolean = true
)

private fun editorFabActions(
    canDiscard: Boolean
): List<EditorFabAction> {
    return listOf(
        EditorFabAction(
            id = EditorFabActionId.CreateNewMemo,
            label = "New memo",
            icon = Icons.AutoMirrored.Outlined.NoteAdd,
            position = FabActionPosition.VerticalNear
        ),
        EditorFabAction(
            id = EditorFabActionId.OpenInsertPalette,
            label = "Insert",
            icon = Icons.Outlined.AutoFixHigh,
            position = FabActionPosition.Left,
            enabled = true
        ),
        EditorFabAction(
            id = EditorFabActionId.DiscardMemo,
            label = "Discard",
            icon = Icons.Outlined.DeleteSweep,
            position = FabActionPosition.VerticalFar,
            enabled = canDiscard
        )
    )
}

@Composable
private fun ExpandableFabMenu(
    expanded: Boolean,
    actions: List<EditorFabAction>,
    onToggleExpanded: () -> Unit,
    onActionClick: (EditorFabAction) -> Unit
) {
    val verticalNearOffset = (-78).dp
    val verticalFarOffset = (-146).dp
    val leftOffset = (-82).dp

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(width = 218.dp, height = 232.dp)
    ) {
        if (expanded) {
            actions.forEach { action ->
                val actionOffset = when (action.position) {
                    FabActionPosition.VerticalNear -> Pair(0.dp, verticalNearOffset)
                    FabActionPosition.VerticalFar -> Pair(0.dp, verticalFarOffset)
                    FabActionPosition.Left -> Pair(leftOffset, 0.dp)
                }
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = {
                        Log.d(
                            EDITOR_SCREEN_TAG,
                            "Mini FAB tapped: id=${action.id}, enabled=${action.enabled}, position=${action.position}"
                        )
                        if (action.enabled) {
                            onActionClick(action)
                        } else {
                            Log.d(
                                EDITOR_SCREEN_TAG,
                                "Mini FAB tap ignored because action is disabled: id=${action.id}"
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = actionOffset.first, y = actionOffset.second)
                        .size(50.dp),
                    containerColor = if (action.id == EditorFabActionId.DiscardMemo) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (action.id == EditorFabActionId.DiscardMemo) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        modifier = Modifier.size(22.dp),
                        tint = if (action.enabled) {
                            androidx.compose.ui.graphics.Color.Unspecified
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = onToggleExpanded,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = if (expanded) {
                    Icons.Outlined.Close
                } else {
                    Icons.AutoMirrored.Outlined.List
                },
                contentDescription = if (expanded) {
                    "Close quick actions"
                } else {
                    "Open quick actions"
                }
            )
        }
    }
}

private fun paletteIcon(icon: PaletteIcon): ImageVector {
    return when (icon) {
        PaletteIcon.AddText -> Icons.AutoMirrored.Outlined.NoteAdd
        PaletteIcon.AddImage -> Icons.Outlined.Image
        PaletteIcon.AddList -> Icons.AutoMirrored.Outlined.List
        PaletteIcon.Handwriting -> Icons.Outlined.AutoFixHigh
        PaletteIcon.Edit -> Icons.Outlined.Draw
        PaletteIcon.Export -> Icons.Outlined.FileDownload
        PaletteIcon.ExportPng -> Icons.Outlined.Save
        PaletteIcon.Discard -> Icons.Outlined.DeleteSweep
    }
}

@Composable
private fun BlockEditorSheet(
    uiState: EditorUiState,
    onTextChange: (String) -> Unit,
    onAnimationChange: (AnimationStyle) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (MemoFontFamily) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onAddListItem: () -> Unit,
    onUpdateListItemText: (String, String) -> Unit,
    onUpdateListItemType: (String, ListItemType) -> Unit,
    onRemoveListItem: (String) -> Unit,
    onToggleListItemChecked: (String) -> Unit,
    onIncreaseIndent: (String) -> Unit,
    onDecreaseIndent: (String) -> Unit,
    onMoveListItemUp: (String) -> Unit,
    onMoveListItemDown: (String) -> Unit,
    onListAppearanceChange: (ListAppearance) -> Unit,
    onDeleteBlock: () -> Unit,
    onClose: () -> Unit
) {
    val block = uiState.selectedBlock ?: return
    val animationChoices = AnimationStyle.availableFor(block.type)
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (block.type) {
                MemoBlockType.Text -> "Text block editor"
                MemoBlockType.Image -> "Image block editor"
                MemoBlockType.Drawing -> "Handwriting block editor"
                MemoBlockType.List -> "List block editor"
            },
            style = MaterialTheme.typography.titleLarge
        )

        when (block.type) {
            MemoBlockType.Text -> {
                OutlinedTextField(
                    value = block.text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Text") },
                    placeholder = { Text("Short phrase or lyric fragment") },
                    minLines = 3,
                    maxLines = 6
                )
            }

            MemoBlockType.Image -> {
                Text(
                    text = "This image block can move freely on the memo and now supports the same motion effects as text.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            MemoBlockType.Drawing -> {
                Text(
                    text = "This handwriting block can be repositioned and animated after you place it on the memo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            MemoBlockType.List -> {
                ListAppearanceEditor(
                    appearance = block.listAppearance ?: ListAppearance(),
                    onAppearanceChange = onListAppearanceChange
                )
                ListItemsEditor(
                    block = block,
                    onAddItem = onAddListItem,
                    onUpdateText = onUpdateListItemText,
                    onUpdateType = onUpdateListItemType,
                    onRemoveItem = onRemoveListItem,
                    onToggleChecked = onToggleListItemChecked,
                    onIncreaseIndent = onIncreaseIndent,
                    onDecreaseIndent = onDecreaseIndent,
                    onMoveUp = onMoveListItemUp,
                    onMoveDown = onMoveListItemDown
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Animation",
                style = MaterialTheme.typography.titleMedium
            )
            AnimationStyleChips(
                selected = block.animationStyle,
                styles = animationChoices,
                onSelect = onAnimationChange
            )
        }

        if (block.type == MemoBlockType.Text) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Font",
                    style = MaterialTheme.typography.titleMedium
                )
                MemoFontFamilyChips(
                    selected = block.textStyle.fontFamily,
                    onSelect = onFontFamilyChange
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Style",
                    style = MaterialTheme.typography.titleMedium
                )
                MemoTextStyleChips(
                    isBold = block.textStyle.isBold,
                    isItalic = block.textStyle.isItalic,
                    isUnderline = block.textStyle.isUnderline,
                    onToggleBold = onToggleBold,
                    onToggleItalic = onToggleItalic,
                    onToggleUnderline = onToggleUnderline
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Font size: ${block.textStyle.fontSize.toInt()}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = block.textStyle.fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = TextStyleSetting.MIN_FONT_SIZE..TextStyleSetting.MAX_FONT_SIZE
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (block.type == MemoBlockType.Text || block.type == MemoBlockType.List) {
                    "Text area width: ${(block.widthFraction * 100f).toInt()}%"
                } else {
                    "Width: ${(block.widthFraction * 100f).toInt()}%"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = block.widthFraction,
                onValueChange = onWidthChange,
                valueRange = 0.12f..0.9f
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (block.type == MemoBlockType.Text || block.type == MemoBlockType.List) {
                    "Text area height: ${(block.heightFraction * 100f).toInt()}%"
                } else {
                    "Height: ${(block.heightFraction * 100f).toInt()}%"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = block.heightFraction,
                onValueChange = onHeightChange,
                valueRange = 0.12f..0.9f
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDeleteBlock) {
                Text("Remove block")
            }
            TextButton(onClick = onClose) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun ListAppearanceEditor(
    appearance: ListAppearance,
    onAppearanceChange: (ListAppearance) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("List font scale: ${(appearance.fontScale * 100f).toInt()}%", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = appearance.fontScale,
            onValueChange = { onAppearanceChange(appearance.copy(fontScale = it.coerceIn(0.72f, 1.4f))) },
            valueRange = 0.72f..1.4f
        )
    }
}

@Composable
private fun ListItemsEditor(
    block: com.kazumaproject.animationswipememo.domain.model.MemoBlock,
    onAddItem: () -> Unit,
    onUpdateText: (String, String) -> Unit,
    onUpdateType: (String, ListItemType) -> Unit,
    onRemoveItem: (String) -> Unit,
    onToggleChecked: (String) -> Unit,
    onIncreaseIndent: (String) -> Unit,
    onDecreaseIndent: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Items", style = MaterialTheme.typography.titleMedium)
        block.listItems.forEachIndexed { index, item ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { onUpdateText(item.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Item ${index + 1}") },
                    singleLine = true
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListItemType.entries.forEach { type ->
                        FilterChip(
                            selected = item.itemType == type,
                            onClick = { onUpdateType(item.id, type) },
                            label = { Text(type.name) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onIncreaseIndent(item.id) }) { Text("Indent +") }
                    TextButton(onClick = { onDecreaseIndent(item.id) }) { Text("Indent -") }
                    TextButton(onClick = { onMoveUp(item.id) }) { Text("Up") }
                    TextButton(onClick = { onMoveDown(item.id) }) { Text("Down") }
                    TextButton(onClick = { onRemoveItem(item.id) }) { Text("Delete") }
                }
                if (item.itemType == ListItemType.CHECKBOX) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Checked")
                        Switch(
                            checked = item.checked,
                            onCheckedChange = { onToggleChecked(item.id) }
                        )
                    }
                }
            }
        }
        TextButton(onClick = onAddItem) {
            Text("Add item")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoFontFamilyChips(
    selected: MemoFontFamily,
    onSelect: (MemoFontFamily) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MemoFontFamily.entries.forEach { fontFamily ->
            FilterChip(
                selected = fontFamily == selected,
                onClick = { onSelect(fontFamily) },
                label = {
                    Text(
                        text = fontFamily.displayName,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoTextStyleChips(
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = isBold,
            onClick = onToggleBold,
            label = { Text("Bold") }
        )
        FilterChip(
            selected = isItalic,
            onClick = onToggleItalic,
            label = { Text("Italic") }
        )
        FilterChip(
            selected = isUnderline,
            onClick = onToggleUnderline,
            label = { Text("Underline") }
        )
    }
}

private fun readImageAspectRatio(
    context: android.content.Context,
    uri: Uri
): Float {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }
    val width = options.outWidth.takeIf { it > 0 } ?: return 1f
    val height = options.outHeight.takeIf { it > 0 } ?: return 1f
    return width.toFloat() / height.toFloat()
}

private const val EDITOR_SCREEN_TAG = "EditorScreen"

