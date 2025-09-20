package com.example.jenmix.hu

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.RelativeSizeSpan
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.MainMenuActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textview.MaterialTextView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import com.example.jenmix.R
import com.github.mikephil.charting.data.Entry

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

// OkHttp3 扩展 toMediaType, toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// Kotlin math
import kotlin.math.abs

import android.graphics.Color
import android.text.style.ForegroundColorSpan

// Glide 相關
import com.bumptech.glide.Glide




import android.widget.*
import android.net.Uri
import androidx.appcompat.app.AlertDialog







class FourthActivity : AppCompatActivity() {








    private lateinit var btnShowChart: Button
    private lateinit var lineChart: LineChart


    private lateinit var explanationCard: MaterialCardView
    private lateinit var explanationTextView: TextView


    private var startDate: String? = null
    private var endDate: String? = null




    private val questions = listOf(
        "1. 感到緊張、不安或煩躁(兩個禮拜內)",
        "2. 無法停止或控制憂慮(兩個禮拜內)",
        "3. 過份憂慮不同的事情(兩個禮拜內)",
        "4. 難以放鬆(兩個禮拜內)",
        "5. 心情不寧以至坐立不安(兩個禮拜內)",
        "6. 容易心煩或易怒(兩個禮拜內)",
        "7. 感到害怕，就像要發生可怕的事情(兩個禮拜內)"
    )




    private val scoreList = mutableListOf<RadioGroup>()
    private lateinit var container: LinearLayout
    private lateinit var submitButton: MaterialButton




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fourth)





        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.VISIBLE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.VISIBLE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.VISIBLE

        val buttonGroup = findViewById<LinearLayout>(R.id.warningButtons)
        val warningContainer = findViewById<LinearLayout>(R.id.warningContainer)
        warningContainer.visibility = View.GONE
        buttonGroup.visibility = View.GONE


        imageButton.setOnClickListener {
            val imageButton = findViewById<ImageButton>(R.id.headerImage)
            imageButton.visibility = View.GONE

            val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
            imageButton2.visibility = View.VISIBLE
        }





        val headerTextView = findViewById<TextView>(R.id.headerText)
        val spannable = SpannableString("\uD83D\uDE0C\uD83C\uDF3F 焦慮指數測量:")

// 設定深藍色，顏色用 Color.parseColor 或直接用 Color.rgb
        val darkBlue = Color.parseColor("#00008B")

