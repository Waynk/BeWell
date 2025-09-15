package com.example.jenmix.jen2

import android.content.Context
import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.HtmlCompat
import com.example.jenmix.R

class ReminderAdapter(
    private val context: Context,
    private var items: MutableList<ReminderItem>,
    private val onDeleteClick: (ReminderItem) -> Unit
) : BaseAdapter() {

    // 更新資料來源並通知 ListView 重繪
    fun setItems(newItems: List<ReminderItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): ReminderItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false)
        val tvInfo = view.findViewById<TextView>(R.id.tvInfo)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val btnSource = view.findViewById<Button>(R.id.btnSource)

        val item = items[position]
        val med = item.med

        // 依據 reminderTime 是否存在，決定是否為提醒項目
        val infoHtml = if (item.reminderTime.isNullOrEmpty()) {
            // 無提醒時間 → 藥物清單項目，僅顯示藥物資料
            """
            <b>💊 類別:</b> ${med.type}<br>
            <b>📛 名稱:</b> ${med.name}<br>
            <b>💉 劑量:</b> ${med.dosage}<br>
            <b>🧪 成分:</b> ${med.ingredients}<br>
            <b>🚫 禁忌:</b> ${med.contraindications}<br>
            <b>⚠️ 副作用:</b> ${med.side_effects}<br>
            """.trimIndent()
        } else {
            // 有提醒時間 → 提醒項目，除藥物資料外還顯示提醒時間
            """
            <b>💊 類別:</b> ${med.type}<br>
            <b>📛 名稱:</b> ${med.name}<br>
            <b>💉 劑量:</b> ${med.dosage}<br>
            <b>🧪 成分:</b> ${med.ingredients}<br>
            <b>🚫 禁忌:</b> ${med.contraindications}<br>
            <b>⚠️ 副作用:</b> ${med.side_effects}<br>
            <hr><br>
            <i>⏰ 提醒時間：</i> ${item.reminderTime}
            """.trimIndent()
        }

        tvInfo.text = HtmlCompat.fromHtml(infoHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        tvInfo.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        // 刪除按鈕點擊事件
        btnDelete.setOnClickListener {
            onDeleteClick(item)
        }

        // 資料來源按鈕點擊事件：使用 WebViewActivity 開啟來源網址
        btnSource.setOnClickListener {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra("url", med.source_url)
            }
            context.startActivity(intent)
        }

        return view
    }
}
