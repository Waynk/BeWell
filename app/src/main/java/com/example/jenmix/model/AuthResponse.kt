package com.example.jenmix.model

data class AuthResponse(
    val message: String,
    val token: String? = null,
    val user_id: Int? = null
)