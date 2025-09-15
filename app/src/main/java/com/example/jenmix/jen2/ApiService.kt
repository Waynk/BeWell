package com.example.jenmix.jen2

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    // 不需要在路徑前加斜線
    @GET("get_medications")
    fun getMedications(): Call<List<Medication>>
}
