package com.example.jenmix.hu

data class BloodPressure(
    val id: Int,
    val measure_at: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?
)