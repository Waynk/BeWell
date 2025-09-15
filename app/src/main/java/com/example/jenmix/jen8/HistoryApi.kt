package com.example.jenmix.jen8

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface HistoryApi {

    // ✅ 新版：根據 username 查指定日期區間資料
    @GET("weight-history")
    fun getWeightHistory(
        @Query("start") startDate: String,
        @Query("end") endDate: String,
        @Query("username") username: String
    ): Call<List<WeightRecord>>

    // ✅ 圖表查全部資料（根據登入帳號）
    @GET("weight-history")
    fun getChartData(
        @Query("username") username: String
    ): Call<List<WeightRecord>>
}
