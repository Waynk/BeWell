package com.example.jenmix.jen8

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jenmix.databinding.ActivityHistoryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.jenmix.storage.UserPrefs
import com.example.jenmix.R
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class HistoryActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private lateinit var tts: TextToSpeech
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentDate = Calendar.getInstance()
    private var currentMode = "week"
    private var ttsEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)
        ttsEnabled = UserPrefs.isTtsEnabled(this)
        binding.switchTts.isChecked = ttsEnabled

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        binding.recyclerHistory.layoutAnimation = animation

        binding.fabScrollToTop.setOnClickListener {
            it.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
            if (binding.recyclerHistory.adapter?.itemCount ?: 0 > 0) {
                binding.recyclerHistory.smoothScrollToPosition(0)
            }
        }

        val options = resources.getStringArray(R.array.query_options)
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        binding.spinnerPeriod.adapter = adapterSpinner
        binding.spinnerPeriod.setSelection(1)

        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentDate = Calendar.getInstance()
                currentMode = when (position) {
                    0 -> "all"
                    1 -> "week"
                    2 -> "month"
                    3 -> "year"
                    4 -> "custom"
                    else -> "week"
                }
                if (currentMode == "custom") {
                    showCustomDatePicker()
                } else {
                    loadData()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.btnPrevPeriod.setOnClickListener {
            when (currentMode) {
                "week" -> currentDate.add(Calendar.WEEK_OF_YEAR, -1)
                "month" -> currentDate.add(Calendar.MONTH, -1)
                "year" -> currentDate.add(Calendar.YEAR, -1)
            }
            loadData()
        }

        binding.btnNextPeriod.setOnClickListener {
            when (currentMode) {
                "week" -> currentDate.add(Calendar.WEEK_OF_YEAR, 1)
                "month" -> currentDate.add(Calendar.MONTH, 1)
                "year" -> currentDate.add(Calendar.YEAR, 1)
            }
            loadData()
        }

        binding.switchTts.setOnCheckedChangeListener { _, isChecked ->
            ttsEnabled = isChecked
            UserPrefs.setTtsEnabled(this, isChecked)
            if (!isChecked) tts.stop()
            adapter.updateTtsEnabled(isChecked)
        }

        loadData()
    }

    private fun showCustomDatePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("選擇查詢期間")
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            if (startDate != null && endDate != null) {
                val startStr = sdf.format(Date(startDate))
                val endStr = sdf.format(Date(endDate))
                binding.tvSelectedDate.text = "查詢期間：$startStr ~ $endStr"

                val username = UserPrefs.getUsername(this)
                if (username.isNullOrBlank()) {
                    Toast.makeText(this, "⚠️ 使用者未登入", Toast.LENGTH_SHORT).show()
                    return@addOnPositiveButtonClickListener
                }

                RetrofitClient.create<HistoryApi>()
                    .getWeightHistory(startStr, endStr, username)
                    .enqueue(getCallback(startStr, endStr))
            }
        }

        picker.show(supportFragmentManager, picker.toString())
    }

    private fun loadData() {
        val (start, end) = getDateRange(currentDate, currentMode)

        val username = UserPrefs.getUsername(this)
        if (username.isNullOrBlank()) {
            Toast.makeText(this, "⚠️ 使用者未登入", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.create<HistoryApi>()
            .getWeightHistory(start, end, username)
            .enqueue(object : Callback<List<WeightRecord>> {
                override fun onResponse(call: Call<List<WeightRecord>>, response: Response<List<WeightRecord>>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@HistoryActivity, "資料載入失敗", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val records = response.body().orEmpty()
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    records.forEach {
                        it.parsedDate = try {
                            sdf.parse(it.measuredAt)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (records.isEmpty()) {
                        binding.recyclerHistory.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvSelectedDate.text = getString(R.string.query_period, start, end)
                    } else {
                        binding.recyclerHistory.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE

                        adapter = HistoryAdapter(records)
                        adapter.attachTTS(tts)
                        binding.recyclerHistory.adapter = adapter
                        binding.recyclerHistory.scheduleLayoutAnimation()

                        // ✅ 修正錯誤點：格式化前先檢查是否為 Date
                        val minDate = records
                            .filter { it.parsedDate != null }
                            .minByOrNull { it.parsedDate!! }
                            ?.parsedDate

                        val realStartDate = try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(minDate)
                        } catch (e: Exception) {
                            Log.e("DateFormat", "無法格式化日期：${e.message}")
                            start
                        }

                        binding.tvSelectedDate.text = getString(R.string.query_period, realStartDate, end)
                    }
                }

                override fun onFailure(call: Call<List<WeightRecord>>, t: Throwable) {
                    Toast.makeText(this@HistoryActivity, "伺服器錯誤", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getCallback(start: String, end: String) = object : Callback<List<WeightRecord>> {
        override fun onResponse(call: Call<List<WeightRecord>>, response: Response<List<WeightRecord>>) {
            if (!response.isSuccessful) {
                Toast.makeText(this@HistoryActivity, "資料載入失敗", Toast.LENGTH_SHORT).show()
                return
            }
            val records = response.body().orEmpty()
            if (records.isEmpty()) {
                binding.recyclerHistory.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.recyclerHistory.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE

                adapter = HistoryAdapter(records, ttsEnabled)
                adapter.attachTTS(tts)
                binding.recyclerHistory.adapter = adapter
                binding.recyclerHistory.scheduleLayoutAnimation()

                val recent = records.takeLast(3)
                val abnormalCount = recent.count {
                    val bmi = it.bmi ?: (it.weight / ((UserPrefs.height / 100f).pow(2)))
                    bmi < 18.5 || bmi >= 30
                }
                if (abnormalCount >= 3) {
                    Toast.makeText(this@HistoryActivity, "⚠️ 最近三天體重有異常波動，請留意健康狀況！", Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onFailure(call: Call<List<WeightRecord>>, t: Throwable) {
            Toast.makeText(this@HistoryActivity, "伺服器錯誤", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDateRange(base: Calendar, mode: String): Pair<String, String> {
        val start = base.clone() as Calendar
        val end = base.clone() as Calendar
        when (mode) {
            "week" -> {
                start.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                end.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            }
            "month" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, start.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            "year" -> {
                start.set(Calendar.MONTH, Calendar.JANUARY)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.MONTH, Calendar.DECEMBER)
                end.set(Calendar.DAY_OF_MONTH, 31)
            }
            "all" -> {
                start.set(2000, Calendar.JANUARY, 1)
                end.time = Calendar.getInstance().time
            }
        }
        return sdf.format(start.time) to sdf.format(end.time)
    }

    override fun onInit(status: Int) {}

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
