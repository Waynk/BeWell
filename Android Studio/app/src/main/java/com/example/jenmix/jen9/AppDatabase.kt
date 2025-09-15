package com.example.jenmix.jen9

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReminderEntity::class],
    version = 2,
    exportSchema = false // ✅ 加上這行，避免警告
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reminder_database"
                )
                .fallbackToDestructiveMigration() // 💥 加這行，允許資料庫 schema 改變直接重建
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}