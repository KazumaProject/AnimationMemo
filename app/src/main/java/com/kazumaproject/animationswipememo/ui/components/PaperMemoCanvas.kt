package com.kazumaproject.animationswipememo.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.isSpecified
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazumaproject.animationswipememo.domain.animation.MemoAnimationEngine
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockPayload
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoTextAlign
import com.kazumaproject.animationswipememo.domain.model.fitContentSize
import com.kazumaproject.animationswipememo.domain.model.resolvedContentAspectRatio
import com.kazumaproject.animationswipememo.domain.usecase.ListBlockRenderUseCase
import com.kazumaproject.animationswipememo.platform.composeFontStyle
import com.kazumaproject.animationswipememo.platform.composeFontWeight
import com.kazumaproject.animationswipememo.platform.composeTextDecoration
import com.kazumaproject.animationswipememo.platform.decodeSampledBitmap
import com.kazumaproject.animationswipememo.platform.toComposeFontFamily
import com.kazumaproject.animationswipememo.ui.components.render.KatexBlockView
import com.kazumaproject.animationswipememo.ui.components.render.highlightCode
import kotlin.math.abs
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
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit,
    onToggleListItemChecked: (String, String) -> Unit,
    onToggleListItemExpanded: (String, String) -> Unit,
    onToggleBlockExpanded: (String) -> Unit,
    onCodeBlockLongPress: (String) -> Unit
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
                    val downTouchedBlock = hitTestBlock(
                        blocks = memo.blocks,
                        tapPosition = down.position,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                        progress = progress,
                        density = density
                    )
                    Log.d(
                        PAPER_MEMO_CANVAS_TAG,
                        "tap down: consumed=${down.isConsumed}, blockId=${downTouchedBlock?.id}, blockType=${downTouchedBlock?.type}"
                    )
                    if (down.isConsumed && downTouchedBlock?.type != MemoBlockType.Latex) {
                        Log.d(PAPER_MEMO_CANVAS_TAG, "tap ignored at down: consumed by child and not LaTeX")
                        waitForUpOrCancellation(pass = PointerEventPass.Final)
                    } else {
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                        if (up != null) {
                            val upTouchedBlock = hitTestBlock(
                                blocks = memo.blocks,
                                tapPosition = up.position,
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx,
                                progress = progress,
                                density = density
                            )
                            val tappedBlock = upTouchedBlock ?: downTouchedBlock
                            val consumedByLatex = up.isConsumed && tappedBlock?.type == MemoBlockType.Latex
                            Log.d(
                                PAPER_MEMO_CANVAS_TAG,
                                "tap up: consumed=${up.isConsumed}, blockId=${tappedBlock?.id}, blockType=${tappedBlock?.type}, consumedByLatex=$consumedByLatex"
                            )
                            if (!up.isConsumed || consumedByLatex) {
                                if (tappedBlock != null) {
                                    Log.d(
                                        PAPER_MEMO_CANVAS_TAG,
                                        "onBlockTap dispatch: blockId=${tappedBlock.id}, blockType=${tappedBlock.type}"
                                    )
                                    onBlockTap(tappedBlock.id)
                                } else {
                                    Log.d(PAPER_MEMO_CANVAS_TAG, "onCanvasTap dispatch: no block hit")
                                    onCanvasTap()
                                }
                                if (!up.isConsumed) {
                                    up.consume()
                                }
                            } else {
                                Log.d(PAPER_MEMO_CANVAS_TAG, "tap ignored at up: consumed by child")
                            }
                        } else {
                            Log.d(PAPER_MEMO_CANVAS_TAG, "tap cancelled before up")
                            if (down.isConsumed && downTouchedBlock?.type == MemoBlockType.Latex) {
                                Log.d(
                                    PAPER_MEMO_CANVAS_TAG,
                                    "fallback onBlockTap dispatch: blockId=${downTouchedBlock.id}, blockType=${downTouchedBlock.type}"
                                )
                                onBlockTap(downTouchedBlock.id)
                            }
                        }
                    }
                }
            }
            .drawBehind {
                drawPaperDecorations(
                    paperStyle = memo.paperStyle,
                    palette = paper,
                    cornerRadius = 28.dp,
                    borderWidth = 2.dp
                )
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
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale
                )

                MemoBlockType.Image -> ImageBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale
                )

                MemoBlockType.Drawing -> DrawingBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale
                )

                MemoBlockType.List -> ListBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    darkTheme = darkTheme,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale,
                    onToggleListItemChecked = onToggleListItemChecked,
                    onToggleListItemExpanded = onToggleListItemExpanded
                )

                MemoBlockType.Heading,
                MemoBlockType.Quote,
                MemoBlockType.Code,
                MemoBlockType.LinkCard,
                MemoBlockType.Latex,
                MemoBlockType.Unknown -> PayloadTextBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    darkTheme = darkTheme,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale,
                    onCodeBlockLongPress = onCodeBlockLongPress
                )

                MemoBlockType.Toggle -> ToggleBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    darkTheme = darkTheme,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale,
                    onToggleExpanded = onToggleBlockExpanded
                )

                MemoBlockType.Divider -> DividerBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale
                )

                MemoBlockType.Table,
                MemoBlockType.Conversation -> PayloadCardBlockView(
                    block = block,
                    selected = block.id == selectedBlockId,
                    progress = progress,
                    darkTheme = darkTheme,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    onBlockDragStart = onBlockDragStart,
                    onBlockDrag = onBlockDrag,
                    onBlockScale = onBlockScale
                )
            }
        }
    }
}

