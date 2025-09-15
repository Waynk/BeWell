package com.example.jenmix.jen3

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class YouTubeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawLink = intent.getStringExtra("youtube_url") ?: run {
            Log.e("YouTubeActivity", "❌ 未提供有效 URL")
            finish()
            return
        }

        val link = cleanUrl(rawLink)
        Log.d("YouTubeActivity", "▶️ 準備跳轉網址：$link")

        // ✅ 放棄音訊焦點（避免搶聲音）
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(null)

        // ✅ 嘗試開啟 YouTube App 播放
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                `package` = "com.google.android.youtube"
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("YouTubeActivity", "📵 找不到 YouTube App，改用瀏覽器")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }

        // 5️⃣ 只關閉自己，還原到呼叫它的那個 Activity
        finish()
        // 關閉這個 Activity 時，不要做任何轉場動畫
        overridePendingTransition(0, 0)
    }

    // ✅ 修正 URL 格式
    private fun cleanUrl(url: String): String {
        return url
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF]"), "")
            .replace("http://https://", "https://")
            .replace("​https://", "https://")
            .replace("http://", "https://")
            .trim()
    }
}
