package com.example.jenmix.jen8

import android.graphics.Color
import com.example.jenmix.jen8.model.WeightRecordLocal
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

object WeightPieChartHelper {

    // ✅ 顏色對應（外圈色）
    private val zoneColors = mapOf(
        "過輕" to Color.parseColor("#2196F3"),   // 藍
        "正常" to Color.parseColor("#4CAF50"),   // 綠
        "偏高/偏低" to Color.parseColor("#FFC107"),   // 黃
        "過重" to Color.parseColor("#F44336")    // 紅
    )

    /**
     * 設定圓餅圖並回傳 Map<String, Pair<次數, 百分比>>
     */
    fun setupPieChart(
        chart: PieChart,
        records: List<WeightRecordLocal>
    ): Map<String, Pair<Int, Float>> {
        if (records.isEmpty()) return emptyMap()

        // 統計次數初始化
        val zoneCount = mutableMapOf(
            "過輕" to 0,
            "正常" to 0,
            "偏高/偏低" to 0,
            "過重" to 0
        )

        // 📊 分類統計 BMI 狀態
        for (record in records) {
            val heightM = record.height / 100.0
            val min = 18.5 * heightM * heightM
            val max = 24.0 * heightM * heightM
            val weight = record.weight.toDouble()

            when {
                weight < min -> zoneCount["過輕"] = zoneCount["過輕"]!! + 1
                weight <= max -> zoneCount["正常"] = zoneCount["正常"]!! + 1
                weight <= max * 1.1 -> zoneCount["偏高/偏低"] = zoneCount["偏高/偏低"]!! + 1
                else -> zoneCount["過重"] = zoneCount["過重"]!! + 1
            }
        }

        val total = zoneCount.values.sum().coerceAtLeast(1) // 防除以 0

        // PieEntry 資料組裝
        val entries = zoneCount
            .filter { it.value > 0 }
            .map { (label, count) ->
                PieEntry(count.toFloat() / total, label)
            }

        // 🎨 設定資料集樣式
        val dataSet = PieDataSet(entries, "").apply {
            colors = entries.map { zoneColors[it.label] ?: Color.LTGRAY }
            valueTextSize = 14f
            valueTextColor = Color.DKGRAY
            sliceSpace = 2f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.35f
            valueLinePart2Length = 0.5f
            valueLineColor = Color.DKGRAY
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(chart))
        }

        // 🥧 設定 PieChart 本體樣式
        chart.apply {
            data = pieData
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(20f, 0f, 20f, 20f)
            isDrawHoleEnabled = true
            holeRadius = 32f
            transparentCircleRadius = 36f
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.DKGRAY)
            setEntryLabelTextSize(14f)
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }

        // 回傳統計資料 Map
        val percentMap = mutableMapOf<String, Pair<Int, Float>>()
        zoneCount.forEach { (label, count) ->
            val percent = (count.toFloat() / total) * 100f
            percentMap[label] = Pair(count, percent)
        }

        return percentMap
    }
}
