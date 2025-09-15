package com.example.jenmix.jen4

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GeneralReminderEntity::class],
    version = 2,
    exportSchema = false // ✅ 加上這行，避免警告
)

abstract class AppDatabase4 : RoomDatabase() {
    abstract fun generalReminderDao(): GeneralReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase4? = null

        fun getDatabase(context: Context): AppDatabase4 {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase4::class.java,
                    "general_reminder_db"
                )
                .fallbackToDestructiveMigration() // 💥 加這行，允許資料庫 schema 改變直接重建
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}