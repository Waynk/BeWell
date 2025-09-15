package com.example.jenmix.jen3

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("/diseases")
    fun getAllDiseases(): Call<List<Disease>>

    @GET("/diseases/{id}/videos")
    fun getVideosByDisease(@Path("id") id: Int): Call<List<DiseaseVideo>>
}
