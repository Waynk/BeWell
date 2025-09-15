package com.example.jenmix.jen8

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.databinding.ActivityChartBinding
import com.example.jenmix.jen8.model.WeightRecordLocal
import com.example.jenmix.jen8.WeightLineChartHelper
import com.example.jenmix.jen8.WeightPieChartHelper
import com.example.jenmix.jen8.WeightUtils
import com.example.jenmix.jen8.HistoryApi
import com.example.jenmix.jen8.RetrofitClient
import com.example.jenmix.storage.UserPrefs
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class ChartActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityChartBinding
    private var username: String? = null
    private var currentChart = "line"
    private var allRecords: List<WeightRecordLocal> = emptyList()

    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    private val sdfInput = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val sdfDisplay = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    private lateinit var tts: TextToSpeech
    private var isTtsEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        username = UserPrefs.getUsername(this)
        if (username.isNullOrBlank()) {
            Toast.makeText(this, "⚠️ 尚未登入", Toast.LENGTH_SHORT).show()
            finish()
        }

        tts = TextToSpeech(this, this)
        isTtsEnabled = UserPrefs.isTTSEnabled(this)
        binding.switchTts.isChecked = isTtsEnabled

        setupUiListeners()
        setupDateRangeControls()
        loadWeightData()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.TAIWAN
        }
    }

    private fun setupUiListeners() {
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity8::class.java))
            finish()
        }

        binding.btnLineChart.setOnClickListener {
            currentChart = "line"
            switchChart("line")
        }

        binding.btnPieChart.setOnClickListener {
            currentChart = "pie"
            switchChart("pie")
        }

        binding.switchTts.setOnCheckedChangeListener { _, isChecked ->
            isTtsEnabled = isChecked
            UserPrefs.setTTSEnabled(this, isChecked)
            if (!isChecked) tts.stop()
        }
    }

    private fun setupDateRangeControls() {
        val rangeOptions = listOf("全部", "近 7 日", "本月", "自訂區間")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rangeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDateRange.adapter = adapter

        binding.spinnerDateRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (rangeOptions[position]) {
                    "全部" -> {
                        binding.layoutCustomRange.visibility = View.GONE
                        applyDateFilter(null, null)
                    }
                    "近 7 日" -> {
                        binding.layoutCustomRange.visibility = View.GONE
                        val today = Calendar.getInstance()
                        val calStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                        val endOfToday = extendToEndOfDay(today.time)
                        applyDateFilter(calStart.time, endOfToday)
                    }
                    "本月" -> {
                        binding.layoutCustomRange.visibility = View.GONE
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val start = cal.time
                        val end = Date()
                        applyDateFilter(start, end)
                    }
                    "自訂區間" -> {
                        binding.layoutCustomRange.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.tvStartDate.setOnClickListener {
            showDatePicker { date ->
                selectedStartDate = date
                binding.tvStartDate.text = "開始日期：${sdfDisplay.format(date)}"
            }
        }

        binding.tvEndDate.setOnClickListener {
            showDatePicker { date ->
                selectedEndDate = date
                binding.tvEndDate.text = "結束日期：${sdfDisplay.format(date)}"
            }
        }

        binding.btnApplyCustomRange.setOnClickListener {
            if (selectedStartDate != null && selectedEndDate != null) {
                applyDateFilter(selectedStartDate!!, selectedEndDate!!)
            } else {
                Toast.makeText(this, "請選擇完整區間", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance()
                selected.set(year, month, dayOfMonth, 0, 0, 0)
                onDateSelected(selected.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadWeightData() {
        val api = RetrofitClient.create<HistoryApi>()
        username?.let { name ->
            api.getChartData(name).enqueue(object : Callback<List<WeightRecord>> {
                override fun onResponse(call: Call<List<WeightRecord>>, response: Response<List<WeightRecord>>) {
                    if (response.isSuccessful) {
                        val records = response.body() ?: emptyList()
                        val converted = records.map {
                            WeightRecordLocal(
                                date = sdfInput.parse(it.measuredAt) ?: Date(),
                                weight = it.weight,
                                height = it.height,
                                gender = it.gender,
                                age = it.age
                            )
                        }
                        allRecords = converted
                        applyDateFilter(null, null)
                    } else {
                        Toast.makeText(this@ChartActivity, "❌ 資料載入失敗", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<WeightRecord>>, t: Throwable) {
                    Toast.makeText(this@ChartActivity, "🚫 無法連接伺服器", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun applyDateFilter(start: Date?, end: Date?) {
        val adjustedEnd = end?.let { extendToEndOfDay(it) }

        val filtered = allRecords.filter {
            val date = it.date
            (start == null || !date.before(start)) && (adjustedEnd == null || !date.after(adjustedEnd))
        }

        WeightLineChartHelper.setupLineChart(binding.lineChart, filtered) { suggestion ->
            if (isTtsEnabled) tts.speak(suggestion, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        val statsMap = WeightPieChartHelper.setupPieChart(binding.pieChart, filtered)
        val df = DecimalFormat("#.##")
        val total = statsMap.values.sumOf { it.first }

        val statsText = statsMap.entries.joinToString("\n") { (category, pair) ->
            val (count, percent) = pair
            "$category：$count 次（${df.format(percent)}%）"
        }
        binding.tvPieStats.text = statsText
        binding.tvPieStats.visibility = View.VISIBLE

        setupChartInteraction(filtered)
        switchChart(currentChart)
    }

    private fun setupChartInteraction(records: List<WeightRecordLocal>) {
        val marker = CustomMarkerView(this)
        marker.chartView = binding.lineChart
        binding.lineChart.marker = marker

        binding.lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val record = e?.data as? WeightRecordLocal ?: return
                val status = WeightUtils.getWeightStatus(record.weight, record.height)
                val dateText = SimpleDateFormat("yyyy 年 M 月 d 日", Locale.TAIWAN).format(record.date)
                val timeText = SimpleDateFormat("HH 點 mm 分 ss 秒", Locale.TAIWAN).format(record.date)
                val speakText = "$dateText，時間 $timeText，體重 ${record.weight} 公斤，屬於 $status"
                if (isTtsEnabled) {
                    tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            override fun onNothingSelected() {}
        })
    }

    private fun switchChart(type: String) {
        val fadeIn: Animation = AlphaAnimation(0f, 1f).apply { duration = 500 }

        val legendStatus = binding.legendStatus.root
        val legendTrend = binding.legendTrend.root

        when (type) {
            "line" -> {
                binding.lineChart.visibility = View.VISIBLE
                binding.lineChart.startAnimation(fadeIn)
                binding.pieChart.visibility = View.GONE
                binding.tvPieStats.visibility = View.GONE

                binding.tvAxisUnitLabel.visibility = View.VISIBLE
                legendStatus.visibility = View.VISIBLE
                legendTrend.visibility = View.VISIBLE
            }
            "pie" -> {
                binding.pieChart.visibility = View.VISIBLE
                binding.pieChart.startAnimation(fadeIn)
                binding.lineChart.visibility = View.GONE
                binding.tvPieStats.visibility = View.VISIBLE

                binding.tvAxisUnitLabel.visibility = View.GONE
                legendStatus.visibility = View.GONE
                legendTrend.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }

    private fun extendToEndOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }

}
