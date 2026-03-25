package com.kazumaproject.animationswipememo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY updatedAt DESC")
    fun observeMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    suspend fun getMemoById(id: String): MemoEntity?

    @Upsert
    suspend fun upsertMemo(memo: MemoEntity)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteMemo(id: String)

    @Query("DELETE FROM memos")
    suspend fun deleteAllMemos()
}
