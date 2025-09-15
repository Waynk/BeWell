package com.example.jenmix.hu

import android.content.Context
import android.util.Log
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.example.jenmix.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(
    context: Context,
    layoutResource: Int,
    private val dataList: List<BloodPressureData2> // 接收 dataList
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tvDate)  // 获取日期显示控件

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)

        // 获取索引，确保不超出 dataList 范围
        val index = e?.x?.toInt() ?: return
        if (index !in dataList.indices) {
            tvDate.text = "Invalid Date"
            return
        }

        val date = dataList[index].date  // 读取测量日期
        Log.d("CustomMarkerView", "Original Date: $date")

        // 定义目标格式
        val targetDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        // 尝试解析原始日期
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")  // 强制解析为 UTC 时间
        }

        try {
            val parsedDate = inputDateFormat.parse(date)
            if (parsedDate != null) {
                // 转换为本地时间显示
                targetDateFormat.timeZone = TimeZone.getDefault()
                tvDate.text = targetDateFormat.format(parsedDate)
            } else {
                tvDate.text = "Invalid Date"
            }
        } catch (e: Exception) {
            Log.e("CustomMarkerView", "Error parsing date: ${e.message}")
            tvDate.text = "Invalid Date"
        }
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val offset = super.getOffsetForDrawingAtPoint(posX, posY)

        // 调整标记位置，使其水平居中
        offset.x = -(width / 2f)
        // 调整标记位置，使其位于点的上方 (将height转换为Float)
        offset.y = -(height.toFloat())

        return offset
    }
}