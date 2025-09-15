package com.example.jenmix.hu

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BloodPressureAdapter(private val dataList: List<BloodPressureData2>) :
    RecyclerView.Adapter<BloodPressureAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView? = view.findViewById(R.id.dateText)
        val sysText: TextView? = view.findViewById(R.id.sysText)
        val diaText: TextView? = view.findViewById(R.id.diaText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blood_pressure, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]

        val formattedDate = formatDate(data.date)
        val sysStr = "${data.sys} mmHg"
        val diaStr = "${data.dia} mmHg"

        val isNormal = (data.sys in 90..120) && (data.dia in 60..80)
        val isHigh = (data.sys in 120..140) || (data.dia in 80..90)
        val isLow = (data.sys < 90) || (data.dia < 60)
        val isDanger = (data.sys > 140) || (data.dia > 90)

        when {
            isDanger -> {
                val color = Color.parseColor("#8B0000")
                setBoldText(holder.sysText, sysStr, isLarge = true)
                setBoldText(holder.diaText, diaStr, isLarge = true)
                setBoldText(holder.dateText, formattedDate, isLarge = true)

                holder.sysText?.setTextColor(color)
                holder.diaText?.setTextColor(color)
                holder.dateText?.setTextColor(color)

            }
            isHigh -> {
                val color = Color.parseColor("#FFCC00")
                setBoldText(holder.sysText, sysStr, isLarge = true)
                setBoldText(holder.diaText, diaStr, isLarge = true)
                setBoldText(holder.dateText, formattedDate, isLarge = true)

                holder.sysText?.setTextColor(color)
                holder.diaText?.setTextColor(color)
                holder.dateText?.setTextColor(color)
            }
            isLow -> {
                val color = Color.parseColor("#1E90FF")
                setBoldText(holder.sysText, sysStr, isLarge = true)
                setBoldText(holder.diaText, diaStr, isLarge = true)
                setBoldText(holder.dateText, formattedDate, isLarge = true)

                holder.sysText?.setTextColor(color)
                holder.diaText?.setTextColor(color)
                holder.dateText?.setTextColor(color)
            }
            isNormal -> {
                holder.sysText?.text = sysStr
                holder.diaText?.text = diaStr
                holder.dateText?.text = formattedDate

                holder.sysText?.setTextColor(Color.BLACK)
                holder.diaText?.setTextColor(Color.BLACK)
                holder.dateText?.setTextColor(Color.BLACK)

                holder.sysText?.textSize = 16f
                holder.diaText?.textSize = 16f
                holder.dateText?.textSize = 16f

                holder.sysText?.setTypeface(null, Typeface.NORMAL)
                holder.diaText?.setTypeface(null, Typeface.NORMAL)
                holder.dateText?.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    private fun formatDate(originalDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()

            val date = inputFormat.parse(originalDate)
            outputFormat.format(date ?: originalDate)
        } catch (e: Exception) {
            originalDate
        }
    }

    private fun setBoldText(textView: TextView?, text: String, isLarge: Boolean = false) {
        val spannable = SpannableString(text)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD), 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView?.text = spannable
        textView?.textSize = if (isLarge) 18f else 16f
    }
}
