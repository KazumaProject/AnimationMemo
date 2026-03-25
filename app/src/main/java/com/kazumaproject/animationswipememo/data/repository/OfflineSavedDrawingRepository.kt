package com.kazumaproject.animationswipememo.data.repository

import com.kazumaproject.animationswipememo.data.local.SavedDrawingDao
import com.kazumaproject.animationswipememo.data.local.toDomain
import com.kazumaproject.animationswipememo.data.local.toEntity
import com.kazumaproject.animationswipememo.domain.model.SavedDrawing
import com.kazumaproject.animationswipememo.domain.repository.SavedDrawingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineSavedDrawingRepository(
    private val savedDrawingDao: SavedDrawingDao
) : SavedDrawingRepository {
    override fun observeSavedDrawings(): Flow<List<SavedDrawing>> {
        return savedDrawingDao.observeSavedDrawings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveDrawing(drawing: SavedDrawing) {
        savedDrawingDao.upsertSavedDrawing(drawing.toEntity())
    }
}
