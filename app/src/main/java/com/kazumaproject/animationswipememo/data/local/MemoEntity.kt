package com.kazumaproject.animationswipememo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey val id: String,
    val paperStyle: String,
    val blocksJson: String,
    val createdAt: Long,
    val updatedAt: Long
)
