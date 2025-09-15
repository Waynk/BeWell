package com.example.jenmix.jen8

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.R
import com.example.jenmix.storage.UserPrefs

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "設定"
        setContentView(R.layout.activity_settings)

        val btnRestartGuide = findViewById<Button>(R.id.btnRestartGuide)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // 🔁 重新導覽
        btnRestartGuide.setOnClickListener {
            UserPrefs.resetGuideShown(this)  // 重設導覽偏好值
            val intent = Intent(this, MainActivity8::class.java)
            intent.putExtra("restart_guide", true)
            startActivity(intent)
            finish()
        }

        // ⬅️ 返回主畫面
        btnBack.setOnClickListener {
            finish()
        }
    }
}
