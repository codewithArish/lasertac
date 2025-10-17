package com.lasertrac.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "violations")
data class ViolationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actId: String,
    val actName: String
)


