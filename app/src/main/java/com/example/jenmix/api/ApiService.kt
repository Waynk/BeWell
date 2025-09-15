package com.example.jenmix.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import com.example.jenmix.model.SimpleResponse
import com.example.jenmix.model.UserInfoResponse


interface ApiService {

    // —— 用戶認證 ——
    @GET("register")
    fun register(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("display_name") displayName: String,
        @Query("age") age: Int,
        @Query("gender") gender: String,
        @Query("height") height: Float,
        @Query("weight") weight: Float
    ): Call<AuthResponse>

    @GET("login")
    fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Call<AuthResponse>

    // —— 血壓數據 ——
    @GET("getBloodPressureByValue")
    fun getBloodPressureByValue(
        @Query("type") type: String,
        @Query("min") min: Int,
        @Query("max") max: Int
    ): Call<List<BpRecord>>

    @GET("getFilteredBloodPressureData")
    fun getFilteredBloodPressureData(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("filter") filter: String,
        @Query("user_id") userId: Int
    ): Call<List<BpRecord>>

    // —— 體重數據 ——
    @GET("getFilteredWeightData")
    fun getFilteredWeightData(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("user_id") userId: Int
    ): Call<List<WeightRecord>>

    // —— 焦慮分數 ——
    @POST("submit-anxiety-score")
    fun submitAnxietyScore(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<BasicResponse>

    @GET("get-anxiety-scores")
    fun getAnxietyScores(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("user_id") userId: Int
    ): Call<List<AnxietyScore>>

    // —— CSV 上傳 ——
    @Multipart
    @POST("upload")
    fun uploadCsv(
        @Part csvFile: MultipartBody.Part,
        @Part("user_id") userId: RequestBody
    ): Call<BasicResponse>

    // —— 家庭和提醒等接口……

    // 取得使用者資料
    @GET("user/profile")
    fun getUserInfo(
        @Query("username") username: String
    ): Call<UserInfoResponse>

    // 更新性別、年齡、身高
    @GET("user/update-profile")
    fun updateUserInfo(
        @Query("username") username: String,
        @Query("gender") gender: String,
        @Query("age") age: String,
        @Query("height") height: String
    ): Call<SimpleResponse>

    // 更改密碼
    @GET("user/change-password")
    fun changePassword(
        @Query("username") username: String,
        @Query("current_password") oldPassword: String,
        @Query("new_password") newPassword: String
    ): Call<SimpleResponse>


    /**
     * 注册 / 登录 返回
     */


    /**
     * 血压记录
     */
    data class BpRecord(
        val measure_at: String,
        val systolic_mmHg: Int,
        val diastolic_mmHg: Int
    )

    /**
     * 体重记录
     */
    data class WeightRecord(
        val measure_at: String,
        val weight_kg: Float
    )

    /**
     * 焦虑分数
     */
    data class AnxietyScore(
        val measurementDate: String,
        val score: Int
    )

    /**
     * 通用成功消息
     */
    data class BasicResponse(
        val message: String
    )
}