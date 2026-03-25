package com.kazumaproject.animationswipememo.data.repository

import com.kazumaproject.animationswipememo.data.local.MemoDao
import com.kazumaproject.animationswipememo.data.local.toDomain
import com.kazumaproject.animationswipememo.data.local.toEntity
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineMemoRepository(
    private val memoDao: MemoDao
) : MemoRepository {
    override fun observeMemos(): Flow<List<MemoDraft>> {
        return memoDao.observeMemos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMemoById(id: String): MemoDraft? {
        return memoDao.getMemoById(id)?.toDomain()
    }

    override suspend fun upsertMemo(memo: MemoDraft) {
        memoDao.upsertMemo(memo.toEntity())
    }

    override suspend fun deleteMemo(id: String) {
        memoDao.deleteMemo(id)
    }
}
