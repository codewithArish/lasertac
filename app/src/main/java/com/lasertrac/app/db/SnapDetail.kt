package com.lasertrac.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "snap_details")
@TypeConverters(SnapStatusConverter::class)
data class SnapDetail(
    @PrimaryKey
    val id: String,
    val regNr: String,
    val evidenceDate: String,
    val dateTime: String,
    val status: SnapStatus,
    val speed: Int,
    val deviceId: String,
    val operatorId: String,
    val speedLimit: Int,
    val location: String,
    val violationDistance: String,
    val recordNr: String,
    val latitude: String,
    val longitude: String,
    val district: String,
    val policeStation: String,
    val address: String,
    val uploadStatus: String,
    val mainImage: String?,
    val licensePlateImage: String?,
    val mapImage: String?,
    val violationSummary: String,
    val violationManagementLink: String,
    val accessLink: String,
    val regNrStatus: String
)

class SnapStatusConverter {
    @TypeConverter
    fun fromSnapStatus(status: SnapStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSnapStatus(status: String): SnapStatus {
        return SnapStatus.valueOf(status)
    }
}
