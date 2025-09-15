package com.example.jenmix.jen2

data class Medication(
    val id: Int,
    val name: String,
    val type: String,
    val dosage: String,
    val ingredients: String,
    val contraindications: String,
    val side_effects: String,
    val source_url: String
)
