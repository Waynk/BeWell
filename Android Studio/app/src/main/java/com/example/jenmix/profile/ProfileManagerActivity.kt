package com.example.jenmix.profile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.R
import com.example.jenmix.api.RetrofitClient
import com.example.jenmix.api.ApiService
import com.example.jenmix.model.SimpleResponse
import com.example.jenmix.model.UserInfoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileManagerActivity : AppCompatActivity() {

    private lateinit var tvUserInfo: TextView
    private lateinit var genderInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var btnUpdateInfo: Button

    private lateinit var oldPwdInput: EditText
    private lateinit var newPwdInput: EditText
    private lateinit var btnChangePassword: Button

    private lateinit var username: String  // ✅ 不再寫死帳號

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_manager_screen)

        val api = RetrofitClient.instance

        // ✅ 從 Intent 接收帳號
        username = intent.getStringExtra("username") ?: ""

        if (username.isBlank()) {
            Toast.makeText(this, "未提供帳號，無法載入個人資料", Toast.LENGTH_LONG).show()
            finish() // 結束 Activity
            return
        }

        // 綁定元件
        tvUserInfo = findViewById(R.id.tvUserInfo)
        genderInput = findViewById(R.id.etGender)
        ageInput = findViewById(R.id.etAge)
        heightInput = findViewById(R.id.etHeight)
        btnUpdateInfo = findViewById(R.id.btnUpdateInfo)

        oldPwdInput = findViewById(R.id.etOldPassword)
        newPwdInput = findViewById(R.id.etNewPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        // 載入使用者資料
        loadUserInfo(api)

        // 更新資料按鈕
        btnUpdateInfo.setOnClickListener {
            val gender = genderInput.text.toString()
            val age = ageInput.text.toString()
            val height = heightInput.text.toString()

            api.updateUserInfo(username, gender, age, height)
                .enqueue(object : Callback<SimpleResponse> {
                    override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                        Toast.makeText(this@ProfileManagerActivity, response.body()?.message ?: "更新失敗", Toast.LENGTH_SHORT).show()
                        loadUserInfo(api)
                    }

                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                        Toast.makeText(this@ProfileManagerActivity, "更新失敗: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // 更改密碼按鈕
        btnChangePassword.setOnClickListener {
            val oldPwd = oldPwdInput.text.toString()
            val newPwd = newPwdInput.text.toString()

            api.changePassword(username, oldPwd, newPwd)
                .enqueue(object : Callback<SimpleResponse> {
                    override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                        Toast.makeText(this@ProfileManagerActivity, response.body()?.message ?: "更改失敗", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                        Toast.makeText(this@ProfileManagerActivity, "更改失敗: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun loadUserInfo(api: ApiService) {
        api.getUserInfo(username)
            .enqueue(object : Callback<UserInfoResponse> {
                override fun onResponse(call: Call<UserInfoResponse>, response: Response<UserInfoResponse>) {
                    val data = response.body()
                    if (data != null) {
                        val info = "性別：${data.gender}\n年齡：${data.age}\n身高：${data.height} cm"
                        tvUserInfo.text = info
                    } else {
                        tvUserInfo.text = "無法取得資料"
                    }
                }

                override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                    tvUserInfo.text = "取得失敗: ${t.message}"
                }
            })
    }
}
