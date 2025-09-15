package com.example.jenmix.jen2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_reminders")
data class MedicationReminderEntity(
    @PrimaryKey val requestCode: Int, // 使用 uniqueRequestCode 當 PrimaryKey
    val name: String,
    val type: String,
    val dosage: String,
    val ingredients: String,
    val contraindications: String,
    val sideEffects: String,
    val sourceUrl: String,
    val reminderTime: String,
    val username: String // ✅ 加這個欄位
)