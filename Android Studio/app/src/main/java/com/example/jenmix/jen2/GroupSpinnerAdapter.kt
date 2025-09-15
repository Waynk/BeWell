package com.example.jenmix.jen2

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.jenmix.R
class GroupSpinnerAdapter(
    context: Context,
    private val items: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

    // 快取顏色資源，避免重複查詢
    private val titleTextColor = context.getColor(R.color.purple_700)
    private val titleBackgroundColor = context.getColor(R.color.lightGray)
    private val normalTextColor = context.getColor(R.color.black)
    private val transparentColor = context.getColor(android.R.color.transparent)

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun isEnabled(position: Int): Boolean {
        // 如果項目是分類標題 (以 "【" 開頭)，則禁用選擇
        return getItem(position)?.startsWith("【")?.not() ?: true
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        val item = getItem(position) ?: ""

        if (item.startsWith("【")) {
            // 分類標題樣式
            view.setTextColor(titleTextColor)
            view.setTypeface(null, Typeface.BOLD)
            view.setPadding(32, 24, 16, 8)
            view.textSize = 16f
            view.setBackgroundColor(titleBackgroundColor)
        } else {
            // 藥品名稱樣式
            view.setTextColor(normalTextColor)
            view.setTypeface(null, Typeface.NORMAL)
            view.setPadding(64, 16, 16, 16)
            view.textSize = 14f
            view.setBackgroundColor(transparentColor)
        }

        return view
    }
}
