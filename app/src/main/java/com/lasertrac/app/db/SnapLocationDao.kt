package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the SavedSnapLocationEntity, now using Flow.
 */
@Dao
interface SnapLocationDao {

    /**
     * Inserts or updates a snap location. This is an "upsert" operation.
     */
    @Upsert
    suspend fun insertOrUpdateSnapLocation(snapLocation: SavedSnapLocationEntity)

    /**
     * Fetches all saved snap locations from the database, ordered by timestamp.
     * Returns a Flow to enable reactive updates.
     */
    @Query("SELECT * FROM saved_snap_location ORDER BY timestamp DESC")
    fun getAllSnapLocations(): Flow<List<SavedSnapLocationEntity>>

    /**
     * Fetches a single snap location by its unique snapId.
     * Returns a Flow to enable reactive updates.
     */
    @Query("SELECT * FROM saved_snap_location WHERE snapId = :snapId LIMIT 1")
    fun getSnapLocationById(snapId: String): Flow<SavedSnapLocationEntity?>

    /**
     * Deletes a list of snaps by their IDs.
     */
    @Query("DELETE FROM saved_snap_location WHERE snapId IN (:snapIds)")
    suspend fun deleteSnapsByIds(snapIds: List<String>)

}
