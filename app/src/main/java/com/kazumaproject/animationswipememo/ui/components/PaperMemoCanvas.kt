package com.kazumaproject.animationswipememo.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazumaproject.animationswipememo.domain.animation.MemoAnimationEngine
import com.kazumaproject.animationswipememo.domain.model.fitContentSize
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoTextAlign
import com.kazumaproject.animationswipememo.domain.model.resolvedContentAspectRatio
import com.kazumaproject.animationswipememo.platform.composeFontStyle
import com.kazumaproject.animationswipememo.platform.composeFontWeight
import com.kazumaproject.animationswipememo.platform.composeTextDecoration
import com.kazumaproject.animationswipememo.platform.toComposeFontFamily
import kotlin.math.ceil
import kotlin.math.max
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
    val density = LocalDensity.current
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
            .pointerInput(memo.id, memo.updatedAt) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Final
                    )
                    if (down.isConsumed) {
                        waitForUpOrCancellation(pass = PointerEventPass.Final)
                    } else {
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                        if (up != null && !up.isConsumed) {
                            val tappedBlockId = memo.blocks
                                .asReversed()
                                .firstOrNull { block ->
                                    blockBounds(
                                        block = block,
                                        canvasWidthPx = canvasWidthPx,
                                        canvasHeightPx = canvasHeightPx,
                                        progress = progress,
                                        density = density
                                    ).contains(up.position)
                                }
                                ?.id
                            if (tappedBlockId != null) {
                                onBlockTap(tappedBlockId)
                            } else {
                                onCanvasTap()
                            }
                        }
                    }
                }
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
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )

                MemoBlockType.Image -> ImageBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag
                )

                MemoBlockType.Drawing -> DrawingBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
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
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val displayedText = frame.displayedText(block.text.ifBlank { "Text" })
    val blockWidthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(120)
    val blockWidthDp = with(density) { blockWidthPx.toDp() }
    val minimumBlockHeightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(56)
    val minimumBlockHeightDp = with(density) { minimumBlockHeightPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - (blockWidthPx / 2f) + frame.offsetXPx).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx + frame.offsetYPx).roundToInt()
    val textAlign = when (block.textStyle.textAlign) {
        MemoTextAlign.Start -> TextAlign.Start
        MemoTextAlign.Center -> TextAlign.Center
        MemoTextAlign.End -> TextAlign.End
    }

    Box(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier
                .width(blockWidthDp)
                .defaultMinSize(minHeight = minimumBlockHeightDp),
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag
        ).graphicsLayer {
            alpha = if (block.text.isBlank()) 0.45f else frame.alpha
            scaleX = frame.scale
            scaleY = frame.scale
            rotationZ = frame.rotationDeg
        }
    ) {
        Text(
            text = displayedText,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            style = textStyleFor(block = block, glowRadius = frame.glowRadiusPx, darkTheme = darkTheme),
            textAlign = textAlign
        )
    }
}

