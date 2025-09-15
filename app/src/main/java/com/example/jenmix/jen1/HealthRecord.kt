package com.example.jenmix.jen1

data class HealthRecord(
    val display_name: String,
    val age: Int,
    val gender: String,
    val measure_at: String?, // 血壓時間
    val systolic_mmHg: Int?,
    val diastolic_mmHg: Int?,
    val pulse_bpm: Int?,
    val weight: Float?,
    val weight_height: Float?,
    val weight_measured_at: String?,
    val diseaseType: String? = null,      // 可選，給 URL 查詢用
){
    val measuredAt: String
        get() = measure_at ?: weight_measured_at ?: "未知"
}

