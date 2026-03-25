package com.kazumaproject.animationswipememo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.kazumaproject.animationswipememo.domain.model.SavedDrawing
import com.kazumaproject.animationswipememo.domain.model.StrokeData
import com.kazumaproject.animationswipememo.domain.model.StrokePoint
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import kotlin.math.max

private data class DraftStroke(
    val points: List<Offset>,
    val color: Int,
    val widthPx: Float
)

private data class NormalizedDrawing(
    val strokes: List<StrokeData>,
    val widthFraction: Float,
    val heightFraction: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingEditorOverlay(
    darkTheme: Boolean,
    onClose: () -> Unit,
    onSave: (List<StrokeData>, Float, Float, Boolean, String) -> Unit
) {
    val strokes = remember { mutableStateListOf<DraftStroke>() }
    val activePoints = remember { mutableStateListOf<Offset>() }
    var canvasWidthPx by remember { mutableIntStateOf(1) }
    var canvasHeightPx by remember { mutableIntStateOf(1) }
    var saveToLibrary by rememberSaveable { mutableStateOf(false) }
    var drawingName by rememberSaveable { mutableStateOf("") }
    val strokeColor = if (darkTheme) {
        TextStyleSetting.DEFAULT_DARK_TEXT_COLOR
    } else {
        TextStyleSetting.DEFAULT_LIGHT_TEXT_COLOR
    }
    val strokeWidthPx = 7f
    val canSave = strokes.any { it.points.size > 1 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Handwriting studio")
                        Text(
                            text = "Add multiple strokes, then save to the memo.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Close handwriting studio"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        enabled = canSave,
                        onClick = {
                            normalizeDrawing(
                                strokes = strokes.toList(),
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx
                            )?.let { normalized ->
                                onSave(
                                    normalized.strokes,
                                    normalized.widthFraction,
                                    normalized.heightFraction,
                                    saveToLibrary,
                                    drawingName
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Text("Save", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = if (darkTheme) Color(0xFF221B16) else Color(0xFFF7EEDB),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (darkTheme) Color(0xFF54493C) else Color(0xFFD1BDA0),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            canvasWidthPx = it.width.coerceAtLeast(1)
                            canvasHeightPx = it.height.coerceAtLeast(1)
                        }
                        .pointerInput(darkTheme) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    activePoints.clear()
                                    activePoints.add(offset)
                                },
                                onDrag = { change, _ ->
                                    activePoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (activePoints.size > 1) {
                                        strokes.add(
                                            DraftStroke(
                                                points = activePoints.toList(),
                                                color = strokeColor,
                                                widthPx = strokeWidthPx
                                            )
                                        )
                                    }
                                    activePoints.clear()
                                },
                                onDragCancel = {
                                    activePoints.clear()
                                }
                            )
                        }
                        .padding(4.dp)
                        .background(Color.Transparent)
                        .then(
                            Modifier
                                .fillMaxSize()
                                .padding(0.dp)
                        )
                ) {
                    drawMemoGuideLines(
                        darkTheme = darkTheme,
                        canvasSize = size
                    )

                    strokes.forEach { stroke ->
                        drawDraftStroke(stroke)
                    }
                    if (activePoints.size > 1) {
                        drawDraftStroke(
                            DraftStroke(
                                points = activePoints.toList(),
                                color = strokeColor,
                                widthPx = strokeWidthPx
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = strokes.isNotEmpty(),
                    onClick = { if (strokes.isNotEmpty()) strokes.removeLast() }
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null)
                    Text("Undo", modifier = Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = strokes.isNotEmpty(),
                    onClick = {
                        strokes.clear()
                        activePoints.clear()
                    }
                ) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                    Text("Clear", modifier = Modifier.padding(start = 6.dp))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Save to handwriting library",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Turn this on to reuse the drawing from the handwriting button later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = saveToLibrary,
                    onCheckedChange = { saveToLibrary = it }
                )
            }

            if (saveToLibrary) {
                OutlinedTextField(
                    value = drawingName,
                    onValueChange = { drawingName = it.take(48) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Drawing name") },
                    placeholder = { Text("Short label for the library") },
                    leadingIcon = {
                        Icon(Icons.Outlined.AutoFixHigh, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
fun SavedDrawingLibrarySheet(
    savedDrawings: List<SavedDrawing>,
    onNewDrawing: () -> Unit,
    onInsertDrawing: (SavedDrawing) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Handwriting library",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Create a fresh drawing or drop a saved one onto the memo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNewDrawing
        ) {
            Icon(Icons.Outlined.AutoFixHigh, contentDescription = null)
            Text("New drawing", modifier = Modifier.padding(start = 6.dp))
        }

        if (savedDrawings.isEmpty()) {
            Text(
                text = "No saved drawings yet. Create one and turn on library saving to reuse it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            savedDrawings.forEach { drawing ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SavedDrawingPreview(
                        drawing = drawing,
                        modifier = Modifier.size(width = 112.dp, height = 84.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = drawing.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${drawing.strokes.size} strokes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { onInsertDrawing(drawing) }) {
                        Text("Insert")
                    }
                }
            }
        }
    }
}

@Composable
fun SavedDrawingPreview(
    drawing: SavedDrawing,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawing.strokes.forEach { stroke ->
                if (stroke.points.size < 2) return@forEach
                val path = Path().apply {
                    moveTo(
                        stroke.points.first().x * size.width,
                        stroke.points.first().y * size.height
                    )
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
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMemoGuideLines(
    darkTheme: Boolean,
    canvasSize: Size
) {
    val lineColor = if (darkTheme) Color(0xFF473B31) else Color(0xFFDDC9AC)
    var y = canvasSize.height * 0.16f
    while (y < canvasSize.height * 0.94f) {
        drawLine(
            color = lineColor,
            start = Offset(canvasSize.width * 0.08f, y),
            end = Offset(canvasSize.width * 0.92f, y),
            strokeWidth = 1.6f
        )
        y += canvasSize.height * 0.095f
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDraftStroke(stroke: DraftStroke) {
    if (stroke.points.size < 2) return
    val path = Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
    }
    drawPath(
        path = path,
        color = Color(stroke.color),
        style = Stroke(width = stroke.widthPx)
    )
}

private fun normalizeDrawing(
    strokes: List<DraftStroke>,
    canvasWidthPx: Int,
    canvasHeightPx: Int
): NormalizedDrawing? {
    val validStrokes = strokes.filter { it.points.size > 1 }
    if (validStrokes.isEmpty()) return null

    val allPoints = validStrokes.flatMap { it.points }
    val minX = allPoints.minOf { it.x }
    val maxX = allPoints.maxOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxY = allPoints.maxOf { it.y }
    val widthPx = max(maxX - minX, 1f)
    val heightPx = max(maxY - minY, 1f)

    val normalizedStrokes = validStrokes.map { stroke ->
        StrokeData(
            points = stroke.points.map { point ->
                StrokePoint(
                    x = ((point.x - minX) / widthPx).coerceIn(0f, 1f),
                    y = ((point.y - minY) / heightPx).coerceIn(0f, 1f)
                )
            },
            color = stroke.color,
            width = stroke.widthPx
        )
    }

    return NormalizedDrawing(
        strokes = normalizedStrokes,
        widthFraction = (widthPx / canvasWidthPx.toFloat()).coerceIn(0.12f, 0.78f),
        heightFraction = (heightPx / canvasHeightPx.toFloat()).coerceIn(0.12f, 0.78f)
    )
}
