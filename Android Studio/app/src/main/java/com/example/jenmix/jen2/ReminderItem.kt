package com.example.jenmix.jen2

data class ReminderItem(
    val med: Medication,
    val reminderTime: String = "-",
    val requestCode: Int = 0
)
