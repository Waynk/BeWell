package com.example.jenmix.jen8

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R

class HealthCardAdapter(private val items: MutableList<HealthItem>) :
    RecyclerView.Adapter<HealthCardAdapter.ViewHolder>() {

    private var onCardClickListener: ((HealthItem) -> Unit)? = null

    fun setOnCardClickListener(listener: (HealthItem) -> Unit) {
        this.onCardClickListener = listener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvCardCategory)
        val tvValue: TextView = view.findViewById(R.id.tvCardValue)
        val tvStatus: TextView = view.findViewById(R.id.tvCardStatus)
        val tvTime: TextView = view.findViewById(R.id.tvCardTime)
        val ivStatusIcon: ImageView = view.findViewById(R.id.ivCardStatusIcon)
        val cardView: CardView = view.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_analysis_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvCategory.text = item.title
        holder.tvValue.text = item.value
        holder.tvStatus.text = item.status
        holder.tvTime.text = "🕓 測量時間：${item.measuredTime}"

        // ✅ 狀態樣式與 icon 判斷
        val (bgDrawable, labelColor, statusColor, valueColor, iconRes) = when (item.status) {
            "正常", "良好", "不肥胖" -> Penta(
                R.drawable.bg_card_green,
                "#388E3C", "#388E3C", "#2E7D32",
                R.drawable.ic_bmi_normal
            )
            "偏高", "稍高", "微胖", "偏重" -> Penta(
                R.drawable.bg_card_orange,
                "#FFA000", "#F57C00", "#EF6C00",
                R.drawable.ic_bmi_warning
            )
            "異常", "過高", "極高", "肥胖", "極胖" -> Penta(
                R.drawable.bg_card_red,
                "#D32F2F", "#C62828", "#B71C1C",
                R.drawable.ic_bmi_danger
            )
            "偏低", "過低" -> Penta(
                R.drawable.bg_card_blue,
                "#2196F3", "#1976D2", "#0D47A1",
                R.drawable.ic_bmi_thin
            )
            else -> Penta(
                R.drawable.bg_card_gray,
                "#607D8B", "#607D8B", "#455A64",
                R.drawable.ic_bmi_unknown
            )
        }

        holder.cardView.setBackgroundResource(bgDrawable)
        holder.tvCategory.setTextColor(android.graphics.Color.parseColor(labelColor))
        holder.tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor))
        holder.tvValue.setTextColor(android.graphics.Color.parseColor(valueColor))
        holder.ivStatusIcon.setImageResource(iconRes)

        // ✅ 卡片進場動畫
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 100f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay((position * 100).toLong())
            .start()

        // ✅ 點擊動畫 + 傳遞點擊資料
        holder.itemView.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                it.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        onCardClickListener?.invoke(items[holder.adapterPosition])
                    }
                    .start()
            }
        }
    }

    override fun getItemCount(): Int = items.size

    data class Penta(
        val bgResId: Int,
        val labelColor: String,
        val statusColor: String,
        val valueColor: String,
        val iconResId: Int
    )
}
