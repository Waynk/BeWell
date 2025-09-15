package com.example.jenmix.jen3

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.R
class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview3)

        val webView = findViewById<WebView>(R.id.webView)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        val url = intent.getStringExtra("url") ?: ""

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)

        btnBack.setOnClickListener {
            finish() // 回到上一頁
        }
    }
}

