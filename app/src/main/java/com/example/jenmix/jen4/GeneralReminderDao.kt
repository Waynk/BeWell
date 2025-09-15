package com.example.jenmix.jen4

import androidx.room.*

@Dao
interface GeneralReminderDao {

    // ✅ 依 username 撈出指定使用者的提醒
    @Query("SELECT * FROM general_reminders WHERE username = :username")
    suspend fun getAllByUsername(username: String): List<GeneralReminderEntity>

    // ✅ 新增或更新提醒
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: GeneralReminderEntity)

    // ✅ 刪除特定提醒（可選用這個也可用 deleteById）
    @Delete
    suspend fun delete(reminder: GeneralReminderEntity)

    // ✅ 依 ID 刪除提醒（這個已存在）
    @Query("DELETE FROM general_reminders WHERE id = :id AND username = :username")
    suspend fun deleteByIdAndUsername(id: Int, username: String)
}
