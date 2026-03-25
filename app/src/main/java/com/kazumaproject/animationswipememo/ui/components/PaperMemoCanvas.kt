package com.kazumaproject.animationswipememo.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazumaproject.animationswipememo.domain.animation.MemoAnimationEngine
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoTextAlign
import com.kazumaproject.animationswipememo.domain.model.StrokeData
import com.kazumaproject.animationswipememo.domain.model.StrokePoint
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PaperMemoCanvas(
    memo: MemoDraft,
    selectedBlockId: String?,
    progress: Float,
    darkTheme: Boolean,
    isDrawingMode: Boolean,
    modifier: Modifier = Modifier,
    onBlockTap: (String) -> Unit,
    onCanvasTap: () -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onDrawingComplete: (Float, Float, Float, Float, StrokeData) -> Unit
) {
    val paper = memo.paperStyle.palette(darkTheme)
    val shape = RoundedCornerShape(28.dp)
    val density = LocalDensity.current
    var canvasWidthPx by remember { mutableIntStateOf(1) }
    var canvasHeightPx by remember { mutableIntStateOf(1) }
    val draftPoints = remember { mutableStateListOf<Offset>() }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(paper.paperArgb))
            .onSizeChanged {
                canvasWidthPx = it.width.coerceAtLeast(1)
                canvasHeightPx = it.height.coerceAtLeast(1)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isDrawingMode,
                onClick = onCanvasTap
            )
            .pointerInput(isDrawingMode, canvasWidthPx, canvasHeightPx) {
                if (!isDrawingMode) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        draftPoints.clear()
                        draftPoints.add(offset)
                    },
                    onDrag = { change, dragAmount ->
                        val next = change.position
                        if (draftPoints.isEmpty()) {
                            draftPoints.add(next - dragAmount)
                        }
                        draftPoints.add(next)
                    },
                    onDragEnd = {
                        if (draftPoints.size > 1) {
                            val minX = draftPoints.minOf { it.x }
                            val maxX = draftPoints.maxOf { it.x }
                            val minY = draftPoints.minOf { it.y }
                            val maxY = draftPoints.maxOf { it.y }
                            val widthFraction = ((maxX - minX) / canvasWidthPx.toFloat()).coerceAtLeast(0.08f)
                            val heightFraction = ((maxY - minY) / canvasHeightPx.toFloat()).coerceAtLeast(0.08f)
                            val centerX = ((minX + maxX) / 2f) / canvasWidthPx.toFloat()
                            val centerY = ((minY + maxY) / 2f) / canvasHeightPx.toFloat()
                            val points = draftPoints.map { point ->
                                StrokePoint(
                                    x = ((point.x - minX) / max(maxX - minX, 1f)).coerceIn(0f, 1f),
                                    y = ((point.y - minY) / max(maxY - minY, 1f)).coerceIn(0f, 1f)
                                )
                            }
                            onDrawingComplete(
                                centerX,
                                centerY,
                                widthFraction,
                                heightFraction,
                                StrokeData(
                                    points = points,
                                    color = if (darkTheme) 0xFFF7F0E2.toInt() else 0xFF2D241C.toInt(),
                                    width = with(density) { 4.dp.toPx() }
                                )
                            )
                        }
                        draftPoints.clear()
                    },
                    onDragCancel = {
                        draftPoints.clear()
                    }
                )
            }
            .drawBehind {
                val lineColor = Color(paper.lineArgb)
                val accentColor = Color(paper.accentArgb)
                val edgeColor = Color(paper.edgeArgb)
                drawRoundRect(
                    color = edgeColor,
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                val tapeWidth = size.width * 0.28f
                val tapeHeight = size.height * 0.05f
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.68f),
                    topLeft = Offset((size.width - tapeWidth) / 2f, size.height * 0.018f),
                    size = Size(tapeWidth, tapeHeight),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
                var y = size.height * 0.18f
                while (y < size.height * 0.94f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width * 0.08f, y),
                        end = Offset(size.width * 0.92f, y),
                        strokeWidth = 1.3.dp.toPx()
                    )
                    y += size.height * 0.085f
                }
            }
    ) {
        memo.blocks.forEach { block ->
            when (block.type) {
                MemoBlockType.Text -> TextBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    darkTheme = darkTheme,
                    interactive = !isDrawingMode,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockTap = onBlockTap,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )

                MemoBlockType.Image -> ImageBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    interactive = !isDrawingMode,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockTap = onBlockTap,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )

                MemoBlockType.Drawing -> DrawingBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    interactive = !isDrawingMode,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockTap = onBlockTap,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )
            }
        }

        if (draftPoints.size > 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(draftPoints.first().x, draftPoints.first().y)
                    draftPoints.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = if (darkTheme) Color(0xFFF7F0E2) else Color(0xFF2D241C),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun TextBlockView(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    darkTheme: Boolean,
    interactive: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockTap: (String) -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val displayedText = frame.displayedText(block.text.ifBlank { "Text" })
    val blockWidthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(120)
    val blockWidthDp = with(density) { blockWidthPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - (blockWidthPx / 2f) + frame.offsetXPx).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx + frame.offsetYPx).roundToInt()
    val textAlign = when (block.textStyle.textAlign) {
        MemoTextAlign.Start -> TextAlign.Start
        MemoTextAlign.Center -> TextAlign.Center
        MemoTextAlign.End -> TextAlign.End
    }

    Text(
        text = displayedText,
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.width(blockWidthDp),
            interactive = interactive,
            onBlockTap = onBlockTap,
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag
        ).graphicsLayer {
            alpha = if (block.text.isBlank()) 0.45f else frame.alpha
            scaleX = frame.scale
            scaleY = frame.scale
            rotationZ = frame.rotationDeg
        },
        style = textStyleFor(block = block, glowRadius = frame.glowRadiusPx, darkTheme = darkTheme),
        textAlign = textAlign
    )
}

