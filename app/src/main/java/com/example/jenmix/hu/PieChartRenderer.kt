package com.example.jenmix.hu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

class CustomPieChartRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler,
    private val highlightedEntry: PieEntry?
) : PieChartRenderer(chart, animator, viewPortHandler) {

    // 自己宣告一支 Paint 來畫標籤文字
    private val entryLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 20f
        isFakeBoldText = true
    }

    override fun drawValues(c: Canvas) {
        val dataSets = mChart.data.dataSets
        val radius = mChart.radius
        val rotationAngle = mChart.rotationAngle
        val phaseY = mAnimator.phaseY
        val center = mChart.centerCircleBox
        val drawAngles = mChart.drawAngles
        val absoluteAngles = mChart.absoluteAngles

        val labelRadiusOffset = radius / 1.5f

        val total = dataSets.sumOf { dataSet ->
            (0 until dataSet.entryCount).sumOf { dataSet.getEntryForIndex(it).y.toDouble() }
        }.toFloat()

        for ((dataSetIndex, dataSet) in dataSets.withIndex()) {
            for (entryIndex in 0 until dataSet.entryCount) {
                val entry = dataSet.getEntryForIndex(entryIndex) as? PieEntry ?: continue
                if (entry.value == 0f) continue

                val angle = (if (entryIndex == 0) 0f else absoluteAngles[entryIndex - 1]) +
                        drawAngles[entryIndex] / 2f
                val transformedAngle = rotationAngle + angle * phaseY

                val pos = Utils.getPosition(center, labelRadiusOffset, transformedAngle)

                // 如果是被選中的就放大字體
                entryLabelPaint.textSize = if (entry == highlightedEntry) 80f else 50f
                entryLabelPaint.color = Color.WHITE

                // 顯示百分比文字
                val percent = (entry.value / total) * 100
                val percentText = String.format("%.1f%%", percent)

                c.drawText(percentText, pos.x, pos.y, entryLabelPaint)
            }
        }
    }
}