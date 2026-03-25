package com.kazumaproject.animationswipememo.domain.repository

import com.kazumaproject.animationswipememo.domain.model.SavedDrawing
import kotlinx.coroutines.flow.Flow

interface SavedDrawingRepository {
    fun observeSavedDrawings(): Flow<List<SavedDrawing>>
    suspend fun saveDrawing(drawing: SavedDrawing)
}
