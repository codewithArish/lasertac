package com.lasertrac.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class User(
    @PrimaryKey
    val serverId: Int = 0,
    val name: String,
    val email: String,
    val pass: String,
    val isSynced: Boolean = false
)
