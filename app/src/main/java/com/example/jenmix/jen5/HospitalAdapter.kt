package com.example.jenmix.jen5

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R

class HospitalAdapter : RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {
    private var hospitals = listOf<Hospital>()

    fun submitList(newList: List<Hospital>) {
        hospitals = newList
        notifyDataSetChanged()
    }

    inner class HospitalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvUrl: TextView = view.findViewById(R.id.tv_url)
        val tvMap: TextView = view.findViewById(R.id.tv_map)  // 地圖導航

        init {
            tvUrl.setOnClickListener {
                val hospital = hospitals[adapterPosition]
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(hospital.url))
                view.context.startActivity(browserIntent)
            }

            tvMap.setOnClickListener {
                val hospital = hospitals[adapterPosition]
                val uri = Uri.parse("geo:${hospital.latitude},${hospital.longitude}?q=${Uri.encode(hospital.name)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                view.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hospital, parent, false)
        return HospitalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        val hospital = hospitals[position]
        holder.tvName.text = hospital.name
        holder.tvUrl.text = "🔗 點我掛號"
        holder.tvMap.text = "🗺️ 地圖導航"
    }

    override fun getItemCount() = hospitals.size
}
