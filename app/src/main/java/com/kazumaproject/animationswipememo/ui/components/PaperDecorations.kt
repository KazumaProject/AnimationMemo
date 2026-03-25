package com.kazumaproject.animationswipememo.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kazumaproject.animationswipememo.domain.model.PaperPalette
import com.kazumaproject.animationswipememo.domain.model.PaperPattern
import com.kazumaproject.animationswipememo.domain.model.PaperStyle

fun DrawScope.drawPaperDecorations(
    paperStyle: PaperStyle,
    palette: PaperPalette,
    cornerRadius: Dp,
    borderWidth: Dp
) {
    val cornerRadiusPx = cornerRadius.toPx()
    val borderWidthPx = borderWidth.toPx()
    val lineColor = Color(palette.lineArgb)
    val accentColor = Color(palette.accentArgb)
    val edgeColor = Color(palette.edgeArgb)

    drawRoundRect(
        color = edgeColor,
        cornerRadius = CornerRadius(cornerRadiusPx),
        style = Stroke(width = borderWidthPx)
    )

    if (paperStyle.showTape) {
        val tapeWidth = size.width * 0.28f
        val tapeHeight = size.height * 0.05f
        drawRoundRect(
            color = accentColor.copy(alpha = 0.68f),
            topLeft = Offset((size.width - tapeWidth) / 2f, size.height * 0.018f),
            size = Size(tapeWidth, tapeHeight),
            cornerRadius = CornerRadius(tapeHeight * 0.36f)
        )
    }

    when (paperStyle.pattern) {
        PaperPattern.Lined -> drawLinedPaper(lineColor)
        PaperPattern.Blank -> Unit
        PaperPattern.Grid -> drawGridPaper(lineColor, accentColor)
        PaperPattern.DotGrid -> drawDotGrid(accentColor)
        PaperPattern.Margin -> drawMarginPaper(lineColor, accentColor)
        PaperPattern.Music -> drawMusicPaper(lineColor, accentColor)
    }
}

private fun DrawScope.drawLinedPaper(lineColor: Color) {
    var y = size.height * 0.18f
    while (y < size.height * 0.94f) {
        drawLine(
            color = lineColor,
            start = Offset(size.width * 0.08f, y),
            end = Offset(size.width * 0.92f, y),
            strokeWidth = 1.2.dp.toPx()
        )
        y += size.height * 0.085f
    }
}

private fun DrawScope.drawGridPaper(
    lineColor: Color,
    accentColor: Color
) {
    val top = size.height * 0.12f
    val bottom = size.height * 0.94f
    val left = size.width * 0.08f
    val right = size.width * 0.92f
    val rowStep = size.height * 0.08f
    val columnStep = size.width * 0.12f
    var row = 0
    var y = top
    while (y <= bottom) {
        drawLine(
            color = if (row % 4 == 0) accentColor.copy(alpha = 0.6f) else lineColor,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 1.dp.toPx()
        )
        row++
        y += rowStep
    }
    var column = 0
    var x = left
    while (x <= right) {
        drawLine(
            color = if (column % 4 == 0) accentColor.copy(alpha = 0.52f) else lineColor.copy(alpha = 0.92f),
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = 1.dp.toPx()
        )
        column++
        x += columnStep
    }
}

private fun DrawScope.drawDotGrid(accentColor: Color) {
    val top = size.height * 0.16f
    val bottom = size.height * 0.92f
    val left = size.width * 0.1f
    val right = size.width * 0.9f
    val rowStep = size.height * 0.08f
    val columnStep = size.width * 0.11f
    var y = top
    while (y <= bottom) {
        var x = left
        while (x <= right) {
            drawCircle(
                color = accentColor.copy(alpha = 0.5f),
                radius = 1.6.dp.toPx(),
                center = Offset(x, y)
            )
            x += columnStep
        }
        y += rowStep
    }
}

private fun DrawScope.drawMarginPaper(
    lineColor: Color,
    accentColor: Color
) {
    drawLinedPaper(lineColor)
    val marginX = size.width * 0.18f
    drawLine(
        color = accentColor.copy(alpha = 0.92f),
        start = Offset(marginX, size.height * 0.14f),
        end = Offset(marginX, size.height * 0.94f),
        strokeWidth = 1.6.dp.toPx()
    )
}

private fun DrawScope.drawMusicPaper(
    lineColor: Color,
    accentColor: Color
) {
    val left = size.width * 0.08f
    val right = size.width * 0.92f
    val top = size.height * 0.18f
    val staffGap = size.height * 0.06f
    val lineGap = size.height * 0.022f
    var staffTop = top
    while (staffTop + (lineGap * 4f) < size.height * 0.92f) {
        repeat(5) { index ->
            val y = staffTop + (lineGap * index)
            drawLine(
                color = lineColor,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.15.dp.toPx()
            )
        }
        drawLine(
            color = accentColor.copy(alpha = 0.72f),
            start = Offset(left + 4.dp.toPx(), staffTop - 2.dp.toPx()),
            end = Offset(left + 4.dp.toPx(), staffTop + (lineGap * 4f) + 2.dp.toPx()),
            strokeWidth = 1.4.dp.toPx()
        )
        staffTop += staffGap + (lineGap * 4f)
    }
}
