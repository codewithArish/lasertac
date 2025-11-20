package com.lasertrac.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "violations")
data class Violation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: Long
)
