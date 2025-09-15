package com.example.jenmix.jen4

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "general_reminders")
data class GeneralReminderEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val hour: Int,
    val minute: Int,
    val category: String,
    val dayOfWeek: Int?, // null 表示每天
    val title: String,
    val content: String,
    val isRepeat: Boolean
)