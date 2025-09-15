package com.example.jenmix.jen2

import androidx.room.*

@Dao
interface MedicationReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: MedicationReminderEntity)

    // ✅ 只撈該使用者的提醒
    @Query("SELECT * FROM medication_reminders WHERE username = :username")
    suspend fun getAllByUsername(username: String): List<MedicationReminderEntity>

    // ✅ 只刪除該使用者對應的提醒
    @Query("DELETE FROM medication_reminders WHERE requestCode = :requestCode AND username = :username")
    suspend fun deleteByRequestCode(requestCode: Int, username: String)
}
