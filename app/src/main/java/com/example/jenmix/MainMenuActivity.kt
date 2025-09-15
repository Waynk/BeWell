package com.example.jenmix

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.jenmix.hu.FourthActivity
import com.example.jenmix.hu.SecondActivity
import com.example.jenmix.jen1.MainActivity1
import com.example.jenmix.jen2.MainActivity2
import com.example.jenmix.jen3.MainActivity3
import com.example.jenmix.jen4.MainActivity4
import com.example.jenmix.jen5.MainActivity5
import com.example.jenmix.jen6.MainActivity6
import com.example.jenmix.jen7.MainActivity7
import com.example.jenmix.jen8.MainActivity8
import com.example.jenmix.jen9.MainActivity9
import com.example.jenmix.profile.ProfileManagerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException



class MainMenuActivity : AppCompatActivity() {

    private var keroPlayer: MediaPlayer? = null
    private val client = OkHttpClient()
    private val nodeApiBaseUrl = "https://test-9wne.onrender.com"

    private val REQ_VOICE = 9912
    private val REQ_RECORD_AUDIO = 44

    // 助理選單狀態（只控制展開/收合）
    private var isAssistantOpen = false

    // GIF 資源（請確保放在 res/raw/）
    private val gifFrog = R.raw.ai        // 右下主按鈕

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // ✅ 每日語錄
        val quoteText = findViewById<TextView>(R.id.quoteText)
        getDailyQuote(quoteText)

