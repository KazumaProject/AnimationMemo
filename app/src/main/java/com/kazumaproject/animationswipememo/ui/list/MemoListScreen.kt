package com.kazumaproject.animationswipememo.ui.list

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.fitContentSize
import com.kazumaproject.animationswipememo.domain.model.resolvedContentAspectRatio
import com.kazumaproject.animationswipememo.platform.toComposeFontFamily
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Saved memos")
                        Text(
                            text = "Search across text blocks and continue editing any memo.",
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
                label = { Text("Search text blocks") },
                placeholder = { Text("Find memos by words inside text blocks") },
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
                                onClick = { onMemoClick(memo.id) }
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

@Composable
private fun MemoListItem(
    memo: MemoDraft,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val paper = memo.paperStyle.palette(darkTheme)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = memo.displayPreviewText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(memo.blocks.firstOrNull()?.textStyle?.resolvedTextColor(darkTheme)
                        ?: if (darkTheme) 0xFFF7F0E2.toInt() else 0xFF2D241C.toInt())
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
                val lineColor = Color(paper.lineArgb)
                val edgeColor = Color(paper.edgeArgb)
                drawRoundRect(
                    color = edgeColor,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                var y = size.height * 0.18f
                while (y < size.height * 0.94f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width * 0.08f, y),
                        end = Offset(size.width * 0.92f, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += size.height * 0.11f
                }
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
            }
        }
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
            color = Color(block.textStyle.resolvedTextColor(darkTheme)),
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ThumbnailImageBlock(block: MemoBlock) {
    val bitmap = rememberUriBitmap(block.imageUri)
    val boxWidth = 112f * block.widthFraction
    val boxHeight = 148f * block.heightFraction
    val fitted = fitContentSize(
        boxWidth = boxWidth,
        boxHeight = boxHeight,
        aspectRatio = block.resolvedContentAspectRatio()
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
private fun rememberUriBitmap(imageUri: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return remember(context, imageUri) {
        imageUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri)).use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
}
