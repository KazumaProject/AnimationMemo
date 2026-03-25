package com.kazumaproject.animationswipememo.domain.repository

import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    fun observeMemos(): Flow<List<MemoDraft>>
    suspend fun getMemoById(id: String): MemoDraft?
    suspend fun upsertMemo(memo: MemoDraft)
    suspend fun deleteMemo(id: String)
}
