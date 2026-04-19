package com.example.ca2.data.api

import com.example.ca2.BuildConfig
import com.example.ca2.data.model.Prediction
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

class GeminiDiseaseService(
    private val client: OkHttpClient = OkHttpClient()
) {

    fun buildDetailedPrediction(diseaseName: String, confidence: Double): Prediction {
        val cleanName = diseaseName.replace("___", " ").replace("_", " ").trim()
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            return fallbackPrediction(cleanName, confidence, "Gemini API key missing in local.properties")
        }

        return runCatching {
            val prompt = """
                You are an agricultural plant disease assistant.
                Create a concise, practical report for the plant disease "$cleanName".
                Confidence score: ${(confidence * 100).toInt()}%.
                Respond with valid JSON only using these keys:
                description, causes, prevention, fertilizer, pesticide, recoveryTime, extraCareTips.
                Keep each field farmer-friendly, specific, and under 70 words.
            """.trimIndent()

            val body = JSONObject().apply {
                put("contents", org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                ))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Gemini request failed: ${response.code}")
                }

                val responseBody = response.body?.string().orEmpty()
                val json = JSONObject(responseBody)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val structured = JSONObject(text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
                Prediction(
                    diseaseName = cleanName,
                    confidence = confidence,
                    description = structured.optString("description"),
                    causes = structured.optString("causes"),
                    prevention = structured.optString("prevention"),
                    fertilizer = structured.optString("fertilizer"),
                    pesticide = structured.optString("pesticide"),
                    recoveryTime = structured.optString("recoveryTime"),
                    extraCareTips = structured.optString("extraCareTips")
                )
            }
        }.getOrElse { error ->
            fallbackPrediction(cleanName, confidence, error.message ?: "Unknown Gemini error")
        }
    }

    private fun fallbackPrediction(diseaseName: String, confidence: Double, reason: String): Prediction {
        val readableName = diseaseName.ifBlank { "Plant health issue" }
        val lowerName = readableName.lowercase(Locale.getDefault())
        val isHealthy = listOf("healthy", "normal").any { it in lowerName }

        return Prediction(
            diseaseName = readableName,
            confidence = confidence,
            description = if (isHealthy) {
                "The leaf appears healthy. No major disease markers were detected in the selected image."
            } else {
                "$readableName can stress plant growth if ignored. This fallback summary is being shown because AI details were unavailable."
            },
            causes = if (isHealthy) {
                "Healthy foliage is usually supported by balanced watering, sunlight, clean soil, and low pest pressure."
            } else {
                "Possible triggers include excess moisture, fungal spread, poor airflow, infected tools, nutrient imbalance, or untreated pest damage."
            },
            prevention = if (isHealthy) {
                "Keep monitoring leaves weekly, avoid overwatering, and maintain hygiene around the plant bed."
            } else {
                "Remove affected leaves, improve airflow, water near the roots, avoid leaf wetness at night, and isolate infected plants when possible."
            },
            fertilizer = "Use a balanced NPK fertilizer in moderate quantity and avoid overfeeding stressed plants.",
            pesticide = if (isHealthy) {
                "No pesticide is recommended unless clear signs of pests or fungal spread appear."
            } else {
                "Use a crop-safe fungicide or pesticide recommended for the specific plant variety after confirming the disease with a local expert."
            },
            recoveryTime = if (isHealthy) "Ongoing healthy condition with routine care." else "Typically 7 to 21 days depending on severity and treatment timing.",
            extraCareTips = "Fallback note: $reason. Recheck your Gemini API key to unlock richer AI disease guidance."
        )
    }
}