// 先設定顏色
        spannable.setSpan(
            ForegroundColorSpan(darkBlue),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// 你原本的放大設定，我看到你是想放大第一行，但你的字串沒有換行符號 \n，會得到 -1，建議改成放大整段或確認字串有換行符號
        spannable.setSpan(
            RelativeSizeSpan(1.0f),
            0,
            spannable.length,  // 改成整串長度，或換行符號位置
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        headerTextView.setText(spannable, TextView.BufferType.SPANNABLE)




        container = findViewById(R.id.questionContainer)
        submitButton = findViewById(R.id.btnStartSurvey)


        explanationCard = findViewById(R.id.cardViewExplanation2)
        explanationTextView = findViewById(R.id.textAnxietyExplanation)


        // 刷新表格顯示的邏輯
        resetSurvey()






        btnShowChart = findViewById(R.id.btnShowChart)
        lineChart = findViewById(R.id.lineChart)




        btnShowChart.setOnClickListener {
            showDatePickers()
        }












        submitButton.setOnClickListener {


            lineChart.visibility = View.GONE
            explanationCard.visibility = View.GONE
            explanationTextView.visibility = View.GONE
            findViewById<TextView>(R.id.chartTitle).visibility = View.GONE

            val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
            imageButton2.visibility = View.GONE
            val imageButton = findViewById<ImageButton>(R.id.headerImage)
            imageButton.visibility = View.GONE
            val headerText: TextView = findViewById(R.id.headerText)
            headerText.visibility = View.GONE
            val subHeaderText: TextView = findViewById(R.id.subHeaderText)
            subHeaderText.visibility = View.GONE


            val buttonGroup = findViewById<LinearLayout>(R.id.warningButtons)
            val warningContainer = findViewById<LinearLayout>(R.id.warningContainer)
            warningContainer.visibility = View.GONE
            buttonGroup.visibility = View.GONE


            val message = SpannableString(
                "此焦慮測量方式參考自 GAD-7（廣泛性焦慮量表），用於評估近「兩週」的焦慮程度。\n\nGAD-7問題來源 : https://www.listenerclinic.tw/?p=157"
            )
            Linkify.addLinks(message, Linkify.WEB_URLS)


            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("測量前提醒")
            builder.setMessage(message)
            builder.setPositiveButton("確定") { _, _ ->
                startSurvey()
            }
            builder.setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }


            val dialog = builder.create()
            dialog.show()


            // 一定要在 show() 後設定才有效
            dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
        }






















        val btnBackToThird = findViewById<Button>(R.id.btnBackToThird)
        btnBackToThird.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }




    private fun startSurvey() {
        Log.d("測試", "startSurvey 被觸發了")
        container.visibility = View.VISIBLE  // ← 加上這行讓表格顯示回來
        container.removeAllViews()
        scoreList.clear()




        // 修改 startSurvey() 方法中的問題選項部分
        questions.forEachIndexed { index, question ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16, 0, 16) }
                radius = 24f
                elevation = 8f
                setContentPadding(24, 24, 24, 24)
            }


            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }


            val tv = MaterialTextView(this).apply {
                text = question
                textSize = 18f
                setTextColor(resources.getColor(R.color.black, null))
            }


            // 這裡設置 RadioGroup 為垂直排列
            val radioGroup = RadioGroup(this).apply {
                orientation = RadioGroup.VERTICAL // 設置為垂直排列
            }


            val options = listOf("0", "1", "2", "3")
            val optionDescriptions = listOf(
                "完全沒有",
                "幾天",
                "一半以上的天數",
                "近乎每天"
            )


            options.forEachIndexed { i, option ->
                val rb = MaterialRadioButton(this).apply {
                    text = "$option - ${optionDescriptions[i]}"
                    id = index * 10 + i
                    textSize = 16f
                }
                radioGroup.addView(rb) // 每個選項會在新的行顯示
            }


            scoreList.add(radioGroup)
            innerLayout.addView(tv)
            innerLayout.addView(radioGroup)
            card.addView(innerLayout)
            container.addView(card)
        }




        val btnSubmit = MaterialButton(this).apply {
            text = "送出測驗"
            id = View.generateViewId()
            setOnClickListener { submitTestResults(container, this) }
        }




        container.addView(btnSubmit)
    }




    private fun submitTestResults(container: LinearLayout, submitButton: MaterialButton) {
        var totalScore = 0
        for (group in scoreList) {
            val checkedId = group.checkedRadioButtonId
            if (checkedId != -1) {
                val selected = findViewById<RadioButton>(checkedId)
                val score = selected.text.toString().split(" - ")[0].toInt()  // 只取數字部分
                totalScore += score
            } else {
                Toast.makeText(this, "請完成所有題目", Toast.LENGTH_SHORT).show()
                return
            }
        }


        // 隱藏測驗表格與送出按鈕
        container.visibility = View.GONE

        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.GONE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.GONE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.GONE

        val buttonGroup = findViewById<LinearLayout>(R.id.warningButtons)
        val warningContainer = findViewById<LinearLayout>(R.id.warningContainer)
        warningContainer.visibility = View.GONE
        buttonGroup.visibility = View.GONE



        // 傳送資料
        sendResultsToServer(totalScore)


        // 判斷文字
        val resultText = when (totalScore) {
            in 0..4 -> "極輕微焦慮（$totalScore 分）\n建議：持續觀察自己的情緒狀態，保持良好的生活習慣。\n\n焦慮指數計算說明資料：https://www.listenerclinic.tw/?p=157"
            in 5..9 -> "輕度焦慮（$totalScore 分）\n建議：嘗試自我調適方法，如運動、冥想，並考慮與親友分享感受。\n\n焦慮指數計算說明資料：https://www.listenerclinic.tw/?p=157"
            in 10..14 -> "中度焦慮（$totalScore 分）\n建議：建議尋求心理諮詢或專業協助。\n\n焦慮指數計算說明資料：https://www.listenerclinic.tw/?p=157"
            in 15..21 -> "重度焦慮（$totalScore 分）\n建議：請盡速尋求專業心理或精神科醫師的協助。\n\n焦慮指數計算說明資料：https://www.listenerclinic.tw/?p=157"
            else -> "請確認每題都已填寫"
        }


        // 將文字包成可加超連結的格式
        val spannableResult = SpannableString(resultText)
        Linkify.addLinks(spannableResult, Linkify.WEB_URLS)


        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("測驗結果")
            .setMessage(spannableResult)
            .setPositiveButton("了解") { dialog, _ ->
                dialog.dismiss()
            }
            .create()


        dialog.show()


        // 讓超連結可以被點擊
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }




    private fun sendResultsToServer(totalScore: Int) {
        val url = "https://test-9wne.onrender.com/submit-anxiety-score"

        // 取得 SharedPreferences 裡的 username
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", null)

        if (username == null) {
            Toast.makeText(this, "找不到使用者帳號，請重新登入", Toast.LENGTH_SHORT).show()
            return
        }

        // 取得今天日期
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 建立 JSON 物件
        val json = JSONObject().apply {
            put("username", username)
            put("measurementDate", today)
            put("score", totalScore)
            put("suggestion", getSuggestion(totalScore))
        }

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FourthActivity", "送出失敗：${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@FourthActivity, "資料送出失敗", Toast.LENGTH_SHORT).show()
                    container.visibility = View.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("FourthActivity", "伺服器回應：$responseBody")

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@FourthActivity, "資料送出成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@FourthActivity, "伺服器錯誤：${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }




    private fun getSuggestion(score: Int): String {
        return when (score) {
            in 0..4 -> "持續觀察自己的情緒狀態，保持良好的生活習慣。"
            in 5..9 -> "嘗試自我調適方法，如運動、冥想，並考慮與親友分享感受。"
            in 10..14 -> "建議尋求心理諮詢或專業協助。"
            in 15..21 -> "請盡速尋求專業心理或精神科醫師的協助。"
            else -> "請確認每題都已填寫"
        }
    }




    // 重置測量表格
    private fun resetSurvey() {
        container.visibility = View.VISIBLE
        submitButton.visibility = View.VISIBLE
    }




























    private fun showDatePickers() {
        val calendar = Calendar.getInstance()
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_prompt, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("開始選擇") { _, _ ->
                DatePickerDialog(this, { _, year, month, day ->
                    startDate = formatDate(year, month, day)

                    DatePickerDialog(this, { _, y2, m2, d2 ->
                        endDate = formatDate(y2, m2, d2)

                        if (startDate != null && endDate != null) {
                            // 可以加檢查結束日期不能早於開始日期
                            fetchChartData(startDate!!, endDate!!)
                        }
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()

                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }


    private fun formatDate(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(cal.time)
    }


    private fun fetchChartData(start: String, end: String) {
        val client = OkHttpClient()
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", null)

        if (username == null) {
            Toast.makeText(this, "使用者未登入，請重新登入", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://test-9wne.onrender.com/get-anxiety-scores?username=$username&startDate=$start&endDate=$end"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@FourthActivity, "連線失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    if (jsonData != null) {
                        val (entries, dateList) = parseChartData(jsonData)

                        val format = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
                        val dateScorePairs = dateList.zip(entries.map { it.y }).mapNotNull { (dateStr, score) ->
                            try {
                                val date = format.parse(dateStr)
                                if (date != null) Pair(date, score) else null
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedBy { it.first } // 確保資料依時間順序排序

                        if (dateScorePairs.isEmpty()) {
                            runOnUiThread {
                                showLineChart(entries, dateList)
                            }
                            return
                        }

                        val recentDate = dateScorePairs.maxByOrNull { it.first }!!.first

                        // 從所有比 recentDate 早的資料中往前找
                        var shouldShowWarning = false
                        for (i in dateScorePairs.indices) {
                            val startDate = dateScorePairs[i].first
                            val scoresInRange = dateScorePairs.filter { it.first in startDate..recentDate }

                            val duration = recentDate.time - startDate.time
                            val days = duration / (1000 * 60 * 60 * 24)

                            if (days >= 30 && scoresInRange.all { it.second >= 15f }) {
                                shouldShowWarning = true
                                break
                            }
                        }

                        Log.d("動畫判斷", "是否應該顯示動畫: $shouldShowWarning")

                        runOnUiThread {
                            if (shouldShowWarning) {
                                showWarningThenChart(entries, dateList)
                            } else {
                                showLineChart(entries, dateList)
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@FourthActivity, "伺服器回應空白", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@FourthActivity, "伺服器回應錯誤: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showWarningThenChart(entries: List<Entry>, dateList: List<String>) {
        val warningContainer = findViewById<LinearLayout>(R.id.warningContainer)
        val warningView = findViewById<ImageView>(R.id.gifImageView)
        val warningText = findViewById<TextView>(R.id.warningTextView)
        val warningCard = findViewById<MaterialCardView>(R.id.warningCard)

        val buttonGroup = findViewById<LinearLayout>(R.id.warningButtons)

        val explanationCard = findViewById<MaterialCardView>(R.id.cardViewExplanation2)
        val explanationTextView = findViewById<TextView>(R.id.textAnxietyExplanation)

        val chartTitle = findViewById<TextView>(R.id.chartTitle)
        val container = findViewById<LinearLayout>(R.id.questionContainer) // 例如這是問卷區塊容器
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        val headerText = findViewById<TextView>(R.id.headerText)
        val subHeaderText = findViewById<TextView>(R.id.subHeaderText)

        // 隱藏不需要顯示的
        listOf(
            explanationCard,
            explanationTextView,
            chartTitle,
            container,
            lineChart,
            imageButton2,
            imageButton,
            headerText,
            subHeaderText
        ).forEach { it.visibility = View.GONE }

        // 顯示警告容器和元件
        warningContainer.visibility = View.VISIBLE
        warningView.visibility = View.VISIBLE
        warningText.visibility = View.VISIBLE
        warningCard.visibility = View.VISIBLE

        // 顯示按鈕組
        buttonGroup.visibility = View.VISIBLE

        // 播放 GIF
        Glide.with(this).load(R.raw.ambulance).into(warningView)

        // 設定四個按鈕的點擊事件
        // 假設你在 buttonGroup 裡有四個 Button，id 分別是 btnVideo, btnDoctor, btnAdvice, btnContinue
        findViewById<Button>(R.id.btnVideo).setOnClickListener {
            val videoTitles = arrayOf("影片1：學習如何面對壓力\uD83D\uDCD6", "影片2：抒發壓力運動\uD83C\uDFC3\u200D♂\uFE0F", "影片3：紓壓音樂\uD83C\uDFA7")
            val videoUrls = arrayOf(
                "https://www.youtube.com/watch?v=ztScKTPtMA4&pp=ygUS57ep6Kej5aOT5Yqb5b2x54mH",
                "https://www.youtube.com/watch?v=808wIufw3h4&pp=ygUS57ep6Kej5aOT5Yqb6YGL5YuV",
                "https://www.youtube.com/watch?v=jd0L1Nn_U7U&list=RDjd0L1Nn_U7U&start_radio=1&pp=ygUS57ep6Kej5aOT5Yqb6Z-z5qiCoAcB"
            )

            AlertDialog.Builder(this)
                .setTitle("選擇想觀看的影片")
                .setItems(videoTitles) { dialog, which ->
                    // which 是被點選的索引
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrls[which]))
                    startActivity(intent)
                }
                .setNegativeButton("取消", null)
                .show()
        }



        findViewById<Button>(R.id.btnDoctor).setOnClickListener {
            // 可選的醫生專線
            val doctorOptions = arrayOf(
                "安心專線（1925）",
                "張老師專線（1980）"
            )
            val phoneNumbers = arrayOf(
                "1925",
                "1980"
            )

            // 顯示提示框
            AlertDialog.Builder(this)
                .setTitle("請選擇要撥打的專線")
                .setItems(doctorOptions) { _, which ->
                    val selectedNumber = phoneNumbers[which]
                    // 使用 Uri.encode 避免被誤格式化
                    val phoneIntent = Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + Uri.encode(selectedNumber))
                    )
                    startActivity(phoneIntent)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        findViewById<Button>(R.id.btnAdvice).setOnClickListener {
            val regionCityMap = mapOf(
                "北部" to listOf("台北市", "新北市", "基隆市", "桃園市"),
                "中部" to listOf("台中市", "苗栗縣", "彰化縣", "南投縣"),
                "南部" to listOf("台南市", "高雄市", "屏東縣", "嘉義縣", "嘉義市", "雲林縣"),
                "東部" to listOf("花蓮縣", "台東縣", "宜蘭縣")
            )

            val clinicMap = mapOf(
                "台北市" to mapOf(
                    "永康身心診所" to "https://www.psyclinic.com.tw/",
                    "台北榮民總醫院精神醫學部" to "https://www.vghtpe.gov.tw/",
                    "蘭心診所" to "https://lansinclinic.com/"
                ),
                "新北市" to mapOf(
                    "心悅身心診所" to "https://shinyue.com.tw/",
                    "康迎身心科" to "https://www.comein-wellbeing.com/",
                    "林口長庚紀念醫院精神科" to "https://www.cgmh.org.tw/department/chi/dept_22.html"
                ),
                "基隆市" to mapOf(
                    "基隆長庚紀念醫院精神科" to "https://www.cgmh.org.tw/hosp/cgmh/department/psych/",
                    "基隆心身心精神科診所" to "http://www.keelungheart.com.tw/",
                    "詠欣精神科診所" to "https://spirits.tw/Keelung/"
                ),
                "桃園市" to mapOf(
                    "正文身心診所" to "https://mental-clinic.com.tw/%E6%A1%83%E5%9C%92%E5%BF%83%E7%90%86%E8%AB%AE%E5%95%86/",
                    "林口長庚紀念醫院" to "https://www.cgmh.org.tw/",
                    "振心身心診所" to "https://www.inspireclinic.com.tw/"
                ),
                "台中市" to mapOf(
                    "康誠身心醫學科診所" to "https://www.mindfield.com.tw/",
                    "嘉樂身心診所" to "https://cheer-clinic.com/",
                    "心森林身心診所" to "https://www.mindforest.com.tw/"
                ),
                "苗栗縣" to mapOf(
                    "苗栗醫院" to "https://www.mlh.mohw.gov.tw/",
                    "天慈身心科診所" to "http://www.lushaowen.com.tw/"
                ),
                "彰化縣" to mapOf(
                    "好事彰化身心醫學診所" to "https://www.mainpi.com/query?i=3171",
                    "沐昀心理診所" to "https://www.daylight-psy.com/"
                ),
                "南投縣" to mapOf(
                    "南投醫院" to "https://www.nth.gov.tw/",
                    "惠良精神科診所" to "https://healthmental.com.tw/"
                ),
                "雲林縣" to mapOf(
                    "雲林斗六基督教醫院" to "https://www.dch.org.tw/",
                    "穗訸心理治療所" to "https://www.suihetherapy.com.tw/%E6%B2%BB%E7%99%82%E5%9C%98%E9%9A%8A/"
                ),
                "嘉義市" to mapOf(
                    "欣明心理成長中心" to "https://www.xin-ming.org/",
                    "吳南逸診所" to "https://drwu-clinic.com/"
                ),
                "嘉義縣" to mapOf(
                    "嘉義基督教醫院" to "https://www.chih.org.tw/"
                ),
                "台南市" to mapOf(
                    "心樂活診所" to "https://mind-yoho.com/",
                    "心悠活診所" to "https://mind-yoho.com/",
                    "真心身心診所" to "https://www.heartfeltclinic.com/"
                ),
                "高雄市" to mapOf(
                    "盼望心理治療所" to "https://www.livinghopetw.com/",
                    "文山身心診所" to "https://www.wensan632.com.tw/"
                ),
                "屏東縣" to mapOf(
                    "得立身心診所" to "https://www.phdkao.com.tw/"
                ),
                "宜蘭縣" to mapOf(
                    "五餅二魚身心科診所" to "https://hisworkclinic.com/"
                ),
                "花蓮縣" to mapOf(
                    "花蓮慈濟醫院" to "https://www.ttsh.org.tw/"
                ),
                "台東縣" to mapOf(
                    "台東馬偕醫院" to "https://www.mmh.org.tw/"
                )
            )

            val regionNames = regionCityMap.keys.toTypedArray()

            AlertDialog.Builder(this)
                .setTitle("請選擇地區")
                .setItems(regionNames) { _, regionIndex ->
                    val selectedRegion = regionNames[regionIndex]
                    val cityList = regionCityMap[selectedRegion] ?: return@setItems
                    val cityArray = cityList.toTypedArray()

                    AlertDialog.Builder(this)
                        .setTitle("請選擇縣市")
                        .setItems(cityArray) { _, cityIndex ->
                            val selectedCity = cityArray[cityIndex]
                            val clinics = clinicMap[selectedCity]

                            if (clinics != null && clinics.isNotEmpty()) {
                                val clinicNames = clinics.keys.toTypedArray()

                                AlertDialog.Builder(this)
                                    .setTitle("推薦診所")
                                    .setItems(clinicNames) { _, clinicIndex ->
                                        val clinicUrl = clinics[clinicNames[clinicIndex]]
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clinicUrl))
                                        startActivity(intent)
                                    }
                                    .show()
                            } else {
                                Toast.makeText(this, "目前尚無 $selectedCity 推薦診所資訊", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .show()
                }
                .show()
        }
        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            // 隱藏警告容器與按鈕，顯示圖表
            warningContainer.visibility = View.GONE
            buttonGroup.visibility = View.GONE
            showLineChart(entries, dateList)
        }
    }

    // 你可以另外寫一個顯示抒壓建議的對話框函式
    private fun showStressReliefAdviceDialog() {
        AlertDialog.Builder(this)
            .setTitle("抒壓建議")
            .setMessage("這裡放你的抒壓建議內容...")
            .setPositiveButton("知道了") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    private fun parseChartData(jsonData: String): Pair<List<Entry>, List<String>> {
        val entries = mutableListOf<Entry>()
        val dateList = mutableListOf<String>()
        val jsonArray = JSONArray(jsonData)


        if (jsonArray.length() == 0) {
            runOnUiThread {
                Toast.makeText(this, "此日期區間內沒有焦慮紀錄", Toast.LENGTH_SHORT).show()
            }
            return Pair(entries, dateList)
        }


        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        utcFormat.timeZone = TimeZone.getTimeZone("UTC") // 明確解析 UTC 時區


        val taiwanFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
        taiwanFormat.timeZone = TimeZone.getTimeZone("Asia/Taipei") // 轉成台灣時區格式


        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val rawDate = obj.getString("measurementDate")  // 從API取得的欄位名
            val score = obj.getInt("score")


            val formattedDate = try {
                val parsedDate = utcFormat.parse(rawDate)
                if (parsedDate != null) {
                    taiwanFormat.format(parsedDate)
                } else {
                    "未知日期"
                }
            } catch (e: Exception) {
                Log.e("日期解析錯誤", "解析失敗: $rawDate", e)
                "未知日期"
            }


            if (score > 0 && formattedDate != "未知日期") {
                entries.add(Entry(i.toFloat(), score.toFloat()))
                dateList.add(formattedDate)
            }
        }


        return Pair(entries, dateList)
    }


    private fun showLineChart(entries: List<Entry>, dateList: List<String>) {
        val dataSet = LineDataSet(entries, "焦慮指數")
        dataSet.color = resources.getColor(android.R.color.holo_blue_dark)
        dataSet.valueTextColor = resources.getColor(android.R.color.black)
        dataSet.lineWidth = 5f
        dataSet.valueTextSize = 18f


        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }


        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false


        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(true)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.labelRotationAngle = -45f
        xAxis.textSize = 12f


        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = (entries.size - 1).toFloat()


        val labelInterval = max(1, entries.size / 5)


        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in dateList.indices && index % labelInterval == 0) {
                    dateList[index]
                } else {
                    ""
                }
            }
        }


        lineChart.setExtraRightOffset(40f)


        val leftAxis = lineChart.axisLeft
        val maxY = entries.maxOfOrNull { it.y } ?: 0f
        val labelCount = 6
        leftAxis.setLabelCount(labelCount, true)
        leftAxis.axisMaximum = maxY + 4f
        leftAxis.textSize = 14f


        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val interval = (leftAxis.axisMaximum - leftAxis.axisMinimum) / (labelCount - 1)
                return if (abs(value - leftAxis.axisMaximum) < interval / 2) {
                    "(焦\n慮\n分\n數)"
                } else {
                    value.toInt().toString()
                }
            }
        }


        leftAxis.removeAllLimitLines()
        lineChart.axisRight.isEnabled = false
        lineChart.legend.textSize = 18f


        val markerView = DateMarkerView(this, R.layout.marker_date, dateList)
        markerView.chartView = lineChart
        lineChart.marker = markerView


        lineChart.invalidate()
        lineChart.visibility = View.VISIBLE
        Log.d("ChartData", "entries.size=${entries.size}")


        val explanationCard = findViewById<MaterialCardView>(R.id.cardViewExplanation2)
        val explanationTextView = findViewById<TextView>(R.id.textAnxietyExplanation)


        explanationCard.visibility = View.VISIBLE
        explanationTextView.visibility = View.VISIBLE
        findViewById<TextView>(R.id.chartTitle).visibility = View.VISIBLE
        container.visibility = View.GONE

        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.GONE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.GONE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.GONE

        val buttonGroup = findViewById<LinearLayout>(R.id.warningButtons)
        val warningContainer = findViewById<LinearLayout>(R.id.warningContainer)
        warningContainer.visibility = View.GONE
        buttonGroup.visibility = View.GONE


        val startText = startDate ?: "未知"
        val endText = endDate ?: "未知"


        explanationTextView.text = "此圖包括 $startText 到 $endText 的焦慮數據\n" +
                "0 至 4 分：極輕微焦慮\n" +
                "5 至 9 分：輕度焦慮\n" +
                "10 至 14 分：中度焦慮\n" +
                "15 至 21 分：重度焦慮\n\n" +
                "(橫坐標為日期，縱坐標為焦慮指數)"
    }


}

