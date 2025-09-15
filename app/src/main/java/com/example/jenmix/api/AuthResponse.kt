// AuthResponse.kt
package com.example.jenmix.api

data class AuthResponse(
    val token: String,
    val user_id: Int,
    val display_name: String
)
