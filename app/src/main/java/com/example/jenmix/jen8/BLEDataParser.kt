package com.example.jenmix.jen8

data class BLEParsedResult(
    val weight: Float,            // 體重 (公斤)
    val bmr: Int,                 // 基礎代謝率 (修正後 kcal)
    val impedance: Int,           // 生物電阻抗值
    val rawData: String,
    val timestampMillis: Long = System.currentTimeMillis(), // ✅ 加入時間戳
    val dataHash: Int = "$weight-$impedance".hashCode()     // ✅ 判斷資料唯一用
)

object BLEDataParser {

    fun parse(data: ByteArray): BLEParsedResult {
        val hexString = data.joinToString("-") { "%02X".format(it) }

        // 體重：data[13], data[14] => Big Endian
        val weightRaw = ((data[13].toInt() and 0xFF) shl 8) + (data[14].toInt() and 0xFF)
        val weightKg = weightRaw / 100f  // ✅ 實測應為除以 100 才為公斤

        // BMR：data[16], data[17] => Big Endian 並乘上 0.76 修正為實際值
        val bmrRaw = ((data[16].toInt() and 0xFF) shl 8) + (data[17].toInt() and 0xFF)
        val bmr = (bmrRaw * 0.76).toInt()

        // 電阻抗值（Impedance）：data[10], data[11] => Big Endian
        val impedance = ((data[11].toInt() and 0xFF) shl 8) + (data[10].toInt() and 0xFF)

        return BLEParsedResult(
            weight = weightKg,
            bmr = bmr,
            impedance = impedance,
            rawData = hexString
        )
    }

    // 可擴充項目（未來逐項加入）
    // fun parseFatPercentage(data: ByteArray): Float { ... }
    // fun parseWaterPercentage(data: ByteArray): Float { ... }
}
