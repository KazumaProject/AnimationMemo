package com.kazumaproject.animationswipememo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDrawingDao {
    @Query("SELECT * FROM saved_drawings ORDER BY updatedAt DESC")
    fun observeSavedDrawings(): Flow<List<SavedDrawingEntity>>

    @Upsert
    suspend fun upsertSavedDrawing(drawing: SavedDrawingEntity)
}
