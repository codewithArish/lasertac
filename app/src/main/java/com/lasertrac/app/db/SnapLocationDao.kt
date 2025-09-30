package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSnapLocation(snapLocation: SavedSnapLocationEntity)

    @Query("SELECT * FROM saved_snap_locations WHERE snap_id = :snapId LIMIT 1")
    fun getSnapLocationById(snapId: String): Flow<SavedSnapLocationEntity?>

    @Query("SELECT * FROM saved_snap_locations ORDER BY snap_id ASC")
    fun getAllSnapLocations(): Flow<List<SavedSnapLocationEntity>>
}
