package com.example.jenmix.jen2

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.widget.Toast
import com.example.jenmix.R

object ReminderUtils {
    private const val CHANNEL_ID = "reminder_channel"
    private const val CHANNEL_NAME = "Medication Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for medication reminders"

    /**
     * 建立通知渠道 (Android 8.0+)
     * 加入聲音設定，確保通知有聲音
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // 改為 HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                setSound(notificationSound, audioAttributes)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * 播放通知音效
     */
    fun playSoundFeedback(context: Context) {
        try {
            val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(context, notificationSound)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 觸發震動
     */
    fun triggerVibration(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE)
                    it.vibrate(effect)
                } else {
                    it.vibrate(2000)
                }
            }
        } else {
            Toast.makeText(context, "沒有震動權限", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 顯示通知
     */
    fun showNotification(context: Context, reminder: Reminder) {
        createNotificationChannel(context)

        // Android 13+ 需要動態通知權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                displayNotification(context, reminder)
            } else {
                Toast.makeText(context, "沒有通知權限", Toast.LENGTH_SHORT).show()
            }
        } else {
            displayNotification(context, reminder)
        }
    }

    /**
     * 具備 POST_NOTIFICATIONS 權限後，真正顯示通知
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun displayNotification(context: Context, reminder: Reminder) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminder.title)
            .setContentText(reminder.content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.content))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 設為 HIGH
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        val manager = NotificationManagerCompat.from(context)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
