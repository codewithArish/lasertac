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
) {
    companion object {
        val predefinedViolations = listOf(
            Violation(id = 1, title = "Speeding", description = "Exceeding the speed limit.", timestamp = System.currentTimeMillis()),
            Violation(id = 2, title = "Red Light", description = "Running a red light.", timestamp = System.currentTimeMillis()),
            Violation(id = 3, title = "Illegal Parking", description = "Parking in a restricted area.", timestamp = System.currentTimeMillis()),
            Violation(id = 4, title = "Invalid License", description = "Driving with an invalid license.", timestamp = System.currentTimeMillis()),
            Violation(id = 5, title = "Seat Belt", description = "Not wearing a seat belt.", timestamp = System.currentTimeMillis())
        )
    }
}