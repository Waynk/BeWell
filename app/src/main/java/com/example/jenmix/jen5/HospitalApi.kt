package com.example.jenmix.jen5

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface HospitalApi {
    @GET("hospitals")
    fun getHospitals(
        @Query("region") region: String
    ): Call<List<Hospital>>
}
