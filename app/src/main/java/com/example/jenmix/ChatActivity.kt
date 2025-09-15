package com.example.jenmix

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.bumptech.glide.Glide

class ChatActivity : AppCompatActivity() {

    private lateinit var tvMessages: TextView
    private lateinit var etMessage: EditText
    private lateinit var scrollMessages: ScrollView
    private lateinit var btnMic: ImageButton


    private val client = OkHttpClient()
    private val nodeApiBaseUrl = "https://test-9wne.onrender.com" // 模擬器用 10.0.2.2，真機用本機 IP
    private val REQUEST_CODE_SPEECH_INPUT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        tvMessages = findViewById(R.id.tvMessages)
        etMessage = findViewById(R.id.etMessage)
        scrollMessages = findViewById(R.id.scrollMessages)
        btnMic = findViewById<ImageButton>(R.id.btnMic)

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val userInput = etMessage.text.toString().trim()
            if (userInput.isNotEmpty()) {
                appendMessage("👨🏽: $userInput")
                etMessage.setText("")
                sendToNodeServer(userInput)

                // 🚀 發送後隱藏歡迎語
                val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
                if (tvWelcome.visibility == View.VISIBLE) {
                    tvWelcome.visibility = View.GONE
                }
            }
        }

        Glide.with(this)
            .asGif()
            .load(R.raw.voice)   // 你的 GIF 放在 drawable/raw
            .into(btnMic)

        btnMic.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.TAIWAN)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出您的健康問題🐸")
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "你的裝置不支援語音輸入", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                etMessage.setText(result[0])
            }
        }
    }

    private fun appendMessage(message: String) {
        runOnUiThread {
            tvMessages.append("$message\n\n")
            scrollMessages.post { scrollMessages.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun sendToNodeServer(message: String) {
        val url = "$nodeApiBaseUrl/api/chat"

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", null)

        if (username == null) {
            appendMessage("⚠️ 無法取得使用者帳號，請先登入")
            Toast.makeText(this, "⚠️ 尚未登入", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject()
        json.put("message", message)
        json.put("username", username)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                appendMessage("❌ 無法連接伺服器：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val resJson = JSONObject(response.body?.string() ?: "")
                    val reply = resJson.optString("reply", "⚠️ 無回應內容")
                    appendMessage("🐸: $reply")
                } else {
                    appendMessage("❌ 錯誤：${response.message}")
                }
            }
        })
    }
}
