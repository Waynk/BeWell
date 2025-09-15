package com.example.jenmix.jen1

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.R
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity1 : AppCompatActivity() {

    private lateinit var btnTodayWeight: MaterialButton
    private lateinit var btnRangeWeight: MaterialButton
    private lateinit var btnTodayBP: MaterialButton
    private lateinit var btnRangeBP: MaterialButton
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var btnChart: MaterialButton
    private lateinit var resultView: TextView

    private var analysisMode: String? = null
    private var selectedDate: String? = null
    private var startDate: String? = null
    private var endDate: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val apiService = RetrofitClient.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_analysis)

        val username = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", null)
        if (username == null) {
            Toast.makeText(this, "未登入，請重新登入", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnTodayWeight = findViewById(R.id.btn_today_weight)
        btnRangeWeight = findViewById(R.id.btn_range_weight)
        btnTodayBP = findViewById(R.id.btn_today_bp)
        btnRangeBP = findViewById(R.id.btn_range_bp)
        btnAnalyze = findViewById(R.id.analyze_btn)
        btnChart = findViewById(R.id.chart_btn)
        resultView = findViewById(R.id.result_view)

        btnTodayWeight.setOnClickListener {
            selectedDate = dateFormat.format(Date())
            analysisMode = "single_weight"
            Toast.makeText(this, "已選擇今日體重", Toast.LENGTH_SHORT).show()
        }

        btnTodayBP.setOnClickListener {
            selectedDate = dateFormat.format(Date())
            analysisMode = "single_bp"
            Toast.makeText(this, "已選擇今日血壓", Toast.LENGTH_SHORT).show()
        }

        btnRangeWeight.setOnClickListener {
            showCustomRangeDialog("range_weight")
        }

        btnRangeBP.setOnClickListener {
            showCustomRangeDialog("range_bp")
        }

        btnAnalyze.setOnClickListener {
            when (analysisMode) {
                "single_weight" -> analyzeSingleWeight(username)
                "single_bp" -> analyzeSingleBP(username)
                "range_weight" -> analyzeRangeWeight(username)
                "range_bp" -> analyzeRangeBP(username)
                else -> Toast.makeText(this, "請先選擇分析模式", Toast.LENGTH_SHORT).show()
            }
        }

        btnChart.setOnClickListener {
            val username = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", null)
            if (username != null) {
                val intent = Intent(this, ChartActivity::class.java)
                intent.putExtra("username", username)
                startActivity(intent)
            } else {
                Toast.makeText(this, "使用者未登入", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun showCustomRangeDialog(mode: String) {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_date_range, null)
        dialog.setContentView(view)

        val btnStart = view.findViewById<Button>(R.id.btnStartDate)
        val btnEnd = view.findViewById<Button>(R.id.btnEndDate)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

        val cal = Calendar.getInstance()

        btnStart.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                startDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                btnStart.text = "開始：$startDate"
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        btnEnd.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                endDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                btnEnd.text = "結束：$endDate"
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        btnConfirm.setOnClickListener {
            if (startDate != null && endDate != null) {
                analysisMode = mode
                Toast.makeText(this, "已選擇區間：$startDate 到 $endDate", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "請選擇完整的日期區間", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun analyzeSingleWeight(username: String) {
        val date = selectedDate ?: return
        apiService.analyzeSingleWeight(username, date).enqueue(object : Callback<AnalysisResult> {
            override fun onResponse(call: Call<AnalysisResult>, response: Response<AnalysisResult>) {
                resultView.text = response.body()?.analysis ?: "❌ 無法取得分析結果"
            }

            override fun onFailure(call: Call<AnalysisResult>, t: Throwable) {
                resultView.text = "❌ 分析失敗：${t.message}"
            }
        })
    }

    private fun analyzeSingleBP(username: String) {
        val date = selectedDate ?: return
        apiService.analyzeSingleBP(username, date).enqueue(object : Callback<AnalysisResult> {
            override fun onResponse(call: Call<AnalysisResult>, response: Response<AnalysisResult>) {
                resultView.text = response.body()?.analysis ?: "❌ 無法取得分析結果"
            }

            override fun onFailure(call: Call<AnalysisResult>, t: Throwable) {
                resultView.text = "❌ 分析失敗：${t.message}"
            }
        })
    }

    private fun analyzeRangeWeight(username: String) {
        if (startDate == null || endDate == null) return
        apiService.analyzeRangeWeight(username, startDate!!, endDate!!).enqueue(object : Callback<AnalysisResult> {
            override fun onResponse(call: Call<AnalysisResult>, response: Response<AnalysisResult>) {
                resultView.text = response.body()?.analysis ?: "❌ 區間體重分析失敗"
            }

            override fun onFailure(call: Call<AnalysisResult>, t: Throwable) {
                resultView.text = "❌ 分析失敗：${t.message}"
            }
        })
    }

    private fun analyzeRangeBP(username: String) {
        if (startDate == null || endDate == null) return
        apiService.analyzeRangeBP(username, startDate!!, endDate!!).enqueue(object : Callback<AnalysisResult> {
            override fun onResponse(call: Call<AnalysisResult>, response: Response<AnalysisResult>) {
                resultView.text = response.body()?.analysis ?: "❌ 區間血壓分析失敗"
            }

            override fun onFailure(call: Call<AnalysisResult>, t: Throwable) {
                resultView.text = "❌ 分析失敗：${t.message}"
            }
        })
    }
}
