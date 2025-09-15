package com.example.jenmix.hu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.hu.WeightEntry
import com.example.jenmix.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class WeightAdapter(private val weightList: List<WeightEntry>) :
    RecyclerView.Adapter<WeightAdapter.WeightViewHolder>() {

    class WeightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.weightDate)
        val weightTextView: TextView = itemView.findViewById(R.id.weightValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight, parent, false)
        return WeightViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeightViewHolder, position: Int) {
        val entry = weightList[position]
        holder.dateTextView.text = entry.date
        holder.weightTextView.text = entry.weight.toString()
    }

    override fun getItemCount(): Int = weightList.size
}