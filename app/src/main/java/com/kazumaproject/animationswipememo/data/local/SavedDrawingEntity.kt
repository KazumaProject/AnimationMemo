package com.kazumaproject.animationswipememo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_drawings")
data class SavedDrawingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val strokesJson: String,
    val widthFraction: Float,
    val heightFraction: Float,
    val createdAt: Long,
    val updatedAt: Long
)
