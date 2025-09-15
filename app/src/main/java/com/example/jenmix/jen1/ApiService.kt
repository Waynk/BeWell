package com.example.jenmix.jen1

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // ✅ 單日血壓分析（純血壓、脈搏）
    @GET("analyzeSingleBP")
    fun analyzeSingleBP(
        @Query("username") username: String,
        @Query("date") date: String
    ): Call<AnalysisResult>

    // ✅ 區間血壓分析
    @GET("analyzeRangeBP")
    fun analyzeRangeBP(
        @Query("username") username: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): Call<AnalysisResult>

    // ✅ 單日體重分析（BMI、體重）
    @GET("analyzeSingleWeight")
    fun analyzeSingleWeight(
        @Query("username") username: String,
        @Query("date") date: String
    ): Call<AnalysisResult>

    // ✅ 區間體重分析
    @GET("analyzeRangeWeight")
    fun analyzeRangeWeight(
        @Query("username") username: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): Call<AnalysisResult>

    // ✅ 抓取圖表資料（已整合血壓＋體重）
    @GET("get_combined_records")
    fun getCombinedRecords(
        @Query("username") username: String
    ): Call<List<HealthRecord>>

    @POST("analyzeCombinedRecords")
    fun analyzeCombinedRecord(
        @Body payload: Map<String, String>
    ): Call<AnalyzeResponse>

    // ✅ 疾病建議對應網址
    @GET("get_source_url")
    fun getSourceUrl(@Query("disease") disease: String): Call<UrlResponse>
   }
