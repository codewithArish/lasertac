package com.lasertrac.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lasertrac.app.db.PoliceStation // Explicit import for PoliceStation
import com.lasertrac.app.db.SavedSnapLocationEntity // Explicit import for SavedSnapLocationEntity

@Database(entities = [PoliceStation::class, SavedSnapLocationEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun policeStationDao(): PoliceStationDao
    abstract fun snapLocationDao(): SnapLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lasertrac_app_database"
                )
                // .createFromAsset("databases/delhi_ncr_police_stations.db") // Asset loading commented out for now
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
