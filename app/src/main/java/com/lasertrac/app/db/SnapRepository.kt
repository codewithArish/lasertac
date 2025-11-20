package com.lasertrac.app.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SnapRepository {
    fun getSnap(snapId: String): Flow<SnapDetail?>
}

class SnapRepositoryImpl(private val snapDao: SnapDao) : SnapRepository {
    override fun getSnap(snapId: String): Flow<SnapDetail?> {
        return snapDao.getSnapById(snapId)
    }
}
