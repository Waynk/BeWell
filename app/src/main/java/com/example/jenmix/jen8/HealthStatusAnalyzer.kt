package com.example.jenmix.jen8

object HealthStatusAnalyzer {

    fun getBMIStatus(bmi: Float): String {
        return when {
            bmi < 18.5 -> "偏瘦"
            bmi < 24 -> "正常"
            bmi < 28 -> "偏胖"
            bmi < 35 -> "肥胖"
            else -> "極胖"
        }
    }

    fun getFatStatus(fat: Float): String {
        return when {
            fat < 10 -> "偏低"
            fat < 21 -> "正常"
            fat < 26 -> "偏胖"
            else -> "肥胖"
        }
    }

    fun getMuscleStatus(muscle: Float): String {
        return if (muscle < 51.0) "偏低" else "標準"
    }

    fun getWaterStatus(water: Float): String {
        return when {
            water < 55 -> "偏低"
            water < 65 -> "標準"
            else -> "偏高"
        }
    }

    fun getProteinStatus(protein: Float): String {
        return when {
            protein < 16.0 -> "偏低"
            protein < 20.0 -> "正常"
            else -> "偏高"
        }
    }

    fun getVisceralFatStatus(visceral: Int): String {
        return when {
            visceral < 10 -> "正常"
            visceral < 15 -> "偏高"
            else -> "超高"
        }
    }

    fun getFatMassStatus(fatMass: Float): String {
        return when {
            fatMass < 7.1f -> "偏低"
            fatMass < 15.0f -> "正常"
            fatMass < 18.5f -> "偏胖"
            else -> "肥胖"
        }
    }

    fun getBoneMassStatus(bonePercent: Float): String {
        return if (bonePercent in 3.0..5.0) "正常" else "異常"
    }

    fun getBMRStatus(bmr: Int): String {
        return if (bmr >= 1600) "達標" else "不足"
    }

    fun getBodyAgeStatus(bodyAge: Int, realAge: Int): String {
        return if (bodyAge <= realAge) "年輕" else "偏老"
    }

    fun getBodyTypeStatus(type: String): String {
        return type // 已為中文標籤如：標準型、隱性肥胖型等
    }

    fun getFatIndexStatus(index: Int): String {
        return when (index) {
            in 0..2 -> "偏低"
            3 -> "正常"
            4 -> "良好"
            5 -> "偏高"
            6 -> "過高"
            else -> "極高"
        }
    }

    fun getObesityLevelStatus(level: Int): String {
        return when (level) {
            0 -> "不肥胖"
            1 -> "不肥胖"
            2 -> "微胖"
            3 -> "偏胖"
            else -> "肥胖"
        }
    }

    fun getIdealWeightStatus(weight: Float, idealMin: Float, idealMax: Float): String {
        return when {
            weight < idealMin -> "偏輕"
            weight <= idealMax -> "正常"
            else -> "偏重"
        }
    }

    fun getLeanBodyMassStatus(lbMass: Float): String {
        return if (lbMass > 0) "正常" else "異常"
    }
}
