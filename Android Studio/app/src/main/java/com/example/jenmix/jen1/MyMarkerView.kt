package com.example.jenmix.jen1

import android.content.Context
import android.graphics.Paint
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.example.jenmix.R
import kotlinx.coroutines.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/** 分析快取：同一筆(時間+focus)不重複呼叫後端 */
object MarkerAnalysisCache {
    val map = ConcurrentHashMap<String, String>()
}

class MyMarkerView(
    context: Context,
    layoutResource: Int,
    private val chartRef: LineChart,
    private val gender: String,
    private val heightM: Float
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val tvLink: TextView = findViewById(R.id.tvLink)
    private val TAG = "MyMarkerView"
    private val markerScope = MainScope()

    // 由建議文字推導出的疾病標籤（雙擊時使用）
    private var diseaseForUrl: String? = null

    // 雙擊偵測器
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            diseaseForUrl?.let {
                (context as? ChartActivity)?.openSuggestionFromMarker(it)
            } ?: run {
                Toast.makeText(context, "建議尚未就緒，請稍候再試", Toast.LENGTH_SHORT).show()
            }
            return true
        }
    })

    init {
        tvContent.movementMethod = ScrollingMovementMethod.getInstance()

        // 泡泡本身吃手勢事件（含雙擊）
        setOnTouchListener { _, ev ->
            gestureDetector.onTouchEvent(ev)
            // 讓內文還是能滾動
            if (ev.action == MotionEvent.ACTION_MOVE) parent.requestDisallowInterceptTouchEvent(true)
            true
        }

        // 連結文字也可單擊開啟（輔助 UX）
        tvLink.paintFlags = tvLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        tvLink.setOnClickListener {
            diseaseForUrl?.let { d ->
                (context as? ChartActivity)?.openSuggestionFromMarker(d)
            } ?: Toast.makeText(context, "建議尚未就緒，請稍候再試", Toast.LENGTH_SHORT).show()
        }
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null || highlight == null) return
        markerScope.coroutineContext.cancelChildren()

        val record = e.data as? HealthRecord ?: return
        val timeDisplay = formatToTaiwanTime(record.measuredAt)

        // 1) 判斷被點到哪一條線
        val clickedLabel = chartRef.data.getDataSetByIndex(highlight.dataSetIndex).label ?: ""
        val focus = when (clickedLabel) {
            "收縮壓", "舒張壓" -> "BP"
            "脈搏"   -> "PULSE"
            "體重"   -> "WEIGHT"
            else     -> "ALL"
        }

        val baseText = "🕒 時間：$timeDisplay\n📏 測量值：${"%.1f".format(e.y)}\n"

        // 2) 只傳遞被點指標；其餘清空，避免 GPT 談到其他指標
        val sysStr   = if (focus == "BP"    || focus == "ALL") record.systolic_mmHg?.toString().orEmpty() else ""
        val diaStr   = if (focus == "BP"    || focus == "ALL") record.diastolic_mmHg?.toString().orEmpty() else ""
        val pulseStr = if (focus == "PULSE" || focus == "ALL") record.pulse_bpm?.toString().orEmpty()  else ""
        val wStr     = if (focus == "WEIGHT"|| focus == "ALL") record.weight?.toString().orEmpty()     else ""

        // 3) 快取鍵（加入 focus）
        val cacheKey = "${record.measuredAt ?: "${e.x}_${e.y}"}|$focus"

        // 先用快取
        MarkerAnalysisCache.map[cacheKey]?.let { cached ->
            tvContent.text = "$baseText💡 $cached"
            diseaseForUrl = inferDiseaseFromSuggestion(cached)
            updateLinkHintVisibility()
            remeasureAndLayout()
            super.refreshContent(e, highlight)
            return
        }

        // 顯示 loading
        tvContent.text = "$baseText💡 GPT 分析中..."
        diseaseForUrl = null
        updateLinkHintVisibility()
        remeasureAndLayout()
        super.refreshContent(e, highlight)

        markerScope.launch(Dispatchers.IO) {
            try {
                val gptResponse = RetrofitClient.instance.analyzeCombinedRecord(
                    mapOf(
                        "focus"          to focus,
                        "systolic_mmHg"  to sysStr,
                        "diastolic_mmHg" to diaStr,
                        "pulse_bpm"      to pulseStr,
                        "weight_kg"      to wStr,
                        "height_cm"      to (heightM * 100).toString(),
                        "gender"         to gender,
                        "age"            to record.age.toString(),
                        "measured_at"    to record.measuredAt
                    )
                ).execute()

                val suggestion = gptResponse.body()?.suggestion ?: "GPT 分析失敗"
                MarkerAnalysisCache.map[cacheKey] = suggestion
                Log.d(TAG, "✅ GPT($focus) 建議：$suggestion")

                // 依 suggestion 推導疾病標籤（雙擊用）
                diseaseForUrl = inferDiseaseFromSuggestion(suggestion)

                withContext(Dispatchers.Main) {
                    // 同步到 Activity（若你有用 ChartActivity.currentDisease）
                    (context as? ChartActivity)?.currentDisease = diseaseForUrl

                    tvContent.text = "$baseText💡 $suggestion"
                    tvContent.setSingleLine(false)
                    tvContent.maxLines = 10
                    tvContent.scrollTo(0, 0)

                    updateLinkHintVisibility()

                    // —— 強制刷新序列 —— //
                    remeasureAndLayout()
                    super.refreshContent(e, highlight)

                    val newH = Highlight(
                        highlight.x, highlight.y, highlight.xPx, highlight.yPx,
                        highlight.dataSetIndex, highlight.axis
                    )
                    chartRef.highlightValue(null)
                    chartRef.highlightValue(newH, true)
                    chartRef.invalidate()
                    chartRef.post {
                        chartRef.highlightValue(newH, true)
                        chartRef.invalidate()
                    }
                }
            } catch (ce: CancellationException) {
                Log.w(TAG, "⚠️ 分析中斷：切換資料點")
            } catch (ex: Exception) {
                Log.e(TAG, "❌ GPT 分析錯誤：${ex.message}")
                withContext(Dispatchers.Main) {
                    tvContent.text = "$baseText💡 GPT 分析失敗"
                    diseaseForUrl = null
                    updateLinkHintVisibility()
                    remeasureAndLayout()
                    super.refreshContent(e, highlight)
                    chartRef.invalidate()
                }
            }
        }
    }

    /** 由建議文字推導疾病標籤（與 ChartActivity 一致） */
    private fun inferDiseaseFromSuggestion(s: String): String? = when {
        s.contains("高血壓")   -> "高血壓"
        s.contains("低血壓")   -> "低血壓"
        s.contains("體重過重") -> "體重過重"
        s.contains("體重過輕") -> "體重過輕"
        s.contains("脈搏太高") -> "脈搏太高"
        s.contains("脈搏太低") -> "脈搏太低"
        else -> null
    }

    /** 重新量測與配置，確保內容改變會反映在尺寸上 */
    private fun remeasureAndLayout() {
        measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        layout(0, 0, measuredWidth, measuredHeight)
        invalidate()
        requestLayout()
    }

    /** 依疾病是否就緒顯示提示文案 */
    private fun updateLinkHintVisibility() {
        if (diseaseForUrl != null) {
            tvLink.visibility = View.VISIBLE
            tvLink.text = "🔗  雙擊查看建議"
        } else {
            tvLink.visibility = View.GONE
        }
    }

    override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat())

    private fun formatToTaiwanTime(raw: String?): String = try {
        val z = ZonedDateTime.parse(raw).withZoneSameInstant(ZoneId.of("Asia/Taipei"))
        DateTimeFormatter.ofPattern("yyyy/MM/dd").format(z)
    } catch (_: Exception) { "無" }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        markerScope.cancel()
    }

    // 預設建構子
    constructor(context: Context) : this(context, R.layout.custom_marker_view, LineChart(context), "男", 1.7f)
    constructor(context: Context, attrs: AttributeSet) : this(context, R.layout.custom_marker_view, LineChart(context), "男", 1.7f)
}
