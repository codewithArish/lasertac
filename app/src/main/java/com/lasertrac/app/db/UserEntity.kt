package com.lasertrac.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user in the local Room database.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    // Note: Storing passwords in plaintext is not secure. 
    // For a real application, this should be a securely hashed password.
    val passwordHash: String 
)
