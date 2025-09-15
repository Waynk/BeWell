package com.example.jenmix.hu

import android.content.Context
import android.widget.TextView
import com.example.jenmix.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class DateMarkerView(
    context: Context,
    layoutResource: Int,
    private val dateList: List<String>
) : MarkerView(context, layoutResource) {


    private val tvDate: TextView = findViewById(R.id.tvMarkerDate)


    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val index = e?.x?.toInt() ?: 0
        if (index in dateList.indices) {
            tvDate.text = dateList[index]
        } else {
            tvDate.text = "無資料"
        }
        super.refreshContent(e, highlight)
    }


    override fun getOffset(): MPPointF {
        // 水平置中，垂直在上方
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}