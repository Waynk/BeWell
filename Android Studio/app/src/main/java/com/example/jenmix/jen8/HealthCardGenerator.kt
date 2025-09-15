package com.example.jenmix.jen8

import java.text.SimpleDateFormat
import java.util.*

object HealthCardGenerator {

    fun generateCards(
        gender: String,
        age: Int,
        height: Float,
        weight: Float,
        impedance: Int,
        bmrFromDevice: Int
    ): List<HealthItem> {
        val bmi = HealthAnalyzer.calculateBMI(weight, height)
        val fat = HealthAnalyzer.estimateFatPercentage(gender, age, bmi)
        val fatIndex = HealthAnalyzer.estimateFatIndex(fat)
        val obesityLevel = HealthAnalyzer.estimateObesityLevel(bmi)
        val (weightMin, weightMax) = HealthAnalyzer.estimateNormalWeightRange(height)

        val now = SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date())  // ✅ 測量時間戳

        // ✅ 統一鼓勵語
        val encouragement = "今天表現不錯，繼續維持健康生活吧！"

        return listOf(
            HealthItem(
                title = "BMI",
                value = "%.1f".format(bmi),
                status = getBMIStatus(bmi),
                suggestion = getBMISuggestion(bmi),
                measuredTime = now,
                encouragement = encouragement
            ),
            HealthItem(
                title = "體脂指數",
                value = "$fatIndex",
                status = getFatIndexStatus(fatIndex),
                suggestion = getFatIndexSuggestion(fatIndex),
                measuredTime = now,
                encouragement = encouragement
            ),
            HealthItem(
                title = "肥胖級別",
                value = "$obesityLevel",
                status = getObesityStatus(obesityLevel),
                suggestion = getObesitySuggestion(obesityLevel),
                measuredTime = now,
                encouragement = encouragement
            ),
            HealthItem(
                title = "正常體重",
                value = "%.1f~%.1f 公斤".format(weightMin, weightMax),
                status = getNormalWeightStatus(weight, weightMin, weightMax),
                suggestion = getNormalWeightSuggestion(weight, weightMin, weightMax),
                measuredTime = now,
                encouragement = encouragement
            )
        )
    }

    // ✅ BMI 狀態分類
    private fun getBMIStatus(bmi: Float) = when {
        bmi < 18.5f -> "偏瘦"       // 🔵
        bmi < 24f -> "正常"        // 🟢
        bmi < 28f -> "偏胖"        // 🟠
        bmi < 35f -> "肥胖"        // 🔴
        else -> "極胖"            // 🔴
    }

    // ✅ 體脂指數狀態分類
    private fun getFatIndexStatus(index: Int) = when (index) {
        in 0..2 -> "過低"          // 🔵
        3 -> "偏低"               // 🔵
        4 -> "良好"               // 🟢
        5 -> "偏高"               // 🟠
        6 -> "過高"               // 🔴
        else -> "極高"            // 🔴
    }

    // ✅ 肥胖級別狀態分類
    private fun getObesityStatus(level: Int) = when (level) {
        0 -> "偏瘦"               // 🔵
        1 -> "不肥胖"             // 🟢
        2 -> "微胖"               // 🟠
        3 -> "偏胖"               // 🟠
        else -> "肥胖"            // 🔴
    }

    // ✅ 正常體重範圍分類
    private fun getNormalWeightStatus(weight: Float, min: Float, max: Float) = when {
        weight < min -> "偏輕"     // 🔵
        weight <= max -> "正常"    // 🟢
        else -> "偏重"             // 🟠
    }

    // ✅ 建議語句 ------------------

    private fun getBMISuggestion(bmi: Float) = when {
        bmi < 18.5f -> "體重過輕，建議增加營養攝取與阻力訓練。"
        bmi < 24f -> "目前 BMI 屬於正常範圍，請持續維持良好習慣。"
        bmi < 28f -> "有些偏胖，建議規律運動與控制總熱量攝取。"
        bmi < 35f -> "已達肥胖等級，應積極改善生活作息與飲食。"
        else -> "屬於重度肥胖，建議尋求醫療與營養師協助控制體重。"
    }

    private fun getFatIndexSuggestion(index: Int) = when (index) {
        in 0..2 -> "體脂過低可能影響健康，建議增加脂肪攝取與訓練。"
        3 -> "體脂略低，請注意是否有營養不均等問題。"
        4 -> "體脂正常，建議維持目前的健康生活型態。"
        5 -> "體脂偏高，建議增加運動與控制油脂攝取。"
        6 -> "體脂過高，請注意心血管健康並減少高熱量飲食。"
        else -> "體脂過高，應積極改善生活習慣與營養狀況。"
    }

    private fun getObesitySuggestion(level: Int) = when (level) {
        0 -> "偏瘦體型，建議增加熱量攝取與增肌訓練。"
        1 -> "屬不肥胖體型，請繼續維持現狀與健康習慣。"
        2 -> "微胖狀態，建議稍微減少熱量攝取並增加活動量。"
        3 -> "偏胖狀態，請配合飲食與運動調整體脂比例。"
        else -> "屬肥胖體型，建議儘速採取減重與健康改善計畫。"
    }

    private fun getNormalWeightSuggestion(weight: Float, min: Float, max: Float) = when {
        weight < min -> "體重偏輕，建議增加攝取量與改善肌肉量。"
        weight <= max -> "體重在正常範圍，請繼續保持良好的生活作息。"
        else -> "體重略重，建議控制澱粉、糖分與脂肪攝取。"
    }
}
