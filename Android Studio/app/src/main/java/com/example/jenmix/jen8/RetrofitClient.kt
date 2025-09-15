package com.example.jenmix.jen8

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object  RetrofitClient {

    // ✅ 改成你自己的後端 API 網址（測試或正式環境）
    private const val BASE_URL = "http://172.20.10.4:10000"

    // ✅ 建立 Retrofit 實例，支援日期格式
    val retrofit: Retrofit by lazy {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // ✅ 呼叫方式：RetrofitClient.create<YourApi>()
    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}
