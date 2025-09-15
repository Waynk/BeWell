package com.example.jenmix.jen8.model

import java.util.*

/**
 * 本地圖表資料模型，專供圖表用（含身高、性別、日期）
 */
data class WeightRecordLocal(
    val date: Date,       // 測量日期（已轉型）
    val weight: Float,    // 體重
    val height: Float,    // 身高（計算正常體重用）
    val gender: String,   // 性別（可未來擴充用）
    val age: Int          // 年齡（可未來擴充用）
)
