package com.example.jenmix.jen3

sealed class DiseaseItem {
    data class Description(val text: String) : DiseaseItem()
    data class Video(
        val title: String,
        val videoUrl: String,
        val referenceUrl: String,
    ) : DiseaseItem()
}
