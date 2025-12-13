package com.lasertrac.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, SavedSnapLocationEntity::class, Violation::class, SnapDetail::class], version = 6, exportSchema = false)
@TypeConverters(SnapStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun snapLocationDao(): SnapLocationDao
    abstract fun violationDao(): ViolationDao
    abstract fun snapDao(): SnapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lasertrac_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(CoroutineScope(Dispatchers.IO)))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let {
                    scope.launch {
                        val violationDao = it.violationDao()
                        violationDao.insertAll(Violation.predefinedViolations)
                    }
                }
            }
        }
    }
}
