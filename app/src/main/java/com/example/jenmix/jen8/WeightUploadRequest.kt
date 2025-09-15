package com.example.jenmix.jen8

data class WeightUploadRequest(
    val username: String,
    val weight: Float,
    val gender: String,
    val height: Float,
    val age: Int
)
