package com.example.jenmix.jen8


import retrofit2.Call
import retrofit2.http.GET


interface GptApi {
    @GET("/api/daily-quote")
    fun getDailyQuote(): Call<QuoteResponse>
}