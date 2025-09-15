package com.example.jenmix.storage

import android.content.Context
import android.content.SharedPreferences

object UserPrefs {

    private const val PREF_NAME = "user_info"
    private const val PREF_USER = "user_prefs"

    private const val KEY_GENDER = "gender"
    private const val KEY_HEIGHT = "height"
    private const val KEY_AGE = "age"
    private const val KEY_TTS_ENABLED = "tts_enabled"
    private const val KEY_USERNAME = "username"
    private const val KEY_DISPLAY_NAME = "display_name"

    private const val PREF_TTS_ENABLED = "pref_tts_enabled"
    private const val PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN = "PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN"

    var height: Float = 170f

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun getUserPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
    }

    // 🧍‍♂️ 基本資料存取（根據帳號）
    fun getGender(context: Context): String {
        val username = getUsername(context) ?: return ""
        return getPrefs(context).getString("${KEY_GENDER}_$username", "") ?: ""
    }

    fun getHeight(context: Context): Float {
        val username = getUsername(context) ?: return 0f
        return getPrefs(context).getFloat("${KEY_HEIGHT}_$username", 0f).also { height = it }
    }

    fun getAge(context: Context): Int {
        val username = getUsername(context) ?: return 0
        return getPrefs(context).getInt("${KEY_AGE}_$username", 0)
    }

    fun saveUserInfo(context: Context, gender: String, height: Float, age: Int) {
        val username = getUsername(context) ?: return
        getPrefs(context).edit().apply {
            putString("${KEY_GENDER}_$username", gender)
            putFloat("${KEY_HEIGHT}_$username", height)
            putInt("${KEY_AGE}_$username", age)
            apply()
        }
        this.height = height
    }

    fun setGender(context: Context, gender: String) {
        val username = getUsername(context) ?: return
        getPrefs(context).edit().putString("${KEY_GENDER}_$username", gender).apply()
    }

    fun setHeight(context: Context, height: Float) {
        val username = getUsername(context) ?: return
        getPrefs(context).edit().putFloat("${KEY_HEIGHT}_$username", height).apply()
        this.height = height
    }

    fun setAge(context: Context, age: Int) {
        val username = getUsername(context) ?: return
        getPrefs(context).edit().putInt("${KEY_AGE}_$username", age).apply()
    }

    fun saveGender(context: Context, gender: String) = setGender(context, gender)
    fun saveHeight(context: Context, height: Float) = setHeight(context, height)
    fun saveAge(context: Context, age: Int) = setAge(context, age)

    // ✅ 檢查使用者是否填過基本資料
    fun isProfileFilled(context: Context): Boolean {
        return getGender(context).isNotEmpty() &&
                getHeight(context) > 0f &&
                getAge(context) > 0
    }

    // ✅ 檢查是否已完成首次設定（依照帳號）
    fun isSetupCompleted(context: Context): Boolean {
        val username = getUsername(context) ?: return false
        return getUserPrefs(context).getBoolean("setup_completed_$username", false)
    }

    // ✅ 設定首次設定完成狀態
    fun setSetupCompleted(context: Context, completed: Boolean) {
        val username = getUsername(context) ?: return
        getUserPrefs(context).edit().putBoolean("setup_completed_$username", completed).apply()
    }

    fun clearUserInfo(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    // ✅ TTS 播報開關狀態
    fun isTtsEnabled(context: Context): Boolean {
        return getUserPrefs(context).getBoolean(KEY_TTS_ENABLED, true)
    }

    fun setTtsEnabled(context: Context, enabled: Boolean) {
        getUserPrefs(context).edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    // ✅ 👤 使用者登入資訊
    fun getUsername(context: Context): String? {
        return getUserPrefs(context).getString(KEY_USERNAME, null)
    }

    fun saveUsername(context: Context, username: String) {
        getUserPrefs(context).edit().putString(KEY_USERNAME, username).apply()
    }

    fun saveDisplayName(context: Context, displayName: String) {
        getUserPrefs(context).edit().putString("display_name", displayName).apply()
    }

    fun getDisplayName(context: Context): String {
        return getUserPrefs(context).getString("display_name", "未知") ?: "未知"
    }

    // ✅ 一起儲存帳號與顯示名稱（選用）
    fun saveLogin(context: Context, username: String, displayName: String = username) {
        getUserPrefs(context).edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_DISPLAY_NAME, displayName)
            apply()
        }
    }

    // ✅ 快捷檢查是否已登入
    fun isLoggedIn(context: Context): Boolean {
        return !getUsername(context).isNullOrBlank()
    }

    // ✅ 清除登入狀態（例如登出用）
    fun clearLoginState(context: Context) {
        getUserPrefs(context).edit()
            .remove(KEY_USERNAME)
            .remove(KEY_DISPLAY_NAME)
            .apply()
    }

    // 🔄 重設導覽提示狀態（首次導覽）
    fun resetGuideShown(context: Context) {
        getUserPrefs(context).edit().putBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, false).apply()
    }

    // ✅ 保留舊 TTS 開關（若其他地方仍有使用）
    fun isTTSEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_TTS_ENABLED, true)
    }

    fun setTTSEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_TTS_ENABLED, enabled).apply()
    }

}
