package com.example.jenmix.jen5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity5 : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var hospitalAdapter: HospitalAdapter

    // 下拉地區
    private val regions = listOf("台北", "新北", "桃園", "台中", "台南", "高雄")

    // 與主畫面一致的 baseUrl
    private val nodeApiBaseUrl by lazy {
        getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("base_url", null)
            ?: "https://test-9wne.onrender.com"
    }

    // Retrofit
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ensureSlash(nodeApiBaseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val api by lazy { retrofit.create(HospitalApi::class.java) }

    // AI 導頁參數
    private var fromAi: Boolean = false
    private var aiHospitalId: Int? = null
    private var aiRegion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main5)

        // 讀取 AI 參數
        fromAi = intent.getBooleanExtra("from_ai", false)
        aiHospitalId = intent.getIntExtra("hospital_id", -1).takeIf { it > 0 }
        aiRegion = intent.getStringExtra("region")?.trim()?.takeIf { it.isNotEmpty() }

        // UI
        spinner = findViewById(R.id.spinner_region)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        hospitalAdapter = HospitalAdapter()
        recyclerView.adapter = hospitalAdapter

        // 地區下拉
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedRegion = regions[position]
                loadHospitals(selectedRegion)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // AI 指定地區就直接切換
        aiRegion?.let { r ->
            val idx = regions.indexOfFirst { it == r }
            if (idx >= 0) spinner.setSelection(idx)
        }
    }

    /** 取醫院清單 */
    private fun loadHospitals(region: String) {
        api.getHospitals(region).enqueue(object : Callback<List<Hospital>> {
            override fun onResponse(call: Call<List<Hospital>>, response: Response<List<Hospital>>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@MainActivity5, "無法載入資料 (${response.code()})", Toast.LENGTH_SHORT).show()
                    return
                }
                val list = response.body().orEmpty()
                hospitalAdapter.submitList(list)

                // AI 導頁：若指定 hospital_id，聚焦並帶出預填對話框
                if (fromAi && aiHospitalId != null) {
                    val index = list.indexOfFirst { it.id == aiHospitalId }
                    if (index >= 0) {
                        recyclerView.scrollToPosition(index)
                        showBookingDialog(list[index])
                    } else {
                        Toast.makeText(this@MainActivity5, "此地區找不到指定醫院，請切換地區或手動選擇", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<Hospital>>, t: Throwable) {
                Toast.makeText(this@MainActivity5, "連線失敗: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** 預填掛號資訊對話框：可「開掛號網址」或「在地圖查看」 */
    private fun showBookingDialog(h: Hospital) {
        val msg = buildString {
            appendLine("🏥 醫院：${h.name}")
            appendLine("📍 地區：${h.region}")
        }.trim()

        MaterialAlertDialogBuilder(this)
            .setTitle("確認掛號資訊")
            .setMessage(msg)
            .setPositiveButton("前往掛號網址") { _, _ ->
                if (h.url.isBlank()) {
                    Toast.makeText(this, "沒有掛號網址可前往", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(h.url)))
                }
            }
            .setNeutralButton("在地圖查看") { _, _ ->
                openInMap(h)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 以 geo:lat,lon?q=name 開地圖 */
    private fun openInMap(h: Hospital) {
        val geo = Uri.parse("geo:${h.latitude},${h.longitude}?q=${Uri.encode(h.name)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, geo)
        mapIntent.setPackage("com.google.android.apps.maps") // 若裝了 Google 地圖優先
        try {
            startActivity(mapIntent)
        } catch (_: Exception) {
            // 沒有 Google Maps 就用任何可處理的地圖 App/瀏覽器
            startActivity(Intent(Intent.ACTION_VIEW, geo))
        }
    }

    private fun ensureSlash(url: String): String = if (url.endsWith("/")) url else "$url/"
}
