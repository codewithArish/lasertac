package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM app_users WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): Flow<User?>

    @Query("SELECT * FROM app_users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<User>
}
