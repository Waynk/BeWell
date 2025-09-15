package com.example.jenmix.jen8

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface UploadApi {
    @POST("upload-weight")
    fun uploadWeight(@Body request: WeightUploadRequest): Call<WeightUploadResponse>
}
