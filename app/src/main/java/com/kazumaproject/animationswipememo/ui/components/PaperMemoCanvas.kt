package com.kazumaproject.animationswipememo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazumaproject.animationswipememo.domain.animation.MemoAnimationEngine
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
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
    val density = LocalDensity.current
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
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
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
            val frame = MemoAnimationEngine.frameAt(block.animationStyle, block.text, progress)
            val displayedText = frame.displayedText(block.text.ifBlank { "Text" })
            val blockWidthPx = (canvasWidthPx * block.widthFraction).roundToInt().coerceAtLeast(120)
            val offsetX = (block.normalizedX * canvasWidthPx - (blockWidthPx / 2f) + frame.offsetXPx).roundToInt()
            val offsetY = (block.normalizedY * canvasHeightPx + frame.offsetYPx).roundToInt()
            val blockWidthDp = with(density) { blockWidthPx.toDp() }
            val textAlign = when (block.textStyle.textAlign) {
                MemoTextAlign.Start -> TextAlign.Start
                MemoTextAlign.Center -> TextAlign.Center
                MemoTextAlign.End -> TextAlign.End
            }
            val selected = block.id == selectedBlockId

            Text(
                text = displayedText,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(blockWidthDp)
                    .offset {
                        IntOffset(
                            x = offsetX,
                            y = offsetY
                        )
                    }
                    .graphicsLayer {
                        alpha = if (block.text.isBlank()) 0.45f else frame.alpha
                        scaleX = frame.scale
                        scaleY = frame.scale
                        rotationZ = frame.rotationDeg
                    }
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .pointerInput(block.id, canvasWidthPx, canvasHeightPx) {
                        detectDragGestures(
                            onDragStart = {
                                onBlockDragStart(block.id)
                            },
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
                        detectTapGestures(
                            onTap = {
                                onBlockTap(block.id)
                            }
                        )
                    },
                style = textStyleFor(
                    block = block,
                    glowRadius = frame.glowRadiusPx,
                    darkTheme = darkTheme
                ),
                textAlign = textAlign
            )
        }
    }
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
