package com.example.ca2.data.api

import com.example.ca2.data.model.Prediction
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PlantApiService {
    @Multipart
    @POST("predict")
    suspend fun predictDisease(
        @Part image: MultipartBody.Part
    ): Response<PredictionResponse>
}

data class PredictionResponse(
    val status: String,
    val message: String,
    val data: Prediction
)
