package com.example.jenmix.jen9

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val title: String,
    val location: String,
    val department: String,
    val note: String,
    val timeInMillis: Long
)