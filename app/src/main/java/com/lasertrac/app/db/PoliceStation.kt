package com.lasertrac.app.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "police_stations")
data class PoliceStation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "city")
    val city: String,

    @ColumnInfo(name = "area_location")
    val areaLocation: String,

    @ColumnInfo(name = "police_station")
    val policeStationName: String, // Renamed to avoid conflict with class name if it were PoliceStation

    @ColumnInfo(name = "contact")
    val contact: String?,

    @ColumnInfo(name = "source")
    val source: String?
)
