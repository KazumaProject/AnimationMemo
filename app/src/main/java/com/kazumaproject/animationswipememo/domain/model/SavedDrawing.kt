package com.kazumaproject.animationswipememo.domain.model

data class SavedDrawing(
    val id: String,
    val name: String,
    val strokes: List<StrokeData>,
    val widthFraction: Float,
    val heightFraction: Float,
    val createdAt: Long,
    val updatedAt: Long
)
