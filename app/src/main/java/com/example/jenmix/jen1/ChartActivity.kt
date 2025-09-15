package com.example.jenmix.jen1

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ChartActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var switchShowDetails: Switch
    private lateinit var diseaseDropdown: AutoCompleteTextView
    private lateinit var userInfoTextView: TextView
    private var selectedDiseaseFilter: String? = null
    private var healthRecords: List<HealthRecord> = listOf()
    private var diseaseMapping: Map<String, String> = mapOf()
    private var currentUsername: String? = null

    /** 由 MyMarkerView 在拿到 GPT 建議後寫回來的疾病標籤（例如：高血壓、體重過重…） */
    var currentDisease: String? = null

    /** 記住最後選到的點（給雙擊手勢使用） */
    private var lastSelectedEntry: Entry? = null
    private var lastSelectedHighlight: Highlight? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_chart_fullscreen)

        lineChart = findViewById(R.id.dialog_chart)
        switchShowDetails = findViewById(R.id.switch_show_details)
        diseaseDropdown = findViewById(R.id.btnSelectDisease)
        userInfoTextView = findViewById(R.id.user_info_text)

        currentUsername = intent.getStringExtra("username")

        setupDiseaseDropdown()
        fetchChartData()

        switchShowDetails.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) addLimitLines(lineChart.axisLeft)
            else lineChart.axisLeft.removeAllLimitLines()
            lineChart.invalidate()
        }
    }

    private fun setupDiseaseDropdown() {
        val diseaseOptions = listOf("全部", "高血壓", "低血壓", "脈搏", "體重")

        // 對應疾病邏輯的 mapping（用於篩選）
        diseaseMapping = mapOf(
            "高血壓" to "血壓",
            "低血壓" to "血壓",
            "脈搏" to "脈搏",
            "體重" to "體重"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, diseaseOptions)
        diseaseDropdown.setAdapter(adapter)

        diseaseDropdown.setOnClickListener { diseaseDropdown.showDropDown() }

        diseaseDropdown.setOnItemClickListener { _, _, position, _ ->
            val selected = diseaseOptions[position]
            selectedDiseaseFilter = if (selected == "全部") null else diseaseMapping[selected]
            diseaseDropdown.setText("📋 $selected", false)
            drawChart()
        }

        diseaseDropdown.setText("📋 全部", false)
    }

    private fun fetchChartData() {
        val username = currentUsername ?: return
        RetrofitClient.instance.getCombinedRecords(username)
            .enqueue(object : Callback<List<HealthRecord>> {
                override fun onResponse(
                    call: Call<List<HealthRecord>>,
                    response: Response<List<HealthRecord>>
                ) {
                    if (response.isSuccessful) {
                        healthRecords = response.body() ?: listOf()
                        healthRecords.firstOrNull()?.let {
                            val genderEmoji = if (it.gender == "男") "🚹" else "🚺"
                            userInfoTextView.text =
                                "👤 ${it.display_name}｜性別：$genderEmoji ${it.gender}｜年齡：🎂 ${it.age} 歲"
                        }
                        drawChart()
                    }
                }

                override fun onFailure(call: Call<List<HealthRecord>>, t: Throwable) {
                    Toast.makeText(this@ChartActivity, "資料載入失敗：${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun drawChart() {
        val entriesSys = ArrayList<Entry>() // 收縮壓
        val entriesDia = ArrayList<Entry>() // 舒張壓
        val entriesPulse = mutableListOf<Entry>()
        val entriesWeight = mutableListOf<Entry>()
        val xLabels = mutableListOf<String>()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        // ✅ 依下拉選擇篩選
        val filtered = healthRecords.filter { record ->
            when (selectedDiseaseFilter) {
                "血壓" -> {
                    val sys = record.systolic_mmHg ?: return@filter false
                    val dia = record.diastolic_mmHg ?: return@filter false
                    sys >= 140 || sys in 120..130 || sys <= 90 || dia >= 90 || dia == 80 || dia <= 60
                }
                "脈搏" -> {
                    val pulse = record.pulse_bpm ?: return@filter false
                    pulse > 120 || pulse < 50
                }
                "體重" -> {
                    val weight = record.weight ?: return@filter false
                    weight > 80 || weight < 45
                }
                else -> true
            }
        }

        healthRecords.forEachIndexed { index, record ->
            val rawDate = record.measuredAt ?: ""
            val formattedDate = try { outputFormat.format(inputFormat.parse(rawDate)!!) } catch (_: Exception) { rawDate }
            xLabels.add(formattedDate)

            record.systolic_mmHg?.let { entriesSys.add(Entry(index.toFloat(), it.toFloat()).apply { data = record }) }
            record.diastolic_mmHg?.let { entriesDia.add(Entry(index.toFloat(), it.toFloat()).apply { data = record }) }
            record.pulse_bpm?.let   { entriesPulse.add(Entry(index.toFloat(), it.toFloat()).apply { data = record }) }
            record.weight?.let      { entriesWeight.add(Entry(index.toFloat(), it).apply { data = record }) }
        }

        // === 依選單決定顯示哪些「線」 ===
        val showBP = (selectedDiseaseFilter == null || selectedDiseaseFilter == "血壓")
        val showPulse = (selectedDiseaseFilter == null || selectedDiseaseFilter == "脈搏")
        val showWeight = (selectedDiseaseFilter == null || selectedDiseaseFilter == "體重")

        val dataSets = mutableListOf<LineDataSet>()

        if (showBP) {
            if (entriesSys.isNotEmpty()) dataSets.add(LineDataSet(entriesSys, "收縮壓").apply {
                color = Color.RED
                lineWidth = 3f
                setDrawValues(false)
                setDrawCircles(true)
                circleRadius = 6f
                circleHoleRadius = 3f
            })
            if (entriesDia.isNotEmpty()) dataSets.add(LineDataSet(entriesDia, "舒張壓").apply {
                color = Color.rgb(255, 140, 0)
                lineWidth = 3f
                setDrawValues(false)
                setDrawCircles(true)
                circleRadius = 6f
                circleHoleRadius = 3f
            })
        }

        if (showPulse && entriesPulse.isNotEmpty()) {
            dataSets.add(LineDataSet(entriesPulse, "脈搏").apply {
                color = Color.MAGENTA
                lineWidth = 3f
                setDrawValues(false)
                setDrawCircles(true)
                circleRadius = 6f
                circleHoleRadius = 3f
            })
        }

        if (showWeight && entriesWeight.isNotEmpty()) {
            dataSets.add(LineDataSet(entriesWeight, "體重").apply {
                color = Color.BLUE
                lineWidth = 3f
                setDrawValues(false)
                setDrawCircles(true)
                circleRadius = 6f
                circleHoleRadius = 3f
            })

            // ✅ 空資料防呆
            if (dataSets.isEmpty()) {
                lineChart.clear()
                lineChart.invalidate()
                Toast.makeText(this, "此篩選條件下無資料可顯示", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 圖表設定
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.isDoubleTapToZoomEnabled = false

        lineChart.setExtraOffsets(20f, 70f, 20f, 60f)
        lineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(xLabels)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.DKGRAY
            textSize = 14f
            labelRotationAngle = -25f
            setLabelCount(xLabels.size, false)
            granularity = 1f
            gridColor = Color.LTGRAY
            gridLineWidth = 1f
            axisLineColor = Color.DKGRAY
            enableGridDashedLine(10f, 10f, 0f)
            setDrawGridLines(true)
            spaceMin = 0.3f
            spaceMax = 0.3f
            yOffset = 10f
            lineChart.setExtraBottomOffset(20f)
        }

        lineChart.axisLeft.apply {
            textSize = 14f
            textColor = Color.DKGRAY
            axisLineColor = Color.DKGRAY
            gridColor = Color.LTGRAY
            spaceTop = 30f // 給 limit line 文字空間

            // ★ 讓 Y 軸一定包含對應的 LimitLine 值
            when (selectedDiseaseFilter) {
                null, "血壓" -> {             // 血壓
                    axisMinimum = 40f
                    axisMaximum = 200f
                }
                "脈搏" -> {                    // 脈搏 50 / 120
                    axisMinimum = 40f
                    axisMaximum = 140f
                }
                "體重" -> {                    // 體重 45 / 80
                    axisMinimum = 0f
                    axisMaximum = 150f
                }
            }
        }
        lineChart.axisRight.isEnabled = false

        lineChart.legend.apply {
            textSize = 18f
            textColor = Color.DKGRAY
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            yOffset = 20f
        }

        lineChart.setExtraOffsets(10f, 30f, 10f, 10f)
        lineChart.data = LineData(dataSets as MutableList<ILineDataSet>)

        // MarkerView（只顯示，不做點擊；點擊交給雙擊手勢）
        val markerView = MyMarkerView(this, R.layout.custom_marker_view, lineChart, "", 1.7f).apply {
            chartView = lineChart
        }
        lineChart.marker = markerView

        // 記住最後選到的點（不在這裡跳轉）
        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                lastSelectedEntry = e
                lastSelectedHighlight = h
            }
            override fun onNothingSelected() {
                lastSelectedEntry = null
                lastSelectedHighlight = null
            }
        })

        // ✅ 手勢雙擊觸發跳轉（不依賴第二次 onValueSelected）
        lineChart.setOnChartGestureListener(object : OnChartGestureListener {
            override fun onChartGestureStart(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {}
            override fun onChartGestureEnd(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {}

            override fun onChartLongPressed(me: MotionEvent?) {}
            override fun onChartDoubleTapped(me: MotionEvent?) { handleDoubleTapJump() }
            override fun onChartSingleTapped(me: MotionEvent?) {}
            override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
        })

        // LimitLine
        lineChart.axisLeft.removeAllLimitLines()
        if (switchShowDetails.isChecked) addLimitLines(lineChart.axisLeft)

        // 動態單位
        val unitSet = mutableSetOf<String>()
        dataSets.forEach { dataSet ->
            when (dataSet.label) {
                "收縮壓", "舒張壓" -> unitSet.add("mmHg")
                "脈搏" -> unitSet.add("bpm")
                "體重" -> unitSet.add("kg")
            }
        }

        lineChart.description.apply {
            isEnabled = true
            text = "單位：${unitSet.joinToString(" / ")}"
            textSize = 15f
            textColor = Color.DKGRAY
            setPosition(
                lineChart.viewPortHandler.contentLeft() + 380f,
                lineChart.viewPortHandler.contentTop() + 0f
            )
        }

        lineChart.invalidate()
    }

    /** 雙擊時執行跳轉 */
    private fun handleDoubleTapJump() {
        val e = lastSelectedEntry ?: run {
            Toast.makeText(this, "請先點一下資料點再雙擊", Toast.LENGTH_SHORT).show()
            return
        }
        val h = lastSelectedHighlight
        val record = e.data as? HealthRecord
        val label = if (h != null) lineChart.data.getDataSetByIndex(h.dataSetIndex).label ?: "" else ""

        val disease = currentDisease ?: inferDisease(record, label, e.y)
        if (disease != null) {
            openSuggestionUrl(disease)
        } else {
            Toast.makeText(this, "查無對應建議類別", Toast.LENGTH_SHORT).show()
        }

        // 清除狀態，確保下次能再次觸發
        lastSelectedEntry = null
        lastSelectedHighlight = null
        lineChart.highlightValue(null, true)
    }

    /** 後備規則：若 MarkerView 尚未提供 currentDisease，就用數值推斷疾病類別 */
    private fun inferDisease(record: HealthRecord?, label: String, y: Float): String? {
        record ?: return null
        return when (label) {
            "收縮壓", "舒張壓" -> {
                val sys = record.systolic_mmHg?.toFloat()
                val dia = record.diastolic_mmHg?.toFloat()
                when {
                    sys != null && sys >= 140f -> "高血壓"
                    dia != null && dia >= 90f -> "高血壓"
                    sys != null && (sys in 120f..130f) -> "血壓偏高"
                    dia != null && dia == 80f -> "血壓偏高"
                    sys != null && sys <= 90f -> "低血壓"
                    dia != null && dia <= 60f -> "低血壓"
                    else -> null
                }
            }
            "脈搏" -> {
                val p = record.pulse_bpm?.toFloat() ?: y
                when {
                    p > 120f -> "脈搏太高"
                    p < 50f  -> "脈搏太低"
                    else -> null
                }
            }
            "體重" -> {
                val w = record.weight?.toFloat() ?: y
                when {
                    w > 80f -> "體重過重"
                    w < 45f -> "體重過輕"
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun openSuggestionUrl(disease: String) {
        Toast.makeText(this, "\uD83D\uDCDA 正在查詢 $disease 建議...", Toast.LENGTH_SHORT).show()
        RetrofitClient.instance.getSourceUrl(disease).enqueue(object : Callback<UrlResponse> {
            override fun onResponse(call: Call<UrlResponse>, response: Response<UrlResponse>) {
                val url = response.body()?.url
                if (!url.isNullOrBlank()) {
                    Toast.makeText(this@ChartActivity, "✅ 正在跳轉建議網頁", Toast.LENGTH_SHORT).show()
                    WebViewActivity.start(this@ChartActivity, url)
                } else {
                    Toast.makeText(this@ChartActivity, "❌ 查無建議網址", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UrlResponse>, t: Throwable) {
                Toast.makeText(this@ChartActivity, "❌ 連線失敗：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun openSuggestionFromMarker(disease: String) {
        openSuggestionUrl(disease)
    }

    private fun addLimitLines(axis: YAxis) {
        axis.removeAllLimitLines()

        val filter = selectedDiseaseFilter
        val allLimitLines = mapOf(
            "血壓" to listOf(
                createLimitLine(140f, "高血壓：收縮壓≥140", Color.RED),
                createLimitLine(90f, "高血壓：舒張壓≥90", Color.RED),
                createLimitLine(130f, "血壓偏高：收縮壓120–130", Color.parseColor("#FFA500")),
                createLimitLine(80f, "血壓偏高：舒張壓=80", Color.parseColor("#FFA500")),
                createLimitLine(90f, "低血壓：收縮壓≤90", Color.BLUE),
                createLimitLine(60f, "低血壓：舒張壓≤60", Color.BLUE),
            ),
            "脈搏" to listOf(
                createLimitLine(120f, "脈搏太高：脈搏>120", Color.MAGENTA),
                createLimitLine(50f, "脈搏太低：脈搏<50", Color.parseColor("#00BCD4")),
            ),
            "體重" to listOf(
                createLimitLine(80f, "體重過重：>80kg", Color.DKGRAY),
                createLimitLine(45f, "體重過輕：<45kg", Color.LTGRAY)
            )
        )

        if (filter == null) {
            allLimitLines.values.flatten().forEach { axis.addLimitLine(it) }
        } else {
            allLimitLines[filter]?.forEach { axis.addLimitLine(it) }
        }
    }

    private fun createLimitLine(value: Float, label: String, color: Int): LimitLine {
        return LimitLine(value, label).apply {
            lineColor = color
            textColor = color
            lineWidth = 1.5f
            textSize = 15f
            enableDashedLine(10f, 5f, 0f)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            xOffset = 10f
            yOffset = 25f
        }
    }
}
