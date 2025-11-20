package com.lasertrac.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflineRegistrationDao {
    @Insert
    suspend fun insert(request: OfflineRegistrationRequest)

    @Query("SELECT * FROM offline_registrations")
    suspend fun getAll(): List<OfflineRegistrationRequest>

    @Query("DELETE FROM offline_registrations WHERE id = :id")
    suspend fun delete(id: Int)
}