@Composable
private fun ImageBlockView(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
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
    val fittedSize = fitContentSize(
        boxWidth = widthPx.toFloat(),
        boxHeight = heightPx.toFloat(),
        aspectRatio = block.resolvedContentAspectRatio()
    )
    val fittedWidthDp = with(density) { fittedSize.width.toDp() }
    val fittedHeightDp = with(density) { fittedSize.height.toDp() }

    Box(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.size(widthDp, heightDp),
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
                    .size(fittedWidthDp, fittedHeightDp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(fittedWidthDp, fittedHeightDp)
                    .align(Alignment.Center)
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
    val fittedSize = fitContentSize(
        boxWidth = widthPx.toFloat(),
        boxHeight = heightPx.toFloat(),
        aspectRatio = block.resolvedContentAspectRatio()
    )
    val paddingX = (widthPx - fittedSize.width) / 2f
    val paddingY = (heightPx - fittedSize.height) / 2f

    androidx.compose.foundation.Canvas(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.size(widthDp, heightDp),
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
                moveTo(
                    paddingX + (stroke.points.first().x * fittedSize.width),
                    paddingY + (stroke.points.first().y * fittedSize.height)
                )
                stroke.points.drop(1).forEach { point ->
                    lineTo(
                        paddingX + (point.x * fittedSize.width),
                        paddingY + (point.y * fittedSize.height)
                    )
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
                val dragChange = awaitTouchSlopOrCancellation(down.id) { change, over ->
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

                if (!dragStarted || dragChange == null) {
                    waitForUpOrCancellation()
                } else {
                    var currentChange = dragChange
                    while (currentChange != null && currentChange.pressed) {
                        val delta = currentChange.positionChange()
                        if (delta != Offset.Zero) {
                            currentChange.consume()
                            onBlockDrag(
                                block.id,
                                delta.x / canvasWidthPx.toFloat(),
                                delta.y / canvasHeightPx.toFloat()
                            )
                        }
                        currentChange = awaitDragOrCancellation(down.id)
                    }
                }
            }
        }
}

private fun blockBounds(
    block: MemoBlock,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    progress: Float,
    density: Density
): Rect {
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val touchSlop = 16f
    return when (block.type) {
        MemoBlockType.Text -> {
            val width = (canvasWidthPx * block.widthFraction).coerceAtLeast(120f)
            val minimumHeight = (canvasHeightPx * block.heightFraction).coerceAtLeast(56f)
            val height = estimateTextBlockHeightPx(
                text = frame.displayedText(block.text.ifBlank { "Text" }),
                widthPx = width,
                minimumHeightPx = minimumHeight,
                fontSizeSp = block.textStyle.fontSize,
                density = density
            )
            val left = (block.normalizedX * canvasWidthPx) - (width / 2f) + frame.offsetXPx
            val top = (block.normalizedY * canvasHeightPx) + frame.offsetYPx
            Rect(
                left = left - touchSlop,
                top = top - touchSlop,
                right = left + width + touchSlop,
                bottom = top + height + touchSlop
            )
        }

        MemoBlockType.Image,
        MemoBlockType.Drawing -> {
            val width = (canvasWidthPx * block.widthFraction).coerceAtLeast(80f)
            val height = (canvasHeightPx * block.heightFraction).coerceAtLeast(80f)
            val left = (block.normalizedX * canvasWidthPx) - (width / 2f) + frame.offsetXPx
            val top = (block.normalizedY * canvasHeightPx) - (height / 2f) + frame.offsetYPx
            Rect(
                left = left - touchSlop,
                top = top - touchSlop,
                right = left + width + touchSlop,
                bottom = top + height + touchSlop
            )
        }
    }
}

private fun estimateTextBlockHeightPx(
    text: String,
    widthPx: Float,
    minimumHeightPx: Float,
    fontSizeSp: Float,
    density: Density
): Float {
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }
    val charsPerLine = max(1f, widthPx / (fontSizePx * 0.58f))
    val lineCount = text
        .split('\n')
        .sumOf { paragraph ->
            max(1, ceil(paragraph.length.coerceAtLeast(1) / charsPerLine).toInt())
        }
        .coerceAtLeast(1)
    val lineHeightPx = fontSizePx * 1.22f
    val contentHeightPx = (lineCount * lineHeightPx) + (fontSizePx * 0.4f)
    return max(minimumHeightPx, contentHeightPx)
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
        fontFamily = block.textStyle.fontFamily.toComposeFontFamily(),
        fontSize = block.textStyle.fontSize.sp,
        lineHeight = (block.textStyle.fontSize * 1.22f).sp,
        color = Color(resolvedTextColor),
        fontWeight = block.textStyle.composeFontWeight(),
        fontStyle = block.textStyle.composeFontStyle(),
        textDecoration = block.textStyle.composeTextDecoration(),
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
