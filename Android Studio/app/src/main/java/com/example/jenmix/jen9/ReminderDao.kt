package com.example.jenmix.jen9

import androidx.room.*

@Dao
interface ReminderDao {

    @Insert
    suspend fun insert(reminder: ReminderEntity)

    // ✅ 根據帳號查詢
    @Query("SELECT * FROM reminders WHERE username = :username ORDER BY timeInMillis ASC")
    suspend fun getRemindersByUsername(username: String): List<ReminderEntity>

    // ✅ 根據帳號 + title + 時間刪除（建議用於刪卡片）
    @Query("DELETE FROM reminders WHERE username = :username AND title = :title AND timeInMillis = :timeInMillis")
    suspend fun deleteByUsernameTitleTime(username: String, title: String, timeInMillis: Long)

    // 備用：刪除某時間所有提醒（不推薦，因為會跨帳號）
    @Query("DELETE FROM reminders WHERE timeInMillis = :timeInMillis")
    suspend fun deleteByTime(timeInMillis: Long)
}
