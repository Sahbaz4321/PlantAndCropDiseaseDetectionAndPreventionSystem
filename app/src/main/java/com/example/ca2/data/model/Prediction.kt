package com.example.ca2.data.model

data class Prediction(
    val predictionId: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val diseaseName: String = "",
    val confidence: Double = 0.0,
    val description: String = "",
    val causes: String = "",
    val prevention: String = "",
    val fertilizer: String = "",
    val pesticide: String = "",
    val recoveryTime: String = "",
    val extraCareTips: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
