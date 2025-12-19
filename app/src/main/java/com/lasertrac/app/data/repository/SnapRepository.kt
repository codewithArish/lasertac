package com.lasertrac.app.data.repository

import com.lasertrac.app.db.SnapDao
import com.lasertrac.app.db.SnapDetail
import kotlinx.coroutines.flow.Flow

class SnapRepository(
    private val snapDao: SnapDao,
    private val localMediaRepository: LocalMediaRepository
) {

    fun getLocalDbSnaps(): Flow<List<SnapDetail>> = snapDao.getAllSnaps()

    fun getSnapsByDate(date: String): Flow<List<SnapDetail>> {
        return snapDao.getSnapsByDate(date)
    }

    suspend fun getLocalMediaSnaps(): Result<List<SnapDetail>> {
        return localMediaRepository.getLocalMediaSnaps()
    }

    /**
     * Fetches the latest snaps from local media, inserts them into the database,
     * and returns the number of *new* snaps that were added.
     */
    suspend fun refreshSnapsFromLocalMedia(): Result<Int> {
        return try {
            val mediaSnapsResult = localMediaRepository.getLocalMediaSnaps()

            if (mediaSnapsResult.isSuccess) {
                val mediaSnaps = mediaSnapsResult.getOrThrow()
                if (mediaSnaps.isNotEmpty()) {
                    val insertResults = snapDao.insertAll(mediaSnaps)
                    val newSnapsCount = insertResults.count { it != -1L }
                    Result.success(newSnapsCount)
                } else {
                    Result.success(0) // No snaps found in media
                }
            } else {
                Result.failure(mediaSnapsResult.exceptionOrNull() ?: Exception("Unknown error fetching from media"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSnaps(snapIds: List<String>) {
        snapDao.deleteSnaps(snapIds)
    }
}
