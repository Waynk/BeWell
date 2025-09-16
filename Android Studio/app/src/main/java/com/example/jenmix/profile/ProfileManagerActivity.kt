package com.example.jenmix.profile

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.R
import com.example.jenmix.api.ApiService
import com.example.jenmix.api.RetrofitClient
import com.example.jenmix.model.SimpleResponse
import com.example.jenmix.model.UserInfoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileManagerActivity : AppCompatActivity() {

    // ───────── 畫面元件 ─────────
    private lateinit var tvUserInfo: TextView
    private lateinit var genderInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var btnUpdateInfo: Button

    private lateinit var oldPwdInput: EditText
    private lateinit var newPwdInput: EditText
    private lateinit var btnChangePassword: Button

    // 帳號來源：Intent → SharedPreferences（兩者取其一）
    private lateinit var username: String
    private val api: ApiService by lazy { RetrofitClient.instance }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_manager_screen)

        // ─── 取得帳號 ───
        username = intent.getStringExtra("username")
            ?: getSharedPreferences("user", MODE_PRIVATE).getString("username", "") ?: ""

        if (username.isBlank()) {
            Toast.makeText(this, "未提供帳號，無法載入個人資料", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ─── 綁定元件 ───
        tvUserInfo     = findViewById(R.id.tvUserInfo)
        genderInput    = findViewById(R.id.etGender)
        ageInput       = findViewById(R.id.etAge)
        heightInput    = findViewById(R.id.etHeight)
        btnUpdateInfo  = findViewById(R.id.btnUpdateInfo)

        oldPwdInput       = findViewById(R.id.etOldPassword)
        newPwdInput       = findViewById(R.id.etNewPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        // 讀取個人資料
        loadUserInfo()

        // ───────── 更新個人資料 ─────────
        btnUpdateInfo.setOnClickListener {
            val gender = genderInput.text.toString().trim()
            val age    = ageInput.text.toString().trim()
            val height = heightInput.text.toString().trim()

            if (gender.isBlank() && age.isBlank() && height.isBlank()) {
                Toast.makeText(this, "至少填寫一項要修改的欄位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.updateUserInfo(username, gender, age, height)
                .enqueue(simpleCallback("更新") {
                    // 重新載入使用者資料
                    loadUserInfo()
                    // ✅ 成功後清空輸入欄位
                    genderInput.text.clear()
                    ageInput.text.clear()
                    heightInput.text.clear()
                })
        }

        // ───────── 修改密碼 ─────────
        btnChangePassword.setOnClickListener {
            val oldPw = oldPwdInput.text.toString().trim()
            val newPw = newPwdInput.text.toString().trim()

            if (oldPw.isBlank() || newPw.isBlank()) {
                Toast.makeText(this, "請輸入目前密碼與新密碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPw.length < 4) {
                Toast.makeText(this, "新密碼至少 4 碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.changePassword(username, oldPw, newPw)
                .enqueue(simpleCallback("修改密碼") {
                    // 成功後清空欄位
                    oldPwdInput.text.clear()
                    newPwdInput.text.clear()
                })
        }
    }

    // ───────── 讀取個人資料 ─────────
    private fun loadUserInfo() {
        api.getUserInfo(username).enqueue(object : Callback<UserInfoResponse> {
            override fun onResponse(
                call: Call<UserInfoResponse>,
                response: Response<UserInfoResponse>
            ) {
                val data = response.body()
                if (response.isSuccessful && data != null) {
                    val info = """
                        🚻 性別：${data.gender ?: "—"}
                        🎂 年齡：${data.age ?: "—"}
                        📏 身高：${data.height ?: "—"} cm
                    """.trimIndent()
                    tvUserInfo.text = info
                } else {
                    tvUserInfo.text = "無法取得資料"
                }
            }

            override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                tvUserInfo.text = "取得失敗：${t.message}"
            }
        })
    }

    // ───────── 共用 Toast Callback 工具 ─────────
    private fun simpleCallback(actionName: String, onSuccess: () -> Unit)
            : Callback<SimpleResponse> =
        object : Callback<SimpleResponse> {
            override fun onResponse(
                call: Call<SimpleResponse>,
                response: Response<SimpleResponse>
            ) {
                val msg = response.body()?.message ?: response.body()?.error ?: "未預期錯誤"
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileManagerActivity, "✅ $actionName 成功", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(this@ProfileManagerActivity, "❌ $actionName 失敗：$msg", Toast.LENGTH_SHORT).show()
                }
                Log.d("Profile", "$actionName -> code:${response.code()} msg:$msg")
            }

            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                Toast.makeText(this@ProfileManagerActivity, "$actionName 失敗：${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("Profile", "$actionName error: ${t.message}")
            }
        }
}
