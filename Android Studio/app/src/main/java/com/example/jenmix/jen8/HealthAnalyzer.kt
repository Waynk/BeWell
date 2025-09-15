package com.example.jenmix.jen8

object HealthAnalyzer {

    fun calculateBMI(weight: Float, height: Float): Float {
        return weight / ((height / 100) * (height / 100))
    }

    fun estimateFatPercentage(gender: String, age: Int, bmi: Float): Float {
        return (1.20f * bmi + 0.23f * age - 16.2f) // 標準體脂公式
    }

    fun estimateFatMass(weight: Float, fatPercentage: Float): Float {
        return weight * fatPercentage / 100f
    }

    fun estimateLeanBodyMass(weight: Float, fatMass: Float): Float {
        return weight - fatMass
    }

    fun estimateWaterPercentage(gender: String, fatPercentage: Float): Float {
        return when (gender.lowercase()) {
            "male", "男" -> 73f - fatPercentage * 0.75f
            "female", "女" -> 70f - fatPercentage * 0.6f
            else -> 70f - fatPercentage * 0.65f
        }
    }

    fun estimateProteinPercentage(fatPercentage: Float): Float {
        return 20f - (fatPercentage * 0.15f)
    }

    fun estimateMusclePercentage(gender: String, water: Float, protein: Float, bone: Float): Float {
        val base = water + protein + bone
        return if (base > 100f) 100f - estimateFatPercentage(gender, 21, 23.0f) else base
    }

    fun estimateBoneMass(gender: String, weight: Float): Float {
        return when (gender.lowercase()) {
            "male", "男" -> weight * 0.043f // 約 4.3% of 體重
            "female", "女" -> weight * 0.035f
            else -> weight * 0.04f
        }
    }

    fun estimateBMRRaw(weight: Float, height: Float, age: Int, gender: String): Int {
        return when (gender.lowercase()) {
            "male", "男" -> (66.47f + 13.75f * weight + 5.003f * height - 6.755f * age).toInt()
            "female", "女" -> (655.1f + 9.563f * weight + 1.85f * height - 4.676f * age).toInt()
            else -> 1500
        }
    }

    fun estimateVisceralFatLevel(fatPercentage: Float): Int {
        return when {
            fatPercentage < 10f -> 2
            fatPercentage < 15f -> 5
            fatPercentage < 20f -> 7
            else -> 9
        }
    }

    fun estimateBodyAge(age: Int, bmr: Int, avgBmr: Int): Int {
        return if (bmr >= avgBmr) age - 1 else age + 1
    }

    fun estimateBodyType(fatPercentage: Float, musclePercentage: Float): String {
        return when {
            fatPercentage > 25 && musclePercentage < 45 -> "隱性肥胖"
            fatPercentage > 25 -> "肥胖型"
            fatPercentage < 15 && musclePercentage > 55 -> "運動健美型"
            fatPercentage < 15 -> "偏瘦型"
            musclePercentage > 55 -> "標準運動型"
            else -> "標準型"
        }
    }

    fun estimateFatIndex(fatPercentage: Float): Int {
        return when {
            fatPercentage < 10 -> 2
            fatPercentage < 15 -> 3
            fatPercentage < 18 -> 4
            fatPercentage < 21 -> 5
            fatPercentage < 25 -> 6
            else -> 7
        }
    }

    fun estimateObesityLevel(bmi: Float): Int {
        return when {
            bmi < 18.5f -> 0
            bmi < 24f -> 1
            bmi < 27f -> 2
            bmi < 30f -> 3
            else -> 4
        }
    }

    fun estimateNormalWeightRange(height: Float): Pair<Float, Float> {
        val min = 18.5f * (height / 100f).pow(2)
        val max = 24f * (height / 100f).pow(2)
        return Pair(min, max)
    }

    private fun Float.pow(exp: Int): Float = Math.pow(this.toDouble(), exp.toDouble()).toFloat()
}
