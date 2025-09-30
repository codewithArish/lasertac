package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoliceStationDao {

    @Query("SELECT DISTINCT city FROM police_stations ORDER BY city ASC")
    fun getAllDistinctCities(): Flow<List<String>>

    // This will be used for the "State" dropdown, as your DB groups by city (e.g., Delhi, Gurugram)
    @Query("SELECT DISTINCT city FROM police_stations ORDER BY city ASC")
    fun getAllDistinctStates(): Flow<List<String>> // Alias for city for state dropdown logic

    @Query("SELECT police_station FROM police_stations WHERE city = :cityName ORDER BY police_station ASC")
    fun getPoliceStationsByCity(cityName: String): Flow<List<String>>

    // Optional: Get all police station records if ever needed
    @Query("SELECT * FROM police_stations ORDER BY city, police_station ASC")
    fun getAllPoliceStations(): Flow<List<PoliceStation>>

    // You might need a method to get a specific police station if details are required
    @Query("SELECT * FROM police_stations WHERE police_station = :policeStationName LIMIT 1")
    suspend fun getPoliceStationByName(policeStationName: String): PoliceStation?
}
