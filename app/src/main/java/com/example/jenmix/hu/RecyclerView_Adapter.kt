package com.example.jenmix.hu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BloodPressureDeleteAdapter(
    private val dataList: List<BloodPressure>,
    private val onCheckedChange: (BloodPressure, Boolean) -> Unit
) : RecyclerView.Adapter<BloodPressureDeleteAdapter.ViewHolder>() {

    // 儲存每個 item 的勾選狀態
    private val checkedMap = mutableMapOf<Int, Boolean>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSysDia: TextView = view.findViewById(R.id.tvSysDia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blood_pressure_checkbox, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]

        // 解析日期並轉成指定格式
        holder.tvDate.text = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("Asia/Taipei")
            val date = inputFormat.parse(item.measure_at)
            val outputFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: item.measure_at
        } catch (e: Exception) {
            item.measure_at
        }

        // 顯示血壓
        holder.tvSysDia.text = "${item.systolic} / ${item.diastolic}"

        // 初始狀態取消勾選監聽，避免重複觸發
        holder.checkBox.setOnCheckedChangeListener(null)

        // 還原勾選狀態
        val isChecked = checkedMap[item.id] ?: false
        holder.checkBox.isChecked = isChecked
        holder.itemView.alpha = if (isChecked) 0.5f else 1.0f

        // 勾選監聽
        holder.checkBox.setOnCheckedChangeListener { _, checked ->
            checkedMap[item.id] = checked
            onCheckedChange(item, checked)
            holder.itemView.alpha = if (checked) 0.5f else 1.0f
        }
    }

    override fun getItemCount() = dataList.size
}
