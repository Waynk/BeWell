package com.example.jenmix.jen8

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.storage.UserPrefs
import com.example.jenmix.R

class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Debug 模式"
        setContentView(R.layout.activity_debug)

        val tvStatus = findViewById<TextView>(R.id.tvTTSStatus)
        val btnToggle = findViewById<Button>(R.id.btnToggleTTS)

        val isEnabled = UserPrefs.isTTSEnabled(this)
        tvStatus.text = if (isEnabled) "✅ 語音提示已啟用" else "❌ 語音提示已停用"

        btnToggle.setOnClickListener {
            val newStatus = !UserPrefs.isTTSEnabled(this)
            UserPrefs.setTTSEnabled(this, newStatus)
            tvStatus.text = if (newStatus) "✅ 語音提示已啟用" else "❌ 語音提示已停用"
            Toast.makeText(this, "狀態已更新", Toast.LENGTH_SHORT).show()
        }
    }
}
