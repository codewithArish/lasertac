package com.lasertrac.app.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_snap_locations")
data class SavedSnapLocationEntity(
    @PrimaryKey
    @ColumnInfo(name = "snap_id")
    val snapId: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "full_address")
    val fullAddress: String,

    @ColumnInfo(name = "district")
    val district: String,

    @ColumnInfo(name = "country")
    val country: String,

    @ColumnInfo(name = "selected_city")
    val selectedCity: String,

    @ColumnInfo(name = "selected_state")
    val selectedState: String,

    @ColumnInfo(name = "selected_police_area")
    val selectedPoliceArea: String
)