@Composable
private fun ImageBlockView(
    block: MemoBlock,
    selected: Boolean,
    interactive: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockTap: (String) -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val widthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(80)
    val heightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(80)
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - widthPx / 2f).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx - heightPx / 2f).roundToInt()
    val bitmap = rememberUriBitmap(block.imageUri)

    Box(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.size(widthDp, heightDp),
            interactive = interactive,
            onBlockTap = onBlockTap,
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag
        )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Inserted image",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DrawingBlockView(
    block: MemoBlock,
    selected: Boolean,
    interactive: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockTap: (String) -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val widthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(80)
    val heightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(80)
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - widthPx / 2f).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx - heightPx / 2f).roundToInt()

    Canvas(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.size(widthDp, heightDp),
            interactive = interactive,
            onBlockTap = onBlockTap,
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag
        )
    ) {
        block.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path().apply {
                moveTo(stroke.points.first().x * size.width, stroke.points.first().y * size.height)
                stroke.points.drop(1).forEach { point ->
                    lineTo(point.x * size.width, point.y * size.height)
                }
            }
            drawPath(
                path = path,
                color = Color(stroke.color),
                style = Stroke(width = stroke.width)
            )
        }
    }
}

@Composable
private fun blockGestureModifier(
    block: MemoBlock,
    selected: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    offsetX: Int,
    offsetY: Int,
    widthModifier: Modifier,
    interactive: Boolean,
    onBlockTap: (String) -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
): Modifier {
    return Modifier
        .then(widthModifier)
        .offset { IntOffset(offsetX, offsetY) }
        .background(
            color = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f) else Color.Transparent,
            shape = RoundedCornerShape(14.dp)
        )
        .pointerInput(block.id, canvasWidthPx, canvasHeightPx) {
            if (!interactive) return@pointerInput
            detectDragGestures(
                onDragStart = { onBlockDragStart(block.id) },
                onDrag = { _, dragAmount ->
                    onBlockDrag(
                        block.id,
                        dragAmount.x / canvasWidthPx.toFloat(),
                        dragAmount.y / canvasHeightPx.toFloat()
                    )
                }
            )
        }
        .pointerInput(block.id) {
            if (!interactive) return@pointerInput
            detectTapGestures(onTap = { onBlockTap(block.id) })
        }
}

@Composable
private fun rememberUriBitmap(imageUri: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return remember(context, imageUri) {
        imageUri?.let { uri ->
            runCatching {
                decodeImageBitmap(context, uri)
            }.getOrNull()
        }
    }
}

private fun decodeImageBitmap(context: Context, uri: String) =
    context.contentResolver.openInputStream(Uri.parse(uri)).use { input ->
        BitmapFactory.decodeStream(input)?.asImageBitmap()
    }

private fun textStyleFor(
    block: MemoBlock,
    glowRadius: Float,
    darkTheme: Boolean
): TextStyle {
    val resolvedTextColor = block.textStyle.resolvedTextColor(darkTheme = darkTheme)
    return TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = block.textStyle.fontSize.sp,
        lineHeight = (block.textStyle.fontSize * 1.22f).sp,
        color = Color(resolvedTextColor),
        shadow = if (glowRadius > 0f) {
            Shadow(
                color = Color(resolvedTextColor).copy(alpha = 0.5f),
                blurRadius = glowRadius
            )
        } else {
            Shadow(color = Color.Transparent, blurRadius = 0f)
        }
    )
}
