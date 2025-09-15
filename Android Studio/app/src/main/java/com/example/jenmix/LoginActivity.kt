package com.example.jenmix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.api.AuthResponse
import com.example.jenmix.api.RetrofitClient
import com.example.jenmix.storage.AuthManager
import com.example.jenmix.storage.UserPrefs // ✅ 加入 UserPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.media.MediaPlayer
import android.view.animation.AnimationUtils

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnToRegister = findViewById<Button>(R.id.btnToRegister)

        val keroImage = findViewById<ImageView>(R.id.imgkero)
        val animation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_fade)
        keroImage.startAnimation(animation)

        // 播放音效函式
        fun playClickSound() {
            val mediaPlayer = MediaPlayer.create(this, R.raw.click_sound)
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
            mediaPlayer.start()
        }

        btnLogin.setOnClickListener {
            playClickSound()

            val u = etUser.text.toString().trim()
            val p = etPass.text.toString().trim()
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "請輸入帳號與密碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitClient.instance
                .login(u, p)
                .enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, resp: Response<AuthResponse>) {
                        if (resp.isSuccessful && resp.body() != null) {
                            val response = resp.body()!!
                            AuthManager.saveToken(this@LoginActivity, response.token)
                            UserPrefs.saveUsername(this@LoginActivity, u)
                            UserPrefs.saveDisplayName(this@LoginActivity, response.display_name) // 👈 加這行

                            // （舊寫法留著也沒關係）
                            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            sharedPref.edit().putString("username", u).apply()

                            startActivity(Intent(this@LoginActivity, MainMenuActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "登入失敗", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "網路錯誤", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        btnToRegister.setOnClickListener {
            playClickSound()
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
