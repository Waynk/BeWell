package com.example.jenmix.jen8

import android.content.Context

object AuthManager {

    private const val PREF = "auth"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"

    fun saveToken(ctx: Context, token: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

    // ✅ 儲存登入帳號
    fun saveUsername(ctx: Context, username: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, username)
            .apply()
    }

    // ✅ 取得登入帳號
    fun getUsername(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, null)
}
