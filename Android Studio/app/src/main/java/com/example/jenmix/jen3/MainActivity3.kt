package com.example.jenmix.jen3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.jenmix.R

class MainActivity3 : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var btnLoad: Button
    private lateinit var swToggle: Switch
    private lateinit var lvCombined: ListView
    private lateinit var cardCombined: View
    private lateinit var diseaseList: List<Disease>
    private var descriptionText: String = ""
    private var videoList: List<DiseaseVideo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        spinner = findViewById(R.id.spinnerDiseases)
        btnLoad = findViewById(R.id.btnLoadDisease)
        swToggle = findViewById(R.id.swToggleVideo)
        lvCombined = findViewById(R.id.lvCombined)
        cardCombined = findViewById<View>(R.id.cardCombined)
        lvCombined.visibility = View.GONE

        // 🧬 取得病症資料
        RetrofitClient.apiService.getAllDiseases().enqueue(object : Callback<List<Disease>> {
            override fun onResponse(call: Call<List<Disease>>, response: Response<List<Disease>>) {
                if (response.isSuccessful) {
                    diseaseList = response.body() ?: emptyList()
                    val names = diseaseList.map { "🩺 ${it.name}" }
                    val adapter = ArrayAdapter(this@MainActivity3, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                }
            }

            override fun onFailure(call: Call<List<Disease>>, t: Throwable) {
                Toast.makeText(this@MainActivity3, "⚠️ 載入病症失敗", Toast.LENGTH_SHORT).show()
            }
        })

        // 📌 點擊查詢病症資訊
        btnLoad.setOnClickListener {
            val index = spinner.selectedItemPosition
            val disease = diseaseList[index]

            // 儲存描述文字
            descriptionText = "📖 說明：${disease.description}"

            // 🚫 清空列表暫時
            lvCombined.adapter = null
            lvCombined.visibility = View.GONE

            // 🎬 取得影片清單
            RetrofitClient.apiService.getVideosByDisease(disease.id).enqueue(object : Callback<List<DiseaseVideo>> {
                override fun onResponse(call: Call<List<DiseaseVideo>>, response: Response<List<DiseaseVideo>>) {
                    if (response.isSuccessful) {
                        videoList = response.body() ?: emptyList()
                        updateListView()
                    }
                }

                override fun onFailure(call: Call<List<DiseaseVideo>>, t: Throwable) {
                    Toast.makeText(this@MainActivity3, "⚠️ 影片載入失敗", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 🔀 開關切換顯示
        swToggle.setOnCheckedChangeListener { _, _ ->
            updateListView()
        }

        // 🌐 點擊影片跳轉 WebView
        lvCombined.setOnItemClickListener { _, _, position, _ ->
            val item = lvCombined.adapter.getItem(position)
            if (item is DiseaseItem.Video) {
                val cleanUrl = cleanUrl(item.videoUrl)
                val intent = if (isYoutubeLink(cleanUrl)) {
                    Intent(this, YouTubeActivity::class.java).apply {
                        putExtra("youtube_url", cleanUrl)
                    }
                } else {
                    Intent(this, WebViewActivity::class.java).apply {
                        putExtra("url", cleanUrl)
                    }
                }
                startActivity(intent)
            }
        }

        // 🎯 更換病症時清空列表
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                lvCombined.adapter = null
                lvCombined.visibility = View.GONE
                descriptionText = ""
                videoList = emptyList()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun cleanUrl(youtube_url: String): String {
        return youtube_url
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF]"), "")
            .replace("http://https://", "https://")
            .replace("​https://", "https://")
            .replace("http://", "https://")
            .trim()
    }

    private fun isYoutubeLink(link: String): Boolean {
        return link.contains("youtube.com/watch") || link.contains("youtu.be/")
    }

    // 📋 切換顯示內容：說明 或 影片
    private fun updateListView() {
        val showVideo = swToggle.isChecked
        val items = mutableListOf<DiseaseItem>()

        if (showVideo) {
            items.addAll(videoList.map {
                DiseaseItem.Video(
                    title = it.title,
                    videoUrl = it.video_url,
                    referenceUrl = it.reference_url,
                )
            })
        }else {
            items.add(DiseaseItem.Description(descriptionText))
        }

        lvCombined.adapter = DiseaseAdapter(this, items)
        lvCombined.visibility = View.VISIBLE
        cardCombined.visibility = View.VISIBLE // ✅ 讓 CardView 顯示
    }
}
