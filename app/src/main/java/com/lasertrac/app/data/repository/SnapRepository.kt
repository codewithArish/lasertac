package com.lasertrac.app.data.repository

import com.lasertrac.app.db.SnapDao
import com.lasertrac.app.db.SnapDetail
import kotlinx.coroutines.flow.Flow

class SnapRepository(
    private val snapDao: SnapDao,
    private val localMediaRepository: LocalMediaRepository
) {

    // This now directly returns the Flow from the DAO.
    fun getLocalDbSnaps(): Flow<List<SnapDetail>> = snapDao.getAllSnaps()

    // This remains a suspend function to fetch from local storage.
    suspend fun getLocalMediaSnaps(): Result<List<SnapDetail>> {
        return localMediaRepository.getLocalMediaSnaps()
    }

    suspend fun deleteSnaps(snapIds: List<String>) {
        snapDao.deleteSnaps(snapIds)
    }
}
