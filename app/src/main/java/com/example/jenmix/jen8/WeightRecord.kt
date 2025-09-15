package com.example.jenmix.jen8

import com.google.gson.annotations.SerializedName
import java.util.*

data class WeightRecord(
    val id: Int,
    val username: String,
    val gender: String,
    val height: Float,
    val age: Int,
    val weight: Float, // 體重

    val bmi: Float? = null, // ✅ 可為 null，避免未回傳閃退

    @SerializedName("measured_at")
    val measuredAt: String, // 原始時間字串 yyyy-MM-dd HH:mm:ss

    // ✅ 轉為 Date 類型，用來排序與格式化
    var parsedDate: Date? = null
)
