package com.lasertrac.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved location, now with detailed address fields.
 */
@Entity(tableName = "saved_snap_location")
data class SavedSnapLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val snapId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long,
    val imageUri: String? = null,
    val fullAddress: String? = null,
    val selectedState: String? = null,
    val selectedCity: String? = null,
    val district: String? = null,
    val country: String? = null,
    val selectedPoliceArea: String? = null
)
