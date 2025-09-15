package com.example.jenmix.jen6

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.jenmix.ChatActivity
import com.example.jenmix.R
import com.example.jenmix.jen1.WebViewActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class MainActivity6 : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private var keroPlayer: MediaPlayer? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_advice)

        val conditionMap = mapOf(
            R.id.cbHypertension to "高血壓",
            R.id.cbLowBloodPressure to "低血壓",
            R.id.cbOverweight to "體重過重",
            R.id.cbUnderweight to "體重過輕",
            R.id.cbHighPulse to "脈搏太高",
            R.id.cbLowPulse to "脈搏太低"
        )

        val checkBoxes = conditionMap.mapNotNull {
            val cb = findViewById<CheckBox>(it.key)
            cb?.let { checkBox -> checkBox to it.value }
        }

        val btnGetAdvice = findViewById<Button>(R.id.btnGetExercise)
        tvResult = findViewById(R.id.tvExerciseResult)

        btnGetAdvice.setOnClickListener {
            val selectedConditions = checkBoxes
                .filter { it.first.isChecked }
                .map { it.second }

            if (selectedConditions.isEmpty()) {
                Toast.makeText(this, "請選擇至少一項病症", Toast.LENGTH_SHORT).show()
            } else {
                fetchAdviceFromServer(selectedConditions)
            }
        }

        keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)

        findViewById<ImageButton>(R.id.frogButton).setOnClickListener {
            try {
                if (keroPlayer == null) {
                    keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)
                }

                if (keroPlayer?.isPlaying == true) return@setOnClickListener

                keroPlayer?.seekTo(0)
                keroPlayer?.start()

                keroPlayer?.setOnCompletionListener {
                    it.reset()
                    it.release()
                    keroPlayer = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)
                keroPlayer?.start()
            }

            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(80)
            }

            it.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).withEndAction {
                it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()

            AlertDialog.Builder(this)
                .setTitle("嗨👋，我是您的AI健康助手Kero🐸")
                .setMessage("您想問我什麼健康問題呢？")
                .setPositiveButton("進入詢問") { _, _ ->
                    startActivity(Intent(this, ChatActivity::class.java))
                }
                .setNegativeButton("先不用", null)
                .show()
        }
    }

    private fun fetchAdviceFromServer(conditions: List<String>) {
        val encodedConditions = URLEncoder.encode(conditions.joinToString(","), StandardCharsets.UTF_8.name())
        val url = "https://test-9wne.onrender.com/get_exercises?conditions=$encodedConditions"

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tvResult.text = "❌ 連線錯誤：${e.localizedMessage}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    runOnUiThread { tvResult.text = "❌ 無回應資料" }
                    return
                }

                try {
                    val json = JSONObject(body)
                    val resultHtml = when (val exercise = json.opt("exercise")) {
                        is JSONObject -> {
                            val builder = StringBuilder()
                            val keys = exercise.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val obj = exercise.optJSONObject(key)
                                if (obj != null) {
                                    builder.append("【$key】<br>")
                                    builder.append("${obj.optString("exercise")}<br>")
                                    val source = obj.optString("url", "")
                                    if (source.isNotEmpty()) {
                                        builder.append("🔗 <a href=\"$source\">點我查看資料來源</a><br><br>")
                                    }
                                }
                            }
                            builder.toString()
                        }
                        is String -> {
                            val source = json.optString("url", "")
                            if (source.isNotEmpty()) {
                                "$exercise<br><br>🔗 <a href=\"$source\">點我查看資料來源</a>"
                            } else {
                                exercise
                            }
                        }
                        else -> "❌ 回傳格式錯誤"
                    }

                    runOnUiThread {
                        val spannedText = HtmlCompat.fromHtml(resultHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        tvResult.text = spannedText
                        tvResult.movementMethod = LinkMovementMethod.getInstance()
                        makeLinksClickable(tvResult)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvResult.text = "❌ JSON 資料解析錯誤：${e.localizedMessage}"
                    }
                }
            }
        })
    }

    private fun makeLinksClickable(textView: TextView) {
        val text = textView.text
        if (text is Spannable) {
            val spans = text.getSpans(0, text.length, URLSpan::class.java)
            for (span in spans) {
                makeLinkClickable(text, span)
            }
            textView.text = text
        }
    }

    private fun makeLinkClickable(spannable: Spannable, span: URLSpan) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val flags = spannable.getSpanFlags(span)
        spannable.removeSpan(span)
        val clickable = object : URLSpan(span.url) {
            override fun onClick(widget: View) {
                val context = widget.context
                val intent = Intent(context, WebViewActivity::class.java)
                intent.putExtra("url", span.url)
                context.startActivity(intent)
            }
        }
        spannable.setSpan(clickable, start, end, flags)
    }
}
