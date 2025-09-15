package com.example.jenmix.jen8

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.example.jenmix.R
import com.example.jenmix.jen8.model.WeightRecordLocal
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(context: Context) : MarkerView(context, R.layout.marker_view) {

    private val tvDate: TextView = findViewById(R.id.tvMarkerDate)
    private val tvTime: TextView = findViewById(R.id.tvMarkerTime)
    private val tvWeight: TextView = findViewById(R.id.tvMarkerWeight)
    private val tvStatus: TextView = findViewById(R.id.tvMarkerStatus)
    private val ivStatus: ImageView = findViewById(R.id.iconMarkerStatus) // 新增狀態圖示

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.TAIWAN)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            val record = e.data as? WeightRecordLocal
            if (record != null) {
                val dateStr = dateFormat.format(record.date)
                val timeStr = timeFormat.format(record.date)
                val weight = record.weight
                val statusText = getWeightStatusText(record)
                val statusIcon = getWeightStatusIcon(record)

                tvDate.text = "📅 日期：$dateStr"
                tvTime.text = "🕒 時間：$timeStr"
                tvWeight.text = "⚖️ 體重：%.1f kg".format(weight)

                // ✅ 狀態改為「圖示 + 狀態文字」
                tvStatus.text = statusText
                ivStatus.setImageResource(statusIcon)
            } else {
                Log.w("CustomMarkerView", "⚠️ 資料轉換錯誤：非 WeightRecordLocal")
            }
        } else {
            Log.w("CustomMarkerView", "⚠️ Entry 為 null，無法顯示資料")
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }

    private fun getWeightStatusText(record: WeightRecordLocal): String {
        val heightM = record.height / 100.0
        val minNormal = 18.5 * heightM * heightM
        val maxNormal = 24.0 * heightM * heightM

        return when {
            record.weight < minNormal * 0.9 -> "過輕"
            record.weight in (minNormal * 0.9)..minNormal -> "偏低"
            record.weight in minNormal..maxNormal -> "正常"
            record.weight in maxNormal..(maxNormal * 1.1) -> "偏高"
            else -> "過重"
        }
    }

    private fun getWeightStatusIcon(record: WeightRecordLocal): Int {
        val heightM = record.height / 100.0
        val minNormal = 18.5 * heightM * heightM
        val maxNormal = 24.0 * heightM * heightM

        return when {
            record.weight < minNormal * 0.9 -> R.drawable.ic_bmi_thin         // 過輕 ➜ 藍色瘦瘦人
            record.weight in (minNormal * 0.9)..minNormal -> R.drawable.ic_bmi_warning // 偏低 ➜ 黃色警告人
            record.weight in minNormal..maxNormal -> R.drawable.ic_bmi_normal // 正常 ➜ 綠色正常人
            record.weight in maxNormal..(maxNormal * 1.1) -> R.drawable.ic_bmi_warning // 偏高 ➜ 黃色警告人
            else -> R.drawable.ic_bmi_danger                                   // 過重 ➜ 紅色胖胖人
        }
    }
}
