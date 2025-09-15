package com.example.jenmix.jen2

import com.example.jenmix.R
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainActivity2 : AppCompatActivity() {

    private lateinit var btnFetch: Button
    private lateinit var tvSelectedTime: TextView
    private lateinit var spinnerMedication: Spinner
    private lateinit var btnSetReminder: Button
    private lateinit var lvItems: ListView
    private lateinit var swToggleMedication: Switch
    private lateinit var swTextZoom: Switch
    private lateinit var reminderDao: MedicationReminderDao
    private lateinit var username: String

    // 從後端抓到的全部藥物
    private var allMedications: List<Medication> = emptyList()

    // 顯示在 ListView 的資料
    private val medicationItems = mutableListOf<ReminderItem>()   // 藥物清單
    private val customReminderItems = mutableListOf<ReminderItem>()// 已設定提醒

    private lateinit var reminderAdapter: ReminderAdapter

    private var selectedHour = -1
    private var selectedMinute = -1

    // 權限請求代碼
    private val VIBRATE_PERMISSION_REQUEST_CODE = 1001
    private val NOTIFICATIONS_PERMISSION_REQUEST_CODE = 1002

    // 與資料庫對應的藥物類型排序
    private val typeOrder = listOf(
        "高血壓藥","低血壓藥","高脈搏藥","低脈搏藥",
        "體重過高藥","體重過低藥","高肌力藥","低肌力藥"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        reminderDao = AppDatabase2.getDatabase(this).reminderDao()
        username = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("username", "") ?: ""

        btnFetch = findViewById(R.id.btnFetch)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        spinnerMedication = findViewById(R.id.spinnerMedication)
        btnSetReminder = findViewById(R.id.btnSetReminder)
        lvItems = findViewById(R.id.lvReminders)
        swToggleMedication = findViewById(R.id.swToggleMedication)

        swTextZoom = findViewById(R.id.swTextZoom)
        swTextZoom.setOnCheckedChangeListener { _, isChecked ->
            setAllTextSizes(findViewById(android.R.id.content), isChecked)
        }

        // ListView Adapter（jen2 專用）
        reminderAdapter = ReminderAdapter(this, customReminderItems.toMutableList()) { item ->
            cancelCustomReminder(item)
        }
        lvItems.adapter = reminderAdapter

        // 切換顯示藥物/提醒
        swToggleMedication.setOnCheckedChangeListener { _, _ -> updateDisplayedItems() }

        checkPermissions()
        checkExactAlarmPermission()
        ReminderUtils.createNotificationChannel(this)

        // 先抓藥物，提供 Spinner 選擇
        fetchAllMedications()

        // 選時間
        tvSelectedTime.setOnClickListener { showMaterialTimePicker() }

        // 設定提醒
        btnSetReminder.setOnClickListener {
            if (selectedHour == -1 || selectedMinute == -1) {
                Toast.makeText(this, "請先選擇提醒時間", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val selectedText = spinnerMedication.selectedItem?.toString() ?: ""
            if (selectedText.isNotEmpty() && selectedText.startsWith("【")) {
                Toast.makeText(this, "請選擇有效的藥物", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val med = allMedications.find { it.name == selectedText }
            if (med == null) {
                Toast.makeText(this, "找不到此藥物", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            setCustomReminder(med)
        }

        // 按「藥物讀取」把藥物清單放進 ListView（依 Switch 決定顯示哪個）
        btnFetch.setOnClickListener { loadMedicationItems() }
    }

    // 每次進入重新撈該帳號資料
    override fun onStart() {
        super.onStart()
        val currentUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("username", "") ?: ""
        if (currentUsername != username) username = currentUsername

        lifecycleScope.launch {
            val saved = reminderDao.getAllByUsername(username)
            customReminderItems.clear()
            for (r in saved) {
                customReminderItems.add(
                    ReminderItem(
                        med = Medication(
                            id = r.requestCode / 10000,
                            name = r.name,
                            type = r.type,
                            dosage = r.dosage,
                            ingredients = r.ingredients,
                            contraindications = r.contraindications,
                            side_effects = r.sideEffects,
                            source_url = r.sourceUrl
                        ),
                        reminderTime = r.reminderTime,
                        requestCode = r.requestCode
                    )
                )
            }
            updateDisplayedItems()
            reminderAdapter.notifyDataSetChanged()
        }
    }

    /** 抓藥物（只給 Spinner 用） */
    private fun fetchAllMedications() {
        RetrofitClient.apiService.getMedications().enqueue(object : Callback<List<Medication>> {
            override fun onResponse(call: Call<List<Medication>>, response: Response<List<Medication>>) {
                if (response.isSuccessful) {
                    val meds = response.body() ?: emptyList()
                    allMedications = meds
                    setupSpinner(meds)
                } else {
                    Toast.makeText(this@MainActivity2, "獲取失敗: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Medication>>, t: Throwable) {
                Toast.makeText(this@MainActivity2, "網路錯誤: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** 按下「藥物讀取」把 allMedications 填入 ListView 的資料源 */
    private fun loadMedicationItems() {
        medicationItems.clear()
        val groupedMap = allMedications.groupBy { it.type }
        typeOrder.forEach { typeName ->
            groupedMap[typeName]?.forEach { med ->
                medicationItems.add(ReminderItem(med = med, reminderTime = "", requestCode = -1))
            }
        }
        Toast.makeText(this, "藥物清單已載入", Toast.LENGTH_SHORT).show()
        updateDisplayedItems()
    }

    /** 建立分組 Spinner（維持原樣式） */
    private fun setupSpinner(meds: List<Medication>) {
        val groupedMap = meds.groupBy { it.type }
        val groupedItems = mutableListOf<String>()
        typeOrder.forEach { typeName ->
            if (groupedMap.containsKey(typeName)) {
                groupedItems.add("【$typeName】")
                groupedItems.addAll(groupedMap[typeName]!!.map { it.name })
            }
        }
        val adapter = GroupSpinnerAdapter(this, groupedItems)
        spinnerMedication.adapter = adapter
    }

    private fun resizeHourMinuteLabels(view: View) {
        if (view is TextView) {
            val text = view.text?.toString()?.trim()
            if (text == "Hour" || text == "Minute") {
                view.textSize = 13f
                view.setTypeface(null, Typeface.BOLD)
                view.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) resizeHourMinuteLabels(view.getChildAt(i))
        }
    }

    /** 顯示時間選擇器 */
    private fun showMaterialTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .setTitleText("選擇提醒時間")
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            tvSelectedTime.text = String.format("提醒時間：%02d:%02d", selectedHour, selectedMinute)
        }

        picker.show(supportFragmentManager, "tag_material_time_picker")

        Handler(Looper.getMainLooper()).postDelayed({
            val dialog = picker.dialog ?: return@postDelayed
            val window = dialog.window ?: return@postDelayed
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.55).toInt()
            )
            val root = window.decorView
            val titleView = findTextViewWithText(root, "選擇提醒時間")
            titleView?.apply { textSize = 16f; setTypeface(null, Typeface.BOLD) }
            resizeHourMinuteLabels(root)
        }, 150)
    }

    private fun setAllTextSizes(view: View, enlarge: Boolean) {
        if (view is TextView) {
            val tagKey = R.id.text_size_tag  // 請在 res/values/ids.xml 定義
            if (view.getTag(tagKey) == null) {
                val originalSizeSp = view.textSize / resources.displayMetrics.scaledDensity
                view.setTag(tagKey, originalSizeSp)
            }
            val baseSize = view.getTag(tagKey) as Float
            view.textSize = baseSize * if (enlarge) 1.3f else 1.0f
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) setAllTextSizes(view.getChildAt(i), enlarge)
        }
    }

    private fun findTextViewWithText(view: View, text: String): TextView? {
        if (view is TextView && view.text.toString().contains(text)) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findTextViewWithText(view.getChildAt(i), text)
                if (result != null) return result
            }
        }
        return null
    }

    /** 單筆立即設定提醒（手動選藥） */
    private fun setCustomReminder(med: Medication) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }
        val uniqueRequestCode = med.id * 10000 + selectedHour * 100 + selectedMinute
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("med_id", med.id)
            putExtra("med_name", med.name)
            putExtra("med_type", med.type)
            putExtra("med_dosage", med.dosage)
            putExtra("med_ingredients", med.ingredients)
            putExtra("med_contraindications", med.contraindications)
            putExtra("med_side_effects", med.side_effects)
            putExtra("med_source_url", med.source_url)
            putExtra("reminder_time", String.format("%02d:%02d", selectedHour, selectedMinute))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, uniqueRequestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
            Toast.makeText(this, "提醒已設定: ${med.name} @ ${calendar.time}", Toast.LENGTH_SHORT).show()
            customReminderItems.add(
                ReminderItem(med, String.format("%02d:%02d", selectedHour, selectedMinute), uniqueRequestCode)
            )
            updateDisplayedItems()
        } catch (se: SecurityException) {
            Log.e("MainActivity2", "無法設定精準鬧鐘: ${se.message}")
            Toast.makeText(this, "需要開啟精準鬧鐘權限才能設定提醒", Toast.LENGTH_SHORT).show()
        }

        // 同步寫入 Room
        lifecycleScope.launch {
            reminderDao.insert(
                MedicationReminderEntity(
                    requestCode = uniqueRequestCode,
                    name = med.name,
                    type = med.type,
                    dosage = med.dosage,
                    ingredients = med.ingredients,
                    contraindications = med.contraindications,
                    sideEffects = med.side_effects,
                    sourceUrl = med.source_url,
                    reminderTime = String.format("%02d:%02d", selectedHour, selectedMinute),
                    username = username
                )
            )
        }
    }

    /** 取消提醒 */
    private fun cancelCustomReminder(item: ReminderItem) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, item.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "提醒已刪除: ${item.med.name} @ ${item.reminderTime}", Toast.LENGTH_SHORT).show()
        customReminderItems.remove(item)
        updateDisplayedItems()

        lifecycleScope.launch { reminderDao.deleteByRequestCode(item.requestCode, username) }
    }

    /** 切換顯示資料來源 */
    private fun updateDisplayedItems() {
        if (swToggleMedication.isChecked) {
            reminderAdapter.setItems(medicationItems)
        } else {
            reminderAdapter.setItems(customReminderItems)
        }
    }

    // ───────────── 權限 ─────────────
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.VIBRATE), VIBRATE_PERMISSION_REQUEST_CODE
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATIONS_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            VIBRATE_PERMISSION_REQUEST_CODE ->
                Toast.makeText(
                    this,
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        "震動權限已授予" else "震動權限被拒絕",
                    Toast.LENGTH_SHORT
                ).show()
            NOTIFICATIONS_PERMISSION_REQUEST_CODE ->
                Toast.makeText(
                    this,
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        "通知權限已授予" else "通知權限被拒絕",
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    // ───────────── 這裡是補齊的工具/功能方法 ─────────────

    /** 今天 yyyy-MM-dd */
    private fun today(): String {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", y, m, d)
    }

    /** 儲存 AI 傳來的多天多時段提醒到 Room（逐筆） */
    private fun saveMedicationRemindersToRoom(
        title: String,
        notes: String,
        startDate: String,           // yyyy-MM-dd
        times: List<String>,         // ["08:00","21:00",...]
        durationDays: Int
    ) {
        val (y, m, d) = startDate.split("-").map { it.toInt() }
        val base = Calendar.getInstance().apply {
            set(Calendar.YEAR, y); set(Calendar.MONTH, m - 1); set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        lifecycleScope.launch {
            for (day in 0 until durationDays) {
                val cal = base.clone() as Calendar
                cal.add(Calendar.DAY_OF_MONTH, day)
                times.forEach { t ->
                    val (hh, mm) = hhmmToHourMinute(t)
                    val requestCode = buildRequestCode(day, hh, mm)
                    reminderDao.insert(
                        MedicationReminderEntity(
                            requestCode = requestCode,
                            name = title,
                            type = "",                 // 如需分類可自行填
                            dosage = notes,            // 先把 notes 放在 dosage，依你的 Entity 需求可自行調整
                            ingredients = "",
                            contraindications = "",
                            sideEffects = "",
                            sourceUrl = "",
                            reminderTime = t,
                            username = username
                        )
                    )
                }
            }
        }
    }

    /** 依開始日 × 天數 × 多時段 安排鬧鐘 */
    private fun scheduleMedicationAlarms(
        title: String,
        startDate: String,
        times: List<String>,
        durationDays: Int
    ) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val (y, m, d) = startDate.split("-").map { it.toInt() }
        val base = Calendar.getInstance().apply {
            set(Calendar.YEAR, y); set(Calendar.MONTH, m - 1); set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        for (day in 0 until durationDays) {
            times.forEach { t ->
                val (hh, mm) = hhmmToHourMinute(t)
                val cal = base.clone() as Calendar
                cal.add(Calendar.DAY_OF_MONTH, day)
                cal.set(Calendar.HOUR_OF_DAY, hh)
                cal.set(Calendar.MINUTE, mm)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (cal.before(Calendar.getInstance())) cal.add(Calendar.DAY_OF_MONTH, 1)

                val requestCode = buildRequestCode(day, hh, mm)
                val intent = Intent(this, ReminderReceiver::class.java).apply {
                    putExtra("med_id", requestCode / 10000)
                    putExtra("med_name", title)
                    putExtra("reminder_time", t)
                }
                val pi = PendingIntent.getBroadcast(
                    this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                } catch (se: SecurityException) {
                    Toast.makeText(this, "需要開啟精準鬧鐘權限", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** 重新載入 DB → 更新 ListView */
    private fun refreshMedicationListUI() {
        lifecycleScope.launch {
            val saved = reminderDao.getAllByUsername(username)
            customReminderItems.clear()
            for (r in saved) {
                customReminderItems.add(
                    ReminderItem(
                        med = Medication(
                            id = r.requestCode / 10000,
                            name = r.name,
                            type = r.type,
                            dosage = r.dosage,
                            ingredients = r.ingredients,
                            contraindications = r.contraindications,
                            side_effects = r.sideEffects,
                            source_url = r.sourceUrl
                        ),
                        reminderTime = r.reminderTime,
                        requestCode = r.requestCode
                    )
                )
            }
            updateDisplayedItems()
            reminderAdapter.notifyDataSetChanged()
        }
    }

    /** "HH:mm" -> (hour, minute) */
    private fun hhmmToHourMinute(hhmm: String): Pair<Int, Int> {
        val p = hhmm.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: 9
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }

    /** 產生唯一 requestCode（避免衝突） */
    private fun buildRequestCode(dayOffset: Int, hour: Int, minute: Int): Int {
        return dayOffset * 10000 + hour * 100 + minute
    }
}
