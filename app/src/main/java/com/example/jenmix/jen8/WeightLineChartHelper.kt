package com.example.jenmix.jen8

import android.graphics.Color
import com.example.jenmix.jen8.model.WeightRecordLocal
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object WeightLineChartHelper {

    fun setupLineChart(
        chart: LineChart,
        records: List<WeightRecordLocal>,
        onSuggestionGenerated: ((String) -> Unit)? = null
    ) {
        if (records.isEmpty()) return

        val sdfLabel = SimpleDateFormat("MM/dd", Locale.getDefault())
        val sorted = records.sortedBy { it.date }
        val labels = sorted.map { sdfLabel.format(it.date) }

        val heightM = records.firstOrNull()?.height?.div(100.0) ?: 1.7
        val minNormal = 18.5 * heightM * heightM
        val maxNormal = 24.0 * heightM * heightM
        val abnormalThreshold = 3.0f

        val entries = sorted.mapIndexed { index, record ->
            Entry(index.toFloat(), record.weight).apply { data = record }
        }

        // ✅ 計算圓點顏色
        val pointColors = entries.map {
            val record = it.data as? WeightRecordLocal
            val w = record?.weight ?: 0f
            when {
                w < minNormal * 0.9 -> Color.parseColor("#2196F3")   // 過輕
                w <= minNormal -> Color.parseColor("#FFC107")        // 偏低
                w <= maxNormal -> Color.parseColor("#4CAF50")        // 正常
                w <= maxNormal * 1.1 -> Color.parseColor("#FFC107")  // 偏高
                else -> Color.parseColor("#F44336")                  // 過重
            }
        }

        // ✅ 主資料點透明線（放最前面）
        val pointSet = LineDataSet(entries, "體重點資料").apply {
            setDrawCircles(true)
            setDrawValues(false)
            setCircleColors(pointColors)
            circleRadius = 5f
            circleHoleRadius = 2.5f
            color = Color.TRANSPARENT
            setDrawFilled(true)
            fillColor = Color.parseColor("#BBDEFB")
            mode = LineDataSet.Mode.LINEAR
            lineWidth = 0f
            axisDependency = YAxis.AxisDependency.LEFT
            isHighlightEnabled = true // ✅ 必須開啟
        }

        // ✅ 趨勢線段資料集
        val trendDataSets = mutableListOf<ILineDataSet>()
        for (i in 1 until entries.size) {
            val prev = entries[i - 1]
            val curr = entries[i]
            val diff = curr.y - prev.y

            val trendColor = when {
                kotlin.math.abs(diff) > abnormalThreshold -> Color.RED
                diff > 0 -> Color.parseColor("#388E3C") // 體重上升
                diff < 0 -> Color.parseColor("#1976D2") // 體重下降
                else -> Color.GRAY
            }

            val segmentEntries = listOf(
                Entry(prev.x, prev.y).apply { data = prev.data },
                Entry(curr.x, curr.y).apply { data = curr.data }
            )

            val lineSet = LineDataSet(segmentEntries, "").apply {
                color = trendColor
                lineWidth = 2.5f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                setDrawCircles(false)
                setDrawFilled(false)
                isHighlightEnabled = false // ✅ 不允許被點擊
            }

            trendDataSets.add(lineSet)
        }

        // ✅ 資料點放最前面（第 0 筆）
        trendDataSets.add(0, pointSet)

        chart.apply {
            data = LineData(trendDataSets)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false

            // ✅ Y 軸固定 0~150kg
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 150f
                granularity = 0.1f
                textColor = Color.DKGRAY
                textSize = 12f
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                valueFormatter = object : IndexAxisValueFormatter() {
                    private val df = DecimalFormat("#.0")
                    override fun getFormattedValue(value: Float): String {
                        return df.format(value.toDouble()) + " kg"
                    }
                }
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelRotationAngle = -30f
                textColor = Color.DKGRAY
                textSize = 12f
                setDrawGridLines(false)
            }

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val record = e?.data as? WeightRecordLocal ?: return
                    val suggestion = generateSuggestion(record.weight.toDouble(), minNormal, maxNormal)
                    onSuggestionGenerated?.invoke(suggestion)
                }

                override fun onNothingSelected() {}
            })

            animateX(1000)
            invalidate()
        }
    }

    private fun generateSuggestion(weight: Double, min: Double, max: Double): String {
        return when {
            weight < min * 0.9 -> "🔵 體重過輕，請多補充營養"
            weight in (min * 0.9)..min -> "🟡 體重偏低，建議注意健康"
            weight in min..max -> "🟢 體重正常，請繼續保持"
            weight in max..(max * 1.1) -> "🟡 體重偏高，建議適度運動"
            else -> "🔴 體重過重，需控制飲食"
        }
    }
}
