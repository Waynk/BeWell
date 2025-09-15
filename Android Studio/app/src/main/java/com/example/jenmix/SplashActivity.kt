package com.example.jenmix

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var btnConfirm: Button
    private var clickPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 顯示提示畫面 UI
        setContentView(R.layout.activity_splash)

        // 綁定確認按鈕
        btnConfirm = findViewById(R.id.btnConfirm)

        // 強制每次都跳 LoginActivity（即使有 token 也不理會）
        btnConfirm.setOnClickListener {
            playClickSound() // 👉 點一下就「叮咚」
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    private fun playClickSound() {
        // 避免重複播放
        if (clickPlayer == null) {
            clickPlayer = MediaPlayer.create(this, R.raw.click_sound)
        }

        if (clickPlayer?.isPlaying == true) {
            clickPlayer?.seekTo(0)  // 若還在播就回到開頭重播一次
        } else {
            clickPlayer?.start()
        }

        // 播放完釋放資源避免記憶體洩漏
        clickPlayer?.setOnCompletionListener {
            it.reset()
            it.release()
            clickPlayer = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clickPlayer?.release()
        clickPlayer = null
    }
}
