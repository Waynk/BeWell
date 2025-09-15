package com.example.jenmix.jen8

import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R
import com.example.jenmix.storage.UserPrefs
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class HistoryAdapter(
    private val records: List<WeightRecord>,
    private var ttsEnabled: Boolean = true
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var lastPosition = -1
    private var tts: TextToSpeech? = null
    private val heightM = UserPrefs.height / 100f

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorBar: View = view.findViewById(R.id.viewColorBar)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvWeight: TextView = view.findViewById(R.id.tvWeight)
        val tvBmi: TextView = view.findViewById(R.id.tvBmi)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val iconStatus: ImageView = view.findViewById(R.id.iconStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight_record, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val record = records[position]

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = try {
            inputFormat.parse(record.measuredAt)
        } catch (e: Exception) {
            Log.e("HistoryAdapter", "解析時間錯誤：${record.measuredAt}")
            null
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        holder.tvDate.text = date?.let { dateFormat.format(it) } ?: "日期錯誤"
        holder.tvTime.text = date?.let { "時間：${timeFormat.format(it)}" } ?: "時間錯誤"
        holder.tvWeight.text = "體重：%.1f 公斤".format(record.weight)

        val bmiValue = record.bmi?.takeIf { it > 0f } ?: (record.weight / heightM.pow(2))
        holder.tvBmi.text = "BMI：%.1f".format(bmiValue)

        val statusInfo = when {
            bmiValue < 18.5 -> BmiStatusInfo("過輕", R.color.blue, "🔵", R.drawable.ic_bmi_thin)
            bmiValue < 25 -> BmiStatusInfo("正常", R.color.green, "✅", R.drawable.ic_bmi_normal)
            bmiValue < 30 -> BmiStatusInfo("偏高", R.color.orange, "🟡", R.drawable.ic_bmi_warning)
            else -> BmiStatusInfo("過重", R.color.red, "❌", R.drawable.ic_bmi_danger)
        }

        holder.tvBmi.setTextColor(ContextCompat.getColor(context, statusInfo.colorRes))
        holder.tvStatus.text = "狀態：${statusInfo.statusText}"
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, statusInfo.colorRes))
        holder.iconStatus.setImageResource(statusInfo.iconRes)
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, statusInfo.colorRes))
        holder.iconStatus.setImageResource(statusInfo.iconRes)
        holder.colorBar.setBackgroundColor(ContextCompat.getColor(context, statusInfo.colorRes))

        holder.itemView.setOnClickListener {
            val dialog = HealthDetailDialogFragment.newInstance(
                HealthItem(
                    title = "BMI 分析",
                    value = "%.1f".format(bmiValue),
                    status = statusInfo.statusText,
                    suggestion = generateSuggestion(statusInfo.statusText)
                )
            )
            dialog.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "health_detail_dialog")

            if (ttsEnabled) {
                val speakText = "${holder.tvDate.text}，${holder.tvTime.text}，體重 ${record.weight} 公斤，BMI ${"%.1f".format(bmiValue)}，${statusInfo.statusText}"
                tts?.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                tts?.stop()
            }
        }

        if (holder.bindingAdapterPosition > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            holder.itemView.startAnimation(animation)
            lastPosition = holder.bindingAdapterPosition
        }
    }

    private fun generateSuggestion(status: String): String {
        return when (status) {
            "過輕" -> "你的體重略低，建議多補充蛋白質與均衡飲食。"
            "正常" -> "保持良好狀態，持續運動與飲食控制。"
            "偏高" -> "建議增加運動頻率，控制糖分與油脂攝取。"
            "過重" -> "請積極控制體重，可諮詢營養師或醫師建議。"
            else -> "維持健康生活，持續追蹤體重變化。"
        }
    }

    fun attachTTS(tts: TextToSpeech) {
        this.tts = tts
    }

    fun updateTtsEnabled(enabled: Boolean) {
        this.ttsEnabled = enabled
    }

    data class BmiStatusInfo(
        val statusText: String,
        val colorRes: Int,
        val emoji: String,
        val iconRes: Int
    )
}
