package com.kazumaproject.animationswipememo.ui.editor

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AutoFixHigh
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
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

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(uiState.selectedBlockId, uiState.isEditorSheetVisible) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    if (up != null && !up.isConsumed) {
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
                        IconButton(onClick = viewModel::toggleToolPaletteVisibility) {
                            Icon(
                                imageVector = if (uiState.isToolPaletteVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = "Toggle tool palette"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Open settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (uiState.isToolPaletteVisible) {
                    FloatingActionButton(onClick = viewModel::createNewMemo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                            contentDescription = "Create new memo"
                        )
                    }
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
                    onAddText = viewModel::addTextBlock,
                    onAddImage = { imagePicker.launch(arrayOf("image/*")) },
                    onOpenDrawingLibrary = viewModel::openDrawingLibrary,
                    onEditSelected = viewModel::showEditorSheet,
                    onExportGif = { viewModel.exportGif(isDarkTheme) },
                    onExportPng = { viewModel.exportPng(isDarkTheme) },
                    onDiscard = viewModel::discardMemo
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

@OptIn(ExperimentalLayoutApi::class)
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
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onOpenDrawingLibrary: () -> Unit,
    onEditSelected: () -> Unit,
    onExportGif: () -> Unit,
    onExportPng: () -> Unit,
    onDiscard: () -> Unit
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
        if (uiState.isToolPaletteVisible) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton("Add Text", Icons.Outlined.AddBox, onAddText)
                ToolButton("Image", Icons.Outlined.Image, onAddImage)
                ToolButton("Handwriting", Icons.Outlined.AutoFixHigh, onOpenDrawingLibrary)
                ToolButton("Edit", Icons.Outlined.Draw, onEditSelected)
                ToolButton("GIF", Icons.Outlined.FileDownload, onExportGif)
                ToolButton("PNG", Icons.Outlined.Image, onExportPng)
                ToolButton("Discard", Icons.Outlined.DeleteSweep, onDiscard)
            }
        }

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
            onBlockScale = onBlockScale
        )
    }
}

@Composable
private fun ToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledTonalButton(onClick = onClick) {
        Icon(icon, contentDescription = label)
        Text(text = label, modifier = Modifier.padding(start = 6.dp))
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
                text = if (block.type == MemoBlockType.Text) {
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
                text = if (block.type == MemoBlockType.Text) {
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
