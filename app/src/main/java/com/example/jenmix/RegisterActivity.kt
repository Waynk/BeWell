package com.example.jenmix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.api.AuthResponse
import com.example.jenmix.api.RetrofitClient
import com.example.jenmix.storage.AuthManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.media.MediaPlayer
import android.widget.RadioButton
import android.widget.RadioGroup

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUser     = findViewById<EditText>(R.id.etUsername)
        val etPass     = findViewById<EditText>(R.id.etPassword)
        val etName     = findViewById<EditText>(R.id.etDisplayName)
        val etAge      = findViewById<EditText>(R.id.etAge)
        val rgGender   = findViewById<RadioGroup>(R.id.rgGender)
        val rbMale     = findViewById<RadioButton>(R.id.rbMale)
        val rbFemale   = findViewById<RadioButton>(R.id.rbFemale)
        val etHeight   = findViewById<EditText>(R.id.etHeight)
        val etWeight   = findViewById<EditText>(R.id.etWeight)
        val btnRegister= findViewById<Button>(R.id.btnRegister)

        fun playClickSound() {
            val mediaPlayer = MediaPlayer.create(this, R.raw.click_sound)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        }

        btnRegister.setOnClickListener {
            playClickSound()  // 👉 點下去時播放叮咚～

            val u  = etUser.text.toString().trim()
            val p  = etPass.text.toString().trim()
            val dn = etName.text.toString().trim()
            val ageText = etAge.text.toString().trim()
            val heightText = etHeight.text.toString().trim()
            val weightText = etWeight.text.toString().trim()
            val gender = when {
                rbMale.isChecked -> "男"
                rbFemale.isChecked -> "女"
                else -> ""
            }

            if (u.isEmpty() || p.isEmpty() || dn.isEmpty()|| ageText.isEmpty() || gender.isEmpty() || heightText.isEmpty() || weightText.isEmpty()){
                Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = try {
                ageText.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "年齡格式錯誤", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val height = try {
                heightText.toFloat()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "身高格式錯誤", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weight = try {
                weightText.toFloat()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "體重格式錯誤", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitClient.instance.register(u, p, dn, age, gender, height, weight)
                .enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val token = response.body()!!.token
                            AuthManager.saveToken(this@RegisterActivity, token)

                            // 註冊成功 → 進入胡
                            startActivity(Intent(this@RegisterActivity,MainMenuActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, "註冊失敗", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        Toast.makeText(this@RegisterActivity, "網路錯誤", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}
