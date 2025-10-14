package com.lasertrac.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PoliceStation::class, SavedSnapLocationEntity::class], version = 2) // Incremented version
abstract class AppDatabase : RoomDatabase() {
    abstract fun policeStationDao(): PoliceStationDao
    abstract fun snapLocationDao(): SnapLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2: Add imageUri column
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE saved_snap_location ADD COLUMN imageUri TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lasertrac_database"
                )
                .addMigrations(MIGRATION_1_2) // Add the migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
