package com.example.jenmix.api

data class ReminderDto(
    val reminder_id: Int,
    val hour: Int,
    val minute: Int,
    val category: String,
    val title: String,
    val content: String,
    val dayOfWeek: Int?,    // 如果 API 回 null 就對應到 Kotlin 的 null
    val isRepeat: Boolean
)
