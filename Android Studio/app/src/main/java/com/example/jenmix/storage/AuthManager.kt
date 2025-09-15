package com.example.jenmix.storage

import android.content.Context

object AuthManager {

    private const val PREF = "auth"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "userId"

    fun saveToken(ctx: Context, token: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
}
