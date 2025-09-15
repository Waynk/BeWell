package com.example.jenmix.jen2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // 先檢查 context 與 intent 是否為 null
        if (context == null || intent == null) {
            Log.e("ReminderReceiver", "Context 或 Intent 為 null")
            return
        }

        // 取得 PowerManager 並建立 WakeLock，讓螢幕亮起 3 秒
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            // 使用 FULL_WAKE_LOCK、ACQUIRE_CAUSES_WAKEUP、ON_AFTER_RELEASE 能喚醒螢幕
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "MyApp::ReminderWakeLock"
        )
        wakeLock.acquire(3000)
        Log.d("ReminderReceiver", "WakeLock acquired for 3 seconds to turn screen on.")

        // 取得藥物名稱，若不存在則預設為「未知藥物」
        val medName = intent.getStringExtra("med_name") ?: "未知藥物"

        // 顯示 Toast 提醒
        val toastMessage = "提醒：$medName 該吃藥了！"
        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()

        // 播放通知音效與觸發震動（在 ReminderUtils 中處理）
        ReminderUtils.playSoundFeedback(context)
        ReminderUtils.triggerVibration(context)

        // 顯示通知 (標題為藥物名稱，內容則為「該吃藥了！」)
        val reminder = Reminder(
            title = medName,
            content = "$medName 該吃藥了！"
        )
        ReminderUtils.showNotification(context, reminder)

        // 重新排程下一次提醒（24 小時後同一時間）
        val reminderTime = intent.getStringExtra("reminder_time") // 格式 "HH:mm"
        if (!reminderTime.isNullOrEmpty()) {
            val parts = reminderTime.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0

                // 計算下一次提醒時間：從現在開始加 1 天，並設置指定的時分
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // 取得藥物 ID，並根據其生成唯一的 requestCode
                val medId = intent.getIntExtra("med_id", -1)
                val uniqueRequestCode = medId * 10000 + hour * 100 + minute

                // 建立新的 Intent，保留原先所有 extras
                val newIntent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtras(intent)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    uniqueRequestCode,
                    newIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // 檢查是否有權限排程精準鬧鐘
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Please grant exact alarm permission", Toast.LENGTH_SHORT).show()
                    val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(settingsIntent)
                } else {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d("ReminderReceiver", "Next reminder scheduled for: ${calendar.time}")
                    } catch (e: SecurityException) {
                        Log.e("ReminderReceiver", "Exact alarm scheduling failed: ${e.message}")
                    }
                }
            }
        }
        // WakeLock 會在 3 秒後自動釋放，如有需要可手動呼叫 wakeLock.release()
    }
}
