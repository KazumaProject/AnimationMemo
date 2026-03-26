package com.kazumaproject.animationswipememo.ui.list

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.fitContentSize
import com.kazumaproject.animationswipememo.domain.model.resolvedContentAspectRatio
import com.kazumaproject.animationswipememo.platform.decodeSampledBitmap
import com.kazumaproject.animationswipememo.platform.composeFontStyle
import com.kazumaproject.animationswipememo.platform.composeFontWeight
import com.kazumaproject.animationswipememo.platform.composeTextDecoration
import com.kazumaproject.animationswipememo.platform.toComposeFontFamily
import com.kazumaproject.animationswipememo.ui.components.drawPaperDecorations
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    viewModel: MemoListViewModel,
    onBack: () -> Unit,
    onMemoClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val clipboardManager = LocalClipboardManager.current
    var pendingDeleteMemo by remember { mutableStateOf<MemoDraft?>(null) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }

    if (pendingDeleteMemo != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteMemo = null },
            title = { Text("Delete memo?") },
            text = {
                Text("This removes \"${pendingDeleteMemo?.displayTitle}\" from Saved memos.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteMemo?.id?.let(viewModel::deleteMemo)
                        pendingDeleteMemo = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMemo = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete all memos?") },
            text = {
                Text("This action removes every saved memo and cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllMemos()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Saved memos")
                        Text(
                            text = "Search across titles and text blocks, then continue editing any memo.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteAllDialog = true },
                        enabled = uiState.hasAnyMemos
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Delete all memos"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                label = { Text("Search memos") },
                placeholder = { Text("Find memos by title or text") },
                singleLine = true
            )

            when {
                !uiState.hasAnyMemos -> {
                    EmptyState(
                        title = "No memos yet",
                        body = "Create one from the editor and use the toolbar save icon."
                    )
                }

                uiState.memos.isEmpty() -> {
                    EmptyState(
                        title = "No matching memos",
                        body = "Try a different word from one of your text blocks."
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.memos, key = { it.id }) { memo ->
                            MemoListItem(
                                memo = memo,
                                darkTheme = isDarkTheme,
                                onClick = { onMemoClick(memo.id) },
                                onCopyText = {
                                    clipboardManager.setText(AnnotatedString(memo.copyableContent()))
                                },
                                onDuplicate = {
                                    viewModel.duplicateMemo(memo)
                                },
                                onDelete = {
                                    pendingDeleteMemo = memo
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoListItem(
    memo: MemoDraft,
    darkTheme: Boolean,
    onClick: () -> Unit,
    onCopyText: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val paper = memo.paperStyle.palette(darkTheme)
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { isMenuExpanded = true }
                ),
            colors = CardDefaults.cardColors(containerColor = Color(paper.paperArgb)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoThumbnail(
                    memo = memo,
                    darkTheme = darkTheme,
                    modifier = Modifier
                        .width(112.dp)
                        .height(148.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = memo.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(
                            memo.blocks.firstOrNull()?.textStyle?.resolvedTextColor(darkTheme)
                                ?: if (darkTheme) 0xFFF7F0E2.toInt() else 0xFF2D241C.toInt()
                        )
                    )
                    Text(
                        text = memo.displayPreviewText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = memo.summaryLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Updated ${formatter.format(Date(memo.updatedAt))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copy text") },
                leadingIcon = {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                },
                onClick = {
                    onCopyText()
                    isMenuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Duplicate memo") },
                leadingIcon = {
                    Icon(Icons.Outlined.FileCopy, contentDescription = null)
                },
                onClick = {
                    onDuplicate()
                    isMenuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                },
                onClick = {
                    onDelete()
                    isMenuExpanded = false
                }
            )
        }
    }
}

@Composable
private fun MemoThumbnail(
    memo: MemoDraft,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val paper = memo.paperStyle.palette(darkTheme)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(paper.paperArgb))
            .drawBehind {
                drawPaperDecorations(
                    paperStyle = memo.paperStyle,
                    palette = paper,
                    cornerRadius = 20.dp,
                    borderWidth = 1.4.dp
                )
            }
    ) {
        memo.blocks.take(5).forEach { block ->
            when (block.type) {
                MemoBlockType.Text -> ThumbnailTextBlock(
                    block = block,
                    darkTheme = darkTheme
                )

                MemoBlockType.Image -> ThumbnailImageBlock(block = block)
                MemoBlockType.Drawing -> ThumbnailDrawingBlock(block = block)
                MemoBlockType.List -> ThumbnailListBlock(block = block, darkTheme = darkTheme)
            }
        }
    }
}

@Composable
private fun ThumbnailListBlock(
    block: MemoBlock,
    darkTheme: Boolean
) {
    val preview = block.listItems
        .take(3)
        .mapIndexed { index, item ->
            val marker = when (item.itemType) {
                ListItemType.ORDERED -> "${index + 1}."
                ListItemType.UNORDERED -> "•"
                ListItemType.CHECKBOX -> if (item.checked) "☑" else "☐"
            }
            "$marker ${item.text.ifBlank { "..." }}"
        }
        .joinToString("\n")
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = preview.ifBlank { "• ..." },
            modifier = Modifier
                .width((block.widthFraction * 112f).dp)
                .offset(
                    x = ((block.normalizedX * 112f) - (block.widthFraction * 56f)).dp,
                    y = ((block.normalizedY * 148f) - 14f).dp
                ),
            fontSize = (block.textStyle.fontSize * 0.22f).sp,
            lineHeight = (block.textStyle.fontSize * 0.26f).sp,
            color = Color(block.textStyle.resolvedTextColor(darkTheme)),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ThumbnailTextBlock(
    block: MemoBlock,
    darkTheme: Boolean
) {
    val textAlign = when (block.textStyle.textAlign) {
        com.kazumaproject.animationswipememo.domain.model.MemoTextAlign.Start -> TextAlign.Start
        com.kazumaproject.animationswipememo.domain.model.MemoTextAlign.Center -> TextAlign.Center
        com.kazumaproject.animationswipememo.domain.model.MemoTextAlign.End -> TextAlign.End
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = block.text.ifBlank { "Text" },
            modifier = Modifier
                .width((block.widthFraction * 112f).dp)
                .offset(
                    x = ((block.normalizedX * 112f) - (block.widthFraction * 56f)).dp,
                    y = ((block.normalizedY * 148f) - 14f).dp
                ),
            fontSize = (block.textStyle.fontSize * 0.24f).sp,
            lineHeight = (block.textStyle.fontSize * 0.28f).sp,
            fontFamily = block.textStyle.fontFamily.toComposeFontFamily(),
            fontWeight = block.textStyle.composeFontWeight(),
            fontStyle = block.textStyle.composeFontStyle(),
            textDecoration = block.textStyle.composeTextDecoration(),
            color = Color(block.textStyle.resolvedTextColor(darkTheme)),
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ThumbnailImageBlock(block: MemoBlock) {
    val boxWidth = 112f * block.widthFraction
    val boxHeight = 148f * block.heightFraction
    val fitted = fitContentSize(
        boxWidth = boxWidth,
        boxHeight = boxHeight,
        aspectRatio = block.resolvedContentAspectRatio()
    )
    val density = LocalDensity.current
    val bitmap = rememberUriBitmap(
        imageUri = block.imageUri,
        targetWidthPx = with(density) { fitted.width.dp.roundToPx().coerceAtLeast(1) },
        targetHeightPx = with(density) { fitted.height.dp.roundToPx().coerceAtLeast(1) }
    )
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(fitted.width.dp, fitted.height.dp)
                    .offset(
                        x = ((block.normalizedX * 112f) - (fitted.width / 2f)).dp,
                        y = ((block.normalizedY * 148f) - (fitted.height / 2f)).dp
                    )
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ThumbnailDrawingBlock(block: MemoBlock) {
    val boxWidth = 112f * block.widthFraction
    val boxHeight = 148f * block.heightFraction
    val fitted = fitContentSize(
        boxWidth = boxWidth,
        boxHeight = boxHeight,
        aspectRatio = block.resolvedContentAspectRatio()
    )
    val density = LocalDensity.current
    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val left = with(density) { ((block.normalizedX * 112f) - (fitted.width / 2f)).dp.toPx() }
        val top = with(density) { ((block.normalizedY * 148f) - (fitted.height / 2f)).dp.toPx() }
        val width = with(density) { fitted.width.dp.toPx() }
        val height = with(density) { fitted.height.dp.toPx() }
        block.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path().apply {
                moveTo(left + (stroke.points.first().x * width), top + (stroke.points.first().y * height))
                stroke.points.drop(1).forEach { point ->
                    lineTo(left + (point.x * width), top + (point.y * height))
                }
            }
            drawPath(
                path = path,
                color = Color(stroke.color),
                style = Stroke(width = stroke.width * 0.3f)
            )
        }
    }
}

@Composable
private fun rememberUriBitmap(
    imageUri: String?,
    targetWidthPx: Int,
    targetHeightPx: Int
): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return remember(context, imageUri, targetWidthPx, targetHeightPx) {
        imageUri?.let { uri ->
            runCatching {
                context.decodeSampledBitmap(
                    uri = Uri.parse(uri),
                    maxWidth = targetWidthPx,
                    maxHeight = targetHeightPx
                )?.asImageBitmap()
            }.getOrNull()
        }
    }
}

private fun MemoDraft.copyableContent(): String {
    val body = blocks
        .filter { it.type == MemoBlockType.Text }
        .mapNotNull { it.text.trim().takeIf(String::isNotBlank) }
        .joinToString("\n")

    return buildString {
        title.trim().takeIf(String::isNotBlank)?.let { appendLine(it) }
        if (isNotEmpty() && body.isNotBlank()) {
            appendLine()
        }
        if (body.isNotBlank()) {
            append(body)
        } else if (isEmpty()) {
            append(summaryLabel)
        }
    }.trim()
}
