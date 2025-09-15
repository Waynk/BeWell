package com.example.jenmix.jen3

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*

class DiseaseAdapter(private val context: Context, private val items: List<DiseaseItem>) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]

        return when (item) {
            is DiseaseItem.Description -> {
                val textView = TextView(context)
                textView.text = item.text
                textView.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                textView.textSize = 16f
                textView.setBackgroundColor(Color.parseColor("#F5F5F5"))
                textView.setTextColor(Color.parseColor("#333333"))
                textView
            }

            is DiseaseItem.Video -> {
                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                    layoutParams = AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT,
                        AbsListView.LayoutParams.WRAP_CONTENT
                    )
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#FAFAFA"))
                        cornerRadius = dpToPx(12).toFloat()
                        setStroke(2, Color.parseColor("#CCCCCC"))
                    }
                }

                val title = TextView(context).apply {
                    text = "🎥 ${item.title}"
                    setTextColor(Color.BLACK)
                    textSize = 16f
                }

                val playButton = TextView(context).apply {
                    text = "▶️ 點我觀看影片"
                    setTextColor(Color.BLUE)
                    textSize = 14f
                    setPadding(0, dpToPx(8), 0, 0)
                    setOnClickListener {
                        val intent = Intent(context, YouTubeActivity::class.java)
                        intent.putExtra("youtube_url", item.videoUrl)
                        context.startActivity(intent)
                    }
                }

                val refButton = TextView(context).apply {
                    text = "🌐 資料來源"
                    setTextColor(Color.BLUE)
                    textSize = 14f
                    setPadding(0, dpToPx(4), 0, 0)
                    setOnClickListener {
                        val intent = Intent(context, WebViewActivity::class.java)
                        intent.putExtra("url", item.referenceUrl)
                        context.startActivity(intent)
                    }
                }

                layout.addView(title)
                layout.addView(playButton)
                layout.addView(refButton)

                layout
            }
        }
    }
}
