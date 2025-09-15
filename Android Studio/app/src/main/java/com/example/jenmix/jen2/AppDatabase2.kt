package com.example.jenmix.jen2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MedicationReminderEntity::class],
    version = 2,
    exportSchema = false // ✅ 加上這行，避免警告
)

abstract class AppDatabase2 : RoomDatabase() {
    abstract fun reminderDao(): MedicationReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase2? = null

        fun getDatabase(context: Context): AppDatabase2 {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase2::class.java,
                    "medication_reminder_db"
                )
                .fallbackToDestructiveMigration() // 💥 加這行，允許資料庫 schema 改變直接重建
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}