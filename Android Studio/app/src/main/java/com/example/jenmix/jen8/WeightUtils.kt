package com.example.jenmix.jen8

object WeightUtils {

    /**
     * 根據身高計算正常體重區間（使用 BMI 18.5～24 範圍）
     * @param heightCm 身高（公分）
     * @return Pair<下限體重, 上限體重>
     */
    fun getNormalRange(heightCm: Float): Pair<Float, Float> {
        val heightM = heightCm / 100
        val min = 18.5f * heightM * heightM
        val max = 24f * heightM * heightM
        return Pair(min, max)
    }

    /**
     * 根據身高與體重計算體重狀態分類
     * @return 狀態文字：過輕、正常、偏高、過重
     */
    fun getWeightStatus(weight: Float, heightCm: Float): String {
        val (min, max) = getNormalRange(heightCm)
        return when {
            weight < min -> "過輕 ⚠️"
            weight > max + 3 -> "過重 ❗"
            weight > max -> "偏高 🟠"
            weight in min..max -> "正常 ✅"
            weight in (min - 3)..min -> "偏低 🟠"
            else -> "其他 ⚪"
        }
    }
}
