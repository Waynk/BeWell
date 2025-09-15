package com.example.jenmix.jen4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.jenmix.R
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity4 : AppCompatActivity() {

    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1
    private val reminderList = mutableListOf<Reminder>()
    private val filteredList = mutableListOf<Reminder>()
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var spinnerCategory: Spinner
    private var selectedCategory: String = Category.ALL.value
    private lateinit var reminderDao: GeneralReminderDao
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)

        // 通知權限
        requestNotificationPermission()
        reminderDao = AppDatabase4.getDatabase(this).generalReminderDao()

        username = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("username", "") ?: ""

        // UI
        val btnAddReminder: Button = findViewById(R.id.btnAddReminder)
        val rvReminders: RecyclerView = findViewById(R.id.rvReminders)
        spinnerCategory = findViewById(R.id.spinnerCategory)

        // 載入本地資料
        lifecycleScope.launch {
            val saved = reminderDao.getAllByUsername(username)
            reminderList.addAll(saved.map {
                Reminder(
                    id = it.id,
                    hour = it.hour,
                    minute = it.minute,
                    category = it.category,
                    dayOfWeek = it.dayOfWeek,
                    title = it.title,
                    content = it.content,
                    isRepeat = it.isRepeat
                )
            })
            filterReminders()
        }

        // RecyclerView
        reminderAdapter = ReminderAdapter(
            reminders = filteredList,
            onReminderDelete = { reminder: Reminder -> deleteReminder(reminder) },
            onReminderTimeChanged = { reminder: Reminder -> updateReminderTime(reminder) },
            onReminderEdit = { reminder: Reminder -> showAddReminderDialog(reminder) }
        )
        rvReminders.layoutManager = LinearLayoutManager(this)
        rvReminders.adapter = reminderAdapter

        // 類別篩選
        val categories = Category.values().map { it.value }
        val spinnerAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = spinnerAdapter
        spinnerCategory.setSelection(0)
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectedCategory = categories[position]
                filterReminders()
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                selectedCategory = Category.ALL.value
                filterReminders()
            }
        }

        // 新增
        btnAddReminder.setOnClickListener { showAddReminderDialog() }
    }

    // 取今天 yyyy-MM-dd
    private fun today(): String =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)

    // 通知權限（Android 13+）
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    /** 新增/修改對話框 */
    private fun showAddReminderDialog(reminder: Reminder? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder4, null)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val spinnerCategoryDialog =
            dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerCategory)
        val spinnerDayOfWeek =
            dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerDayOfWeek)

        val categories = arrayOf(
            Category.BLOOD_PRESSURE.value,
            Category.WEIGHT.value,
            Category.WATER.value,
            Category.OTHER.value
        )
        spinnerCategoryDialog.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        )

        val days = arrayOf("每天", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
        spinnerDayOfWeek.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, days))

        reminder?.let {
            timePicker.hour = it.hour
            timePicker.minute = it.minute
            spinnerCategoryDialog.setText(it.category, false)
            val daySelection = when (it.dayOfWeek) {
                null, -1 -> 0
                0 -> 1
                1 -> 2
                2 -> 3
                3 -> 4
                4 -> 5
                5 -> 6
                6 -> 7
                else -> 0
            }
            spinnerDayOfWeek.setText(days[daySelection], false)
        }

        AlertDialog.Builder(this)
            .setTitle(if (reminder == null) "新增提醒" else "修改提醒")
            .setView(dialogView)
            .setPositiveButton("確定") { _, _ ->
                val hour = timePicker.hour
                val minute = timePicker.minute
                val category = spinnerCategoryDialog.text.toString()
                val dayOfWeek = when (spinnerDayOfWeek.text.toString()) {
                    "每天" -> null
                    "周日" -> 0
                    "周一" -> 1
                    "周二" -> 2
                    "周三" -> 3
                    "周四" -> 4
                    "周五" -> 5
                    "周六" -> 6
                    else -> null
                }

                val titleText = "提醒時間到了"
                val contentText = "請打開APP"

                if (reminder == null) {
                    val newReminder = Reminder(
                        id = System.currentTimeMillis().toInt(),
                        hour = hour,
                        minute = minute,
                        category = category,
                        dayOfWeek = dayOfWeek,
                        title = titleText,
                        content = contentText,
                        isRepeat = false
                    )
                    reminderList.add(newReminder)
                    scheduleReminder(newReminder)

                    lifecycleScope.launch {
                        reminderDao.insert(
                            GeneralReminderEntity(
                                id = newReminder.id,
                                hour = newReminder.hour,
                                minute = newReminder.minute,
                                category = newReminder.category,
                                dayOfWeek = newReminder.dayOfWeek,
                                title = newReminder.title,
                                content = newReminder.content,
                                isRepeat = newReminder.isRepeat,
                                username = username
                            )
                        )
                    }
                    Toast.makeText(this, "✅ 提醒已新增", Toast.LENGTH_SHORT).show()
                } else {
                    val updatedReminder = reminder.copy(
                        hour = hour,
                        minute = minute,
                        category = category,
                        dayOfWeek = dayOfWeek,
                        title = titleText,
                        content = contentText
                    )
                    val index = reminderList.indexOfFirst { it.id == reminder.id }
                    if (index != -1) reminderList[index] = updatedReminder

                    lifecycleScope.launch {
                        reminderDao.insert(
                            GeneralReminderEntity(
                                id = updatedReminder.id,
                                hour = updatedReminder.hour,
                                minute = updatedReminder.minute,
                                category = updatedReminder.category,
                                dayOfWeek = updatedReminder.dayOfWeek,
                                title = updatedReminder.title,
                                content = updatedReminder.content,
                                isRepeat = updatedReminder.isRepeat,
                                username = username
                            )
                        )
                    }
                    scheduleReminder(updatedReminder)
                    Toast.makeText(this, "✏️ 提醒已更新", Toast.LENGTH_SHORT).show()
                }
                filterReminders()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 用 WorkManager 安排提醒 */
    private fun scheduleReminder(reminder: Reminder) {
        val nextTriggerTime = ReminderScheduler.calculateNextTriggerTime(reminder)
        val initialDelay = nextTriggerTime - System.currentTimeMillis()
        if (initialDelay <= 0) return

        val workData = workDataOf(
            "id" to reminder.id,
            "hour" to reminder.hour,
            "minute" to reminder.minute,
            "category" to reminder.category,
            "isRepeat" to reminder.isRepeat,
            "dayOfWeek" to (reminder.dayOfWeek ?: -1)
        )

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workData)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        val uniqueWorkName = "reminder_${reminder.id}"
        WorkManager.getInstance(this).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun deleteReminder(reminder: Reminder) {
        reminderList.removeAll { it.id == reminder.id }
        filterReminders()
        Toast.makeText(this, "🗑️ 提醒已刪除", Toast.LENGTH_SHORT).show()
        val uniqueWorkName = "reminder_${reminder.id}"
        WorkManager.getInstance(this).cancelUniqueWork(uniqueWorkName)

        lifecycleScope.launch { reminderDao.deleteByIdAndUsername(reminder.id, username) }
    }

    private fun updateReminderTime(reminder: Reminder) {
        scheduleReminder(reminder)
        filterReminders()
    }

    private fun filterReminders() {
        filteredList.clear()
        filteredList.addAll(
            if (selectedCategory == Category.ALL.value) reminderList
            else reminderList.filter { it.category == selectedCategory }
        )
        reminderAdapter.notifyDataSetChanged()
    }

    // 不含吃藥
    enum class Category(val value: String) {
        ALL("全部"),
        BLOOD_PRESSURE("測量血壓"),
        WEIGHT("測量體重"),
        WATER("喝水"),
        OTHER("其他")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知權限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "通知權限被拒絕", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
