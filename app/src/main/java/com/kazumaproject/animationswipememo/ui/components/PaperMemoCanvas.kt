package com.kazumaproject.animationswipememo.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import kotlin.math.roundToInt

@Composable
fun PaperMemoCanvas(
    memo: MemoDraft,
    selectedBlockId: String?,
    progress: Float,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onBlockTap: (String) -> Unit,
    onCanvasTap: () -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
) {
    val paper = memo.paperStyle.palette(darkTheme)
    val shape = RoundedCornerShape(28.dp)
    var canvasWidthPx by remember { mutableIntStateOf(1) }
    var canvasHeightPx by remember { mutableIntStateOf(1) }

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
                onClick = onCanvasTap
            )
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
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockTap = onBlockTap,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )

                MemoBlockType.Image -> ImageBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockTap = onBlockTap,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )

                MemoBlockType.Drawing -> DrawingBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockTap = onBlockTap,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
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
    progress: Float,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockTap: (String) -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val widthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(80)
    val heightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(80)
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - widthPx / 2f).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx - heightPx / 2f).roundToInt()
    val bitmap = rememberUriBitmap(block.imageUri)
    val glowAlpha = (frame.glowRadiusPx / 26f).coerceIn(0f, 0.4f)

    Box(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.size(widthDp, heightDp),
            onBlockTap = onBlockTap,
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag
        ).graphicsLayer {
            alpha = frame.alpha
            scaleX = frame.scale
            scaleY = frame.scale
            rotationZ = frame.rotationDeg
            translationX = frame.offsetXPx
            translationY = frame.offsetYPx
        }.drawBehind {
            if (glowAlpha > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = glowAlpha),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
            }
        }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Inserted image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    ),
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
    progress: Float,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockTap: (String) -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val widthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(80)
    val heightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(80)
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - widthPx / 2f).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx - heightPx / 2f).roundToInt()
    val glowAlpha = (frame.glowRadiusPx / 26f).coerceIn(0f, 0.28f)

    androidx.compose.foundation.Canvas(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.size(widthDp, heightDp),
            onBlockTap = onBlockTap,
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag
        ).graphicsLayer {
            alpha = frame.alpha
            scaleX = frame.scale
            scaleY = frame.scale
            rotationZ = frame.rotationDeg
            translationX = frame.offsetXPx
            translationY = frame.offsetYPx
        }.drawBehind {
            if (glowAlpha > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = glowAlpha),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
            }
        }
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
    onBlockTap: (String) -> Unit,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
): Modifier {
    return Modifier
        .then(widthModifier)
        .offset { IntOffset(offsetX, offsetY) }
        .background(
            color = if (selected) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
            } else {
                Color.Transparent
            },
            shape = RoundedCornerShape(14.dp)
        )
        .pointerInput(block.id, canvasWidthPx, canvasHeightPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragStarted = false
                val postSlop = awaitTouchSlopOrCancellation(down.id) { change, over ->
                    if (!dragStarted) {
                        dragStarted = true
                        onBlockDragStart(block.id)
                    }
                    change.consume()
                    onBlockDrag(
                        block.id,
                        over.x / canvasWidthPx.toFloat(),
                        over.y / canvasHeightPx.toFloat()
                    )
                }

                if (!dragStarted) {
                    if (waitForUpOrCancellation() != null) {
                        onBlockTap(block.id)
                    }
                } else if (postSlop != null) {
                    var pointerChange = postSlop
                    while (true) {
                        val currentChange = pointerChange ?: break
                        if (currentChange.changedToUpIgnoreConsumed()) break
                        val delta = currentChange.positionChange()
                        if (delta != Offset.Zero) {
                            currentChange.consume()
                            onBlockDrag(
                                block.id,
                                delta.x / canvasWidthPx.toFloat(),
                                delta.y / canvasHeightPx.toFloat()
                            )
                        }
                        val event = awaitPointerEvent()
                        val tracked = event.changes.firstOrNull { it.id == down.id } ?: break
                        pointerChange = tracked
                    }
                }
            }
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
