package com.lasertrac.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_snap_location")
data class SavedSnapLocationEntity(
    @PrimaryKey
    val snapId: String,
    val latitude: Double,
    val longitude: Double,
    val fullAddress: String,
    val district: String,
    val country: String,
    val selectedCity: String,
    val selectedState: String,
    val selectedPoliceArea: String,
    val imageUri: String? = null // Add this line to store the image path
)
