package com.kazumaproject.animationswipememo.ui.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import com.kazumaproject.animationswipememo.domain.model.ThemeMode
import com.kazumaproject.animationswipememo.ui.components.AnimationStyleChips
import com.kazumaproject.animationswipememo.ui.components.PaperMemoCanvas

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
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (uiState.settings.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    LaunchedEffect(viewModel) {
        viewModel.effects().collect { effect ->
            when (effect) {
                is EditorEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                EditorEffect.PerformHaptic -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    if (uiState.isEditorSheetVisible && uiState.selectedBlock != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideEditorSheet,
            modifier = Modifier.imePadding()
        ) {
            BlockEditorSheet(
                uiState = uiState,
                onTextChange = viewModel::updateSelectedBlockText,
                onAnimationChange = viewModel::updateSelectedBlockAnimation,
                onFontSizeChange = viewModel::updateSelectedBlockFontSize,
                onDeleteBlock = viewModel::deleteSelectedBlock,
                onClose = viewModel::hideEditorSheet
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (uiState.isExistingMemo) "Memo canvas" else "New memo")
                        Text(
                            text = "Drag blocks on the paper, edit from the hidden sheet.",
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Open settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading || uiState.draft == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            EditorCanvasContent(
                uiState = uiState,
                darkTheme = isDarkTheme,
                modifier = Modifier.padding(innerPadding),
                onOpenBlockEditor = viewModel::openBlockEditor,
                onCanvasTap = {
                    viewModel.clearSelection()
                    viewModel.hideEditorSheet()
                },
                onBlockDragStart = viewModel::startBlockDrag,
                onBlockDrag = { blockId, dx, dy ->
                    viewModel.selectBlock(blockId)
                    viewModel.moveSelectedBlock(dx, dy)
                },
                onAddBlock = viewModel::addBlock,
                onEditSelected = viewModel::showEditorSheet,
                onSave = viewModel::saveMemo,
                onExportGif = { viewModel.exportGif(isDarkTheme) },
                onExportPng = { viewModel.exportPng(isDarkTheme) },
                onDiscard = viewModel::discardMemo,
                onOpenList = onOpenList,
                onOpenSettings = onOpenSettings
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
    onAddBlock: () -> Unit,
    onEditSelected: () -> Unit,
    onSave: () -> Unit,
    onExportGif: () -> Unit,
    onExportPng: () -> Unit,
    onDiscard: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton("Add Text", Icons.Outlined.AddBox, onAddBlock)
                ToolButton("Edit", Icons.Outlined.Draw, onEditSelected)
                ToolButton("Save", Icons.Outlined.Save, onSave)
                ToolButton("GIF", Icons.Outlined.FileDownload, onExportGif)
                ToolButton("PNG", Icons.Outlined.Image, onExportPng)
                ToolButton("Discard", Icons.Outlined.DeleteSweep, onDiscard)
                ToolButton("Memos", Icons.AutoMirrored.Outlined.List, onOpenList)
                ToolButton("Settings", Icons.Outlined.Settings, onOpenSettings)
            }

            Box(modifier = Modifier.weight(1f)) {
                PaperMemoCanvas(
                    memo = draft,
                    selectedBlockId = uiState.selectedBlockId,
                    progress = progress,
                    darkTheme = darkTheme,
                    modifier = Modifier.fillMaxSize(),
                    onBlockTap = onOpenBlockEditor,
                    onCanvasTap = onCanvasTap,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )
            }
        }

        if (draft.blocks.isEmpty()) {
            Text(
                text = "Tap + to add your first text block.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineSmall
            )
        } else if (uiState.selectedBlock == null) {
            Text(
                text = "Tap a block to select it, then drag or edit it.",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun BlockEditorSheet(
    uiState: EditorUiState,
    onTextChange: (String) -> Unit,
    onAnimationChange: (AnimationStyle) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onDeleteBlock: () -> Unit,
    onClose: () -> Unit
) {
    val block = uiState.selectedBlock ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Block editor",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "This sheet stays hidden until you edit a selected block.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = block.text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text") },
            placeholder = { Text("Short phrase or lyric fragment") },
            minLines = 3,
            maxLines = 6
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Animation",
                style = MaterialTheme.typography.titleMedium
            )
            AnimationStyleChips(
                selected = block.animationStyle,
                onSelect = onAnimationChange
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
        TextButton(
            onClick = onDeleteBlock,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Remove block")
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Done")
        }
    }
}
