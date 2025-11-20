package com.lasertrac.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_registrations")
data class OfflineRegistrationRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    val pass: String
)