@Composable
private fun ListBlockView(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    darkTheme: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit,
    onToggleListItemChecked: (String, String) -> Unit,
    onToggleListItemExpanded: (String, String) -> Unit
) {
    val density = LocalDensity.current
    val renderUseCase = remember { ListBlockRenderUseCase() }
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val visibleItems = remember(block.listItems, block.listAppearance) {
        renderUseCase.visibleItems(block)
    }

    val blockWidthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(140)
    val blockWidthDp = with(density) { blockWidthPx.toDp() }
    val minHeightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(64)
    val contentHeightPx = estimateListBlockHeightPx(
        itemCount = visibleItems.size,
        baseFontSizeSp = block.textStyle.fontSize,
        density = density
    )
    val blockHeightDp = with(density) { max(minHeightPx.toFloat(), contentHeightPx).toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - (blockWidthPx / 2f) + frame.offsetXPx).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx + frame.offsetYPx).roundToInt()
    val indentStepDp = (block.listAppearance?.indentStepDp ?: 16f).dp

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
                .defaultMinSize(minHeight = blockHeightDp),
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag,
            onBlockScale = onBlockScale
        ).graphicsLayer {
            alpha = frame.alpha
            scaleX = frame.scale
            scaleY = frame.scale
            rotationZ = frame.rotationDeg
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
            visibleItems.forEach { item ->
                val marker = item.markerText
                val text = if (item.text.isBlank()) "..." else item.text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp)
                        .padding(start = indentStepDp * item.indentLevel),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.canExpand) {
                        IconButton(
                            onClick = { onToggleListItemExpanded(block.id, item.itemId) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isExpanded) {
                                    Icons.Outlined.KeyboardArrowDown
                                } else {
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight
                                },
                                contentDescription = "Toggle children"
                            )
                        }
                    } else {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(20.dp))
                    }

                    if (marker == "☑" || marker == "☐") {
                        IconButton(
                            onClick = { onToggleListItemChecked(block.id, item.itemId) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = "Toggle checkbox"
                            )
                        }
                    } else {
                        Text(
                            text = marker,
                            modifier = Modifier.width(24.dp),
                            style = textStyleFor(block = block, glowRadius = frame.glowRadiusPx, darkTheme = darkTheme).copy(
                                fontSize = (block.textStyle.fontSize * item.fontScale).sp
                            )
                        )
                    }

                    Text(
                        text = text,
                        modifier = Modifier.clickable(
                            enabled = marker == "☑" || marker == "☐"
                        ) {
                            onToggleListItemChecked(block.id, item.itemId)
                        },
                        style = textStyleFor(block = block, glowRadius = frame.glowRadiusPx, darkTheme = darkTheme).copy(
                            fontSize = (block.textStyle.fontSize * item.fontScale).sp,
                            textDecoration = item.textDecoration ?: block.textStyle.composeTextDecoration()
                        )
                    )
                }
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
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit
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
            onBlockDrag = onBlockDrag,
            onBlockScale = onBlockScale
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
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit
) {
    val density = LocalDensity.current
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val widthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(80)
    val heightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(80)
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - widthPx / 2f).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx - heightPx / 2f).roundToInt()
    val glowAlpha = (frame.glowRadiusPx / 26f).coerceIn(0f, 0.4f)
    val fittedSize = fitContentSize(
        boxWidth = widthPx.toFloat(),
        boxHeight = heightPx.toFloat(),
        aspectRatio = block.resolvedContentAspectRatio()
    )
    val bitmap = rememberUriBitmap(
        imageUri = block.imageUri,
        targetWidthPx = fittedSize.width.roundToInt().coerceAtLeast(1),
        targetHeightPx = fittedSize.height.roundToInt().coerceAtLeast(1)
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
            onBlockDrag = onBlockDrag,
            onBlockScale = onBlockScale
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
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit
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
            onBlockDrag = onBlockDrag,
            onBlockScale = onBlockScale
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
private fun PayloadTextBlockView(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    darkTheme: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit,
    onCodeBlockLongPress: (String) -> Unit
) {
    val payloadText = when (val payload = block.payload) {
        is MemoBlockPayload.Heading -> payload.text.ifBlank { "Heading" }
        is MemoBlockPayload.Quote -> payload.text.ifBlank { "Quote" }
        is MemoBlockPayload.Code -> payload.code.ifBlank { "code" }
        is MemoBlockPayload.LinkCard -> payload.title.ifBlank { payload.url.ifBlank { "Link card" } }
        is MemoBlockPayload.Latex -> payload.expression.ifBlank { "LaTeX" }
        else -> block.text.ifBlank { block.type.name }
    }
    val textStyle = when (val payload = block.payload) {
        is MemoBlockPayload.Heading -> textStyleFor(block, glowRadius = 0f, darkTheme = darkTheme).copy(
            fontSize = when (payload.level) {
                com.kazumaproject.animationswipememo.domain.model.HeadingLevel.H1 -> 36.sp
                com.kazumaproject.animationswipememo.domain.model.HeadingLevel.H2 -> 30.sp
                com.kazumaproject.animationswipememo.domain.model.HeadingLevel.H3 -> 24.sp
            }
        )
        is MemoBlockPayload.Code,
        is MemoBlockPayload.Latex -> textStyleFor(block, glowRadius = 0f, darkTheme = darkTheme).copy(fontFamily = FontFamily.Monospace)
        else -> textStyleFor(block, glowRadius = 0f, darkTheme = darkTheme)
    }
    val resolvedTextColor = Color(block.textStyle.resolvedTextColor(darkTheme = darkTheme))
    val renderedText: AnnotatedString = when (val payload = block.payload) {
        is MemoBlockPayload.Code -> highlightCode(
            language = payload.language,
            code = payload.code.ifBlank { "code" },
            defaultColor = resolvedTextColor
        )

        is MemoBlockPayload.Latex -> AnnotatedString(payload.expression.ifBlank { "LaTeX" })
        else -> AnnotatedString(payloadText)
    }
    TextBlockLike(
        block = block,
        selected = selected,
        progress = progress,
        canvasWidthPx = canvasWidthPx,
        canvasHeightPx = canvasHeightPx,
        onBlockDragStart = onBlockDragStart,
        onBlockDrag = onBlockDrag,
        onBlockScale = onBlockScale,
        onLongPress = if (block.type == MemoBlockType.Code) {
            { onCodeBlockLongPress(block.id) }
        } else {
            null
        },
        content = {
            if (block.payload is MemoBlockPayload.Latex) {
                KatexBlockView(
                    expression = (block.payload as MemoBlockPayload.Latex).expression.ifBlank { "x^2" },
                    darkTheme = darkTheme,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = renderedText,
                    modifier = Modifier.fillMaxWidth(),
                    style = textStyle,
                    maxLines = if (block.type == MemoBlockType.Code) 8 else 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
private fun ToggleBlockView(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    darkTheme: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit,
    onToggleExpanded: (String) -> Unit
) {
    val toggle = block.payload as? MemoBlockPayload.Toggle ?: MemoBlockPayload.Toggle()
    TextBlockLike(
        block = block,
        selected = selected,
        progress = progress,
        canvasWidthPx = canvasWidthPx,
        canvasHeightPx = canvasHeightPx,
        onBlockDragStart = onBlockDragStart,
        onBlockDrag = onBlockDrag,
        onBlockScale = onBlockScale,
        content = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (toggle.initiallyExpanded) "▼ ${toggle.title.ifBlank { "Toggle" }}" else "▶ ${toggle.title.ifBlank { "Toggle" }}",
                    style = textStyleFor(block, glowRadius = 0f, darkTheme = darkTheme),
                    modifier = Modifier.clickable { onToggleExpanded(block.id) }
                )
                if (toggle.initiallyExpanded) {
                    toggle.childBlocks.take(3).forEach { child ->
                        Text(
                            text = "• ${child.text.ifBlank { "..." }}",
                            style = textStyleFor(block, glowRadius = 0f, darkTheme = darkTheme).copy(fontSize = (block.textStyle.fontSize * 0.85f).sp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DividerBlockView(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit
) {
    val density = LocalDensity.current
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, "", progress)
    val blockWidthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(80)
    val blockHeightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(24)
    val offsetX = (block.normalizedX * canvasWidthPx - (blockWidthPx / 2f) + frame.offsetXPx).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx + frame.offsetYPx).roundToInt()
    Box(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.size(with(density) { blockWidthPx.toDp() }, with(density) { blockHeightPx.toDp() }),
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag,
            onBlockScale = onBlockScale
        )
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(8.dp.toPx(), size.height / 2f),
                end = Offset(size.width - 8.dp.toPx(), size.height / 2f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
private fun PayloadCardBlockView(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    darkTheme: Boolean,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit
) {
    val summary = when (val payload = block.payload) {
        is MemoBlockPayload.Table -> "${payload.rows.size} rows x ${(payload.rows.maxOfOrNull { it.cells.size } ?: 0)} cols"
        is MemoBlockPayload.Conversation -> "${payload.items.size} lines"
        else -> block.type.name
    }
    TextBlockLike(
        block = block,
        selected = selected,
        progress = progress,
        canvasWidthPx = canvasWidthPx,
        canvasHeightPx = canvasHeightPx,
        onBlockDragStart = onBlockDragStart,
        onBlockDrag = onBlockDrag,
        onBlockScale = onBlockScale,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = summary,
                    style = textStyleFor(block, glowRadius = 0f, darkTheme = darkTheme),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
private fun TextBlockLike(
    block: MemoBlock,
    selected: Boolean,
    progress: Float,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    onBlockDragStart: (String) -> Unit,
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit,
    onLongPress: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
    val blockWidthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(120)
    val blockWidthDp = with(density) { blockWidthPx.toDp() }
    val minHeightPx = (canvasHeightPx * block.heightFraction).roundToInt().coerceAtLeast(56)
    val minHeightDp = with(density) { minHeightPx.toDp() }
    val offsetX = (block.normalizedX * canvasWidthPx - (blockWidthPx / 2f) + frame.offsetXPx).roundToInt()
    val offsetY = (block.normalizedY * canvasHeightPx + frame.offsetYPx).roundToInt()
    Box(
        modifier = blockGestureModifier(
            block = block,
            selected = selected,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            widthModifier = Modifier.width(blockWidthDp).defaultMinSize(minHeight = minHeightDp),
            onBlockDragStart = onBlockDragStart,
            onBlockDrag = onBlockDrag,
            onBlockScale = onBlockScale,
            onLongPress = onLongPress
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
            content()
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
    onBlockDrag: (String, Float, Float) -> Unit,
    onBlockScale: (String, Float) -> Unit,
    onLongPress: (() -> Unit)? = null
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
                val touchSlop = viewConfiguration.touchSlop
                var activePointerId = down.id
                var dragStarted = false
                var transformStarted = false
                var accumulatedDrag = Offset.Zero
                var latestEventUptime = down.uptimeMillis
                val longPressDeadline = down.uptimeMillis + CODE_BLOCK_LONG_PRESS_TIMEOUT_MS

                while (true) {
                    val longPressTimeout = if (
                        onLongPress != null &&
                        !dragStarted &&
                        !transformStarted
                    ) {
                        (longPressDeadline - latestEventUptime).coerceAtLeast(0L)
                    } else {
                        Long.MAX_VALUE
                    }
                    val event = if (longPressTimeout == Long.MAX_VALUE) {
                        awaitPointerEvent()
                    } else {
                        withTimeoutOrNull(longPressTimeout) { awaitPointerEvent() }
                    }
                    if (event == null) {
                        onLongPress?.invoke()
                        while (true) {
                            val releaseEvent = awaitPointerEvent()
                            releaseEvent.changes.forEach { change ->
                                if (change.positionChange() != Offset.Zero || !change.pressed) {
                                    change.consume()
                                }
                            }
                            if (releaseEvent.changes.none { it.pressed }) {
                                break
                            }
                        }
                        break
                    }
                    val pressedChanges = event.changes.filter { it.pressed }
                    if (pressedChanges.isEmpty()) {
                        break
                    }
                    latestEventUptime = event.changes.maxOf { it.uptimeMillis }

                    if (!transformStarted && !dragStarted && pressedChanges.size >= 2) {
                        transformStarted = true
                        onBlockDragStart(block.id)
                    }

                    if (transformStarted) {
                        val zoomChange = calculatePointerZoomChange(event.changes)
                        if (abs(zoomChange - 1f) > 0.01f) {
                            onBlockScale(block.id, zoomChange)
                        }
                        pressedChanges.forEach { change ->
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                            }
                        }
                        continue
                    }

                    val activeChange = event.changes.firstOrNull { it.id == activePointerId }
                    if (activeChange == null) {
                        activePointerId = pressedChanges.first().id
                        continue
                    }

                    val delta = activeChange.positionChange()
                    if (!dragStarted) {
                        accumulatedDrag += delta
                        if (accumulatedDrag.getDistance() > touchSlop) {
                            dragStarted = true
                            onBlockDragStart(block.id)
                            if (accumulatedDrag != Offset.Zero) {
                                activeChange.consume()
                                onBlockDrag(
                                    block.id,
                                    accumulatedDrag.x / canvasWidthPx.toFloat(),
                                    accumulatedDrag.y / canvasHeightPx.toFloat()
                                )
                                accumulatedDrag = Offset.Zero
                            }
                        }
                    } else if (delta != Offset.Zero) {
                        activeChange.consume()
                        onBlockDrag(
                            block.id,
                            delta.x / canvasWidthPx.toFloat(),
                            delta.y / canvasHeightPx.toFloat()
                        )
                    }
                }
            }
        }
}

private fun calculatePointerZoomChange(changes: List<androidx.compose.ui.input.pointer.PointerInputChange>): Float {
    val pressedChanges = changes.filter { it.pressed }
    if (pressedChanges.size < 2) return 1f

    val currentCentroid = averageOffset(pressedChanges.map { it.position })
    val previousCentroid = averageOffset(pressedChanges.map { it.previousPosition })
    if (!currentCentroid.isSpecified || !previousCentroid.isSpecified) return 1f

    val currentSpan = pressedChanges
        .map { (it.position - currentCentroid).getDistance() }
        .average()
        .toFloat()
    val previousSpan = pressedChanges
        .map { (it.previousPosition - previousCentroid).getDistance() }
        .average()
        .toFloat()

    return if (currentSpan > 0f && previousSpan > 0f) {
        currentSpan / previousSpan
    } else {
        1f
    }
}

private fun averageOffset(offsets: List<Offset>): Offset {
    if (offsets.isEmpty()) return Offset.Unspecified
    val sumX = offsets.sumOf { it.x.toDouble() }.toFloat()
    val sumY = offsets.sumOf { it.y.toDouble() }.toFloat()
    return Offset(sumX / offsets.size, sumY / offsets.size)
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

        MemoBlockType.List -> {
            val width = (canvasWidthPx * block.widthFraction).coerceAtLeast(140f)
            val minimumHeight = (canvasHeightPx * block.heightFraction).coerceAtLeast(64f)
            val listHeight = estimateListBlockHeightPx(
                itemCount = block.listItems.size.coerceAtLeast(1),
                baseFontSizeSp = block.textStyle.fontSize,
                density = density
            )
            val height = max(minimumHeight, listHeight)
            val left = (block.normalizedX * canvasWidthPx) - (width / 2f) + frame.offsetXPx
            val top = (block.normalizedY * canvasHeightPx) + frame.offsetYPx
            Rect(
                left = left - touchSlop,
                top = top - touchSlop,
                right = left + width + touchSlop,
                bottom = top + height + touchSlop
            )
        }

        MemoBlockType.Heading,
        MemoBlockType.Toggle,
        MemoBlockType.Quote,
        MemoBlockType.Code,
        MemoBlockType.Divider,
        MemoBlockType.LinkCard,
        MemoBlockType.Table,
        MemoBlockType.Conversation,
        MemoBlockType.Latex,
        MemoBlockType.Unknown -> {
            val width = (canvasWidthPx * block.widthFraction).coerceAtLeast(120f)
            val height = (canvasHeightPx * block.heightFraction).coerceAtLeast(56f)
            val left = (block.normalizedX * canvasWidthPx) - (width / 2f) + frame.offsetXPx
            val top = (block.normalizedY * canvasHeightPx) + frame.offsetYPx
            Rect(
                left = left - touchSlop,
                top = top - touchSlop,
                right = left + width + touchSlop,
                bottom = top + height + touchSlop
            )
        }
    }
}

private fun hitTestBlock(
    blocks: List<MemoBlock>,
    tapPosition: Offset,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    progress: Float,
    density: Density
): MemoBlock? {
    return blocks
        .asReversed()
        .firstOrNull { block ->
            blockBounds(
                block = block,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                progress = progress,
                density = density
            ).contains(tapPosition)
        }
}

private fun estimateListBlockHeightPx(
    itemCount: Int,
    baseFontSizeSp: Float,
    density: Density
): Float {
    val fontSizePx = with(density) { baseFontSizeSp.sp.toPx() }
    return (itemCount.coerceAtLeast(1) * fontSizePx * 1.45f) + (fontSizePx * 0.6f)
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
private fun rememberUriBitmap(
    imageUri: String?,
    targetWidthPx: Int,
    targetHeightPx: Int
): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return remember(context, imageUri, targetWidthPx, targetHeightPx) {
        imageUri?.let { uri ->
            runCatching {
                decodeImageBitmap(
                    context = context,
                    uri = uri,
                    targetWidthPx = targetWidthPx,
                    targetHeightPx = targetHeightPx
                )
            }.getOrNull()
        }
    }
}

private fun decodeImageBitmap(
    context: Context,
    uri: String,
    targetWidthPx: Int,
    targetHeightPx: Int
) = context.decodeSampledBitmap(
    uri = Uri.parse(uri),
    maxWidth = targetWidthPx,
    maxHeight = targetHeightPx
)?.asImageBitmap()

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

private const val CODE_BLOCK_LONG_PRESS_TIMEOUT_MS = 450L
private const val PAPER_MEMO_CANVAS_TAG = "PaperMemoCanvas"

