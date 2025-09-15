package com.example.jenmix.jen8

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.jenmix.storage.UserPrefs
import com.example.jenmix.R
import java.util.*

class FirstTimeSetupActivity : AppCompatActivity() {

    private lateinit var btnMale: Button
    private lateinit var btnFemale: Button
    private lateinit var etHeight: AutoCompleteTextView
    private lateinit var etAge: AutoCompleteTextView
    private lateinit var btnHeightVoice: ImageButton
    private lateinit var btnAgeVoice: ImageButton
    private lateinit var btnFinish: Button

    private var selectedGender: String = ""

    private val REQ_CODE_HEIGHT_VOICE = 1001
    private val REQ_CODE_AGE_VOICE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (UserPrefs.isSetupCompleted(this)) {
            startActivity(Intent(this, MainActivity8::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_first_time_setup)

        btnMale = findViewById(R.id.btnMale)
        btnFemale = findViewById(R.id.btnFemale)
        etHeight = findViewById(R.id.etHeight)
        etAge = findViewById(R.id.etAge)
        btnHeightVoice = findViewById(R.id.btnHeightVoice)
        btnAgeVoice = findViewById(R.id.btnAgeVoice)
        btnFinish = findViewById(R.id.btnFinish)

        btnMale.setOnClickListener {
            selectedGender = "男"
            btnMale.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_500))
            btnFemale.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light))
        }

        btnFemale.setOnClickListener {
            selectedGender = "女"
            btnFemale.setBackgroundColor(ContextCompat.getColor(this, R.color.pink_300))
            btnMale.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light))
        }

        val heights = (100..250).map { it.toString() }
        etHeight.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, heights))

        val ages = (10..100).map { it.toString() }
        etAge.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ages))

        etHeight.setOnClickListener { etHeight.showDropDown() }
        etHeight.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) etHeight.showDropDown() }
        etAge.setOnClickListener { etAge.showDropDown() }
        etAge.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) etAge.showDropDown() }

        btnHeightVoice.setOnClickListener { startVoiceInput(REQ_CODE_HEIGHT_VOICE) }
        btnAgeVoice.setOnClickListener { startVoiceInput(REQ_CODE_AGE_VOICE) }

        etHeight.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                etAge.requestFocus(); etAge.showDropDown(); true
            } else false
        }

        etAge.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(); validateAndSubmit(); true
            } else false
        }

        btnFinish.setOnClickListener { validateAndSubmit() }

        btnFinish.postDelayed({
            btnFinish.animate().scaleX(1.05f).scaleY(1.05f).setDuration(400).withEndAction {
                btnFinish.animate().scaleX(1f).scaleY(1f).duration = 400
            }
        }, 800)
    }

    private fun validateAndSubmit() {
        val heightStr = etHeight.text.toString().trim()
        val ageStr = etAge.text.toString().trim()

        // ✅ 檢查性別
        if (selectedGender.isEmpty()) {
            Toast.makeText(this, "⚠️ 請選擇您的性別", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 檢查身高
        if (heightStr.isEmpty()) {
            Toast.makeText(this, "⚠️ 請輸入您的身高", Toast.LENGTH_SHORT).show()
            return
        }

        val heightCm = try {
            heightStr.toFloat()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "⚠️ 請輸入有效的身高數字", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 檢查年齡
        if (ageStr.isEmpty()) {
            Toast.makeText(this, "⚠️ 請輸入您的年齡", Toast.LENGTH_SHORT).show()
            return
        }

        val age = try {
            ageStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "⚠️ 請輸入有效的年齡數字", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 通過所有驗證，顯示確認 Dialog
        showConfirmDialog(selectedGender, heightCm, age)
    }

    private fun showConfirmDialog(gender: String, height: Float, age: Int) {
        AlertDialog.Builder(this)
            .setTitle("確認資料")
            .setMessage("是否已確認您的基本資料填寫無誤？")
            .setPositiveButton("確認") { _, _ ->
                UserPrefs.saveUserInfo(this, gender, height, age)

                // ✅ 新增這行：標記該帳號已完成首次設定
                UserPrefs.setSetupCompleted(this, true)

                Toast.makeText(this, "資料已儲存", Toast.LENGTH_SHORT).show()

                // ✅ 導向主畫面
                startActivity(Intent(this, MainActivity8::class.java))
                finish()
            }

            .setNegativeButton("再次檢查", null)
            .show()
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun startVoiceInput(requestCode: Int) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出數字")
        try {
            startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            Toast.makeText(this, "語音輸入失敗：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.filter { it.isDigit() }
            if (result != null) {
                when (requestCode) {
                    REQ_CODE_HEIGHT_VOICE -> etHeight.setText(result)
                    REQ_CODE_AGE_VOICE -> etAge.setText(result)
                }
            }
        }
    }
}