        // ✅ 左下個人資訊
        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            showCurrentUserDialogPretty()
        }

        // 個人資料管理
        findViewById<ImageButton>(R.id.btnProfileManager).setOnClickListener {
            val sp = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val username = sp.getString("username", "未知") ?: "未知"

            val intent = Intent(this, ProfileManagerActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }


        // ✅ 既有功能導航
        findViewById<ImageButton>(R.id.btnWeight).setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
        }
        findViewById<ImageButton>(R.id.btnAnxiety).setOnClickListener {
            startActivity(Intent(this, MainActivity3::class.java))
        }
        findViewById<ImageButton>(R.id.btnMedications).setOnClickListener {
            startActivity(Intent(this, MainActivity4::class.java))
        }
        findViewById<ImageButton>(R.id.btnSecondActivity).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnGoToAnalysis).setOnClickListener {
            startActivity(Intent(this, MainActivity1::class.java))
        }
        findViewById<ImageButton>(R.id.btnGoToExercise).setOnClickListener {
            startActivity(Intent(this, MainActivity6::class.java))
        }
        findViewById<ImageButton>(R.id.btnHelpNoticeActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity7::class.java))
        }
        findViewById<ImageButton>(R.id.btnhosActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
        }
        findViewById<ImageButton>(R.id.btnbmiActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity8::class.java))
        }
        findViewById<ImageButton>(R.id.btnAppointmentActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity9::class.java))
        }
        findViewById<ImageButton>(R.id.btnanxietyindex).setOnClickListener {
            startActivity(Intent(this, FourthActivity::class.java))
        }


        // 🐸 右下主按鈕（ImageButton）：載入 GIF、負責展開/收合選單
        val frogBtn = findViewById<ImageButton>(R.id.frogButton)
        Glide.with(this).asGif().load(gifFrog).into(frogBtn)

        // 🔽 助理選單（水平展開）；btnAiAutomation 一點就直接語音
        setupAssistantMenu(frogBtn)
    }

    /** ====== 右下角：一顆在左、一顆在上；GIF；上面那顆直接語音 ====== */
    private fun setupAssistantMenu(frogBtn: ImageButton) {
        // 新的兩顆按鈕
        val btnLeft = findViewById<ImageButton>(R.id.btnAiChat)
        val btnTop  = findViewById<ImageButton>(R.id.btnAiAutomation)

        // 載入 GIF 圖（左邊：聊天；上面：自動化/麥克風）
        Glide.with(this).asGif().load(R.raw.robot).into(btnLeft)
        Glide.with(this).asGif().load(R.raw.assistance).into(btnTop)

        // 青蛙：展開/收合 + 音效 + 震動
        frogBtn.setOnClickListener {
            playSoundAndVibrate(it)
            toggleCornerButtons(btnLeft, btnTop, expand = !isAssistantOpen)
        }

        // 左邊 → 進聊天頁
        btnLeft.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
            toggleCornerButtons(btnLeft, btnTop, expand = false)
        }

        // 上面 → 直接開語音面板
        btnTop.setOnClickListener {
            ensureRecordAudioAndStartVoice()
        }
    }

    /** 展開/收合動畫：左邊從右→左滑入；上面從下→上滑入 */
    private fun toggleCornerButtons(btnLeft: View, btnTop: View, expand: Boolean) {
        if (expand) {
            isAssistantOpen = true

            // 左邊
            btnLeft.apply {
                alpha = 0f
                translationX = 40f
                visibility = View.VISIBLE
                animate().alpha(1f).translationX(0f).setDuration(160).start()
            }
            // 上面
            btnTop.apply {
                alpha = 0f
                translationY = 40f
                visibility = View.VISIBLE
                animate().alpha(1f).translationY(0f).setDuration(160).start()
            }
        } else {
            isAssistantOpen = false

            btnLeft.animate().alpha(0f).translationX(40f).setDuration(140).withEndAction {
                btnLeft.visibility = View.GONE
            }.start()

            btnTop.animate().alpha(0f).translationY(40f).setDuration(140).withEndAction {
                btnTop.visibility = View.GONE
            }.start()
        }
    }

    /** ===== 權限 + 語音啟動 ===== */
    private fun ensureRecordAudioAndStartVoice() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startVoiceInput()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQ_RECORD_AUDIO
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput()
            } else {
                Toast.makeText(this, "需要麥克風權限才能語音輸入", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** ===== 音效 + 震動 + 小動畫 ===== */
    private fun playSoundAndVibrate(view: View) {
        try {
            if (keroPlayer == null) keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)
            if (keroPlayer?.isPlaying == true) return
            keroPlayer?.seekTo(0)
            keroPlayer?.start()
            keroPlayer?.setOnCompletionListener { it.reset(); it.release(); keroPlayer = null }
        } catch (_: Exception) {
            keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)
            keroPlayer?.start()
        }

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(80)
        }
        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    /** ===== 語音輸入 → /api/ai/automation → 依意圖跳轉 ===== */
    private fun startVoiceInput() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出要我幫你做什麼")
        }
        try {
            startActivityForResult(i, REQ_VOICE)
        } catch (e: Exception) {
            Toast.makeText(this, "此裝置不支援語音輸入", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VOICE && resultCode == Activity.RESULT_OK) {
            val list = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = list?.firstOrNull()?.trim()
            if (!text.isNullOrBlank()) {
                handleAiAutomation(text)
            }
        }
    }

    /** 🔗 呼叫後端 AI 自動化並依意圖導頁 */
    private fun handleAiAutomation(naturalText: String) {
        val sp = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sp.getString("username", null)
        val jwt = sp.getString("jwt", null) ?: sp.getString("token", null)

        if (username.isNullOrBlank()) {
            Toast.makeText(this, "尚未登入", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "$nodeApiBaseUrl/api/ai/automation"
        val body = JSONObject().apply {
            put("username", username)
            put("text", naturalText)
        }.toString()

        val req = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaType(), body))
            .apply { if (!jwt.isNullOrBlank()) header("Authorization", "Bearer $jwt") }
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainMenuActivity, "連線失敗：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainMenuActivity, "AI 解析失敗 (${response.code})", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val json = JSONObject(response.body?.string() ?: "{}")
                val ok = json.optBoolean("ok", false)
                val intentType = json.optString("intent", "")
                val speakback = json.optString("speakback", "")
                val payload = json.optJSONObject("payload") ?: JSONObject()

                runOnUiThread {
                    if (!ok || intentType.isBlank()) {
                        Toast.makeText(this@MainMenuActivity, "我不太確定要做什麼，請換句話說", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    when (intentType) {
                        "book_hospital"              -> gotoHospital(payload)
                        else -> Toast.makeText(this@MainMenuActivity, "目前不支援此操作", Toast.LENGTH_SHORT).show()
                    }
                    if (speakback.isNotBlank()) {
                        Toast.makeText(this@MainMenuActivity, speakback, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    /** ====== 各意圖跳轉 & 預填 ====== */
    private fun gotoHospital(p: JSONObject) {
        val hospital = p.optJSONObject("hospital")
        val i = Intent(this, MainActivity5::class.java).apply {
            putExtra("from_ai", true)
            putExtra("hospital_id", hospital?.optInt("hospital_id", -1) ?: -1)
            putExtra("department", hospital?.optString("department", "") ?: "")
            putExtra("doctor", hospital?.optString("doctor", "") ?: "")
            putExtra("date", p.optString("date", null))
            putExtra("time", p.optString("time", null))
            putExtra("region", hospital?.optString("region", null)) // ⬅ 讓 MainActivity5 自動切地區
        }
        startActivity(i)
    }

    /** ===== 每日語錄 ===== */
    private fun getDailyQuote(quoteText: TextView) {
        val url = "$nodeApiBaseUrl/api/daily-quote"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w("DailyQuote", "語錄載入失敗: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return
                val json = JSONObject(response.body?.string() ?: "")
                val quote = json.optString("quote", "🌿 今天沒有語錄喔～")
                runOnUiThread { quoteText.text = quote }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        try { keroPlayer?.release(); keroPlayer = null } catch (_: Exception) {}
    }

    /** ===== JSONArray -> List<String> 小工具 ===== */
    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            val v = optString(i, "").trim()
            if (v.isNotEmpty()) list.add(v)
        }
        return list
    }

    /** ===== 使用者資訊 Dialog（美化版） ===== */
    private fun showCurrentUserDialogPretty() {
        val sp = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sp.getString("username", "未知") ?: "未知"

        val view = layoutInflater.inflate(R.layout.dialog_current_user, null)
        view.findViewById<TextView>(R.id.tvUsername).text = username
        view.findViewById<TextView>(R.id.tvUserHint).text = "已登入"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<MaterialButton>(R.id.btnSwitch).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        view.findViewById<MaterialButton>(R.id.btnOk).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
