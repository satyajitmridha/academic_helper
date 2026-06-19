package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val TAG = "GeminiClient"
    
    // OkHttp client configured with generous 60s timeout as per the SDK rules
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Returns if API key is present and is not the default mock placeholder
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Executes a REST API call to Gemini-3.5-flash to retrieve a rich response.
     * Integrates local query content and sources directly as system instructions/context.
     */
    suspend fun generateResponse(
        prompt: String, 
        systemInstruction: String = "", 
        chatHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext "API Error: Please configure your GEMINI_API_KEY in the AI Studio Secrets Panel. Currently operating in Offline Simulation Mode."
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        try {
            val root = JSONObject()
            
            // System instructions
            if (systemInstruction.isNotEmpty()) {
                val sysInstObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", systemInstruction))
                sysInstObj.put("parts", partsArray)
                root.put("systemInstruction", sysInstObj)
            }

            // Chat content (history + active prompt)
            val contentsArray = JSONArray()
            
            // Append history
            chatHistory.forEach { (role, text) ->
                val contentObj = JSONObject()
                val partsArr = JSONArray()
                partsArr.put(JSONObject().put("text", text))
                contentObj.put("role", if (role == "user") "user" else "model")
                contentObj.put("parts", partsArr)
                contentsArray.put(contentObj)
            }

            // Append current user prompt
            val promptObj = JSONObject()
            val promptParts = JSONArray()
            promptParts.put(JSONObject().put("text", prompt))
            promptObj.put("role", "user")
            promptObj.put("parts", promptParts)
            contentsArray.put(promptObj)

            root.put("contents", contentsArray)

            // High compatibility generation settings
            val configObj = JSONObject()
            configObj.put("temperature", 0.3)
            root.put("generationConfig", configObj)

            val requestBodyStr = root.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyStr.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code: ${response.code}. Body: $responseBodyStr")
                    return@withContext "Error: Request failed with code ${response.code}. Details: ${response.message}"
                }

                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val partsArr = contentObj.optJSONArray("parts")
                        if (partsArr != null && partsArr.length() > 0) {
                            return@withContext partsArr.getJSONObject(0).optString("text", "No text found")
                        }
                    }
                }
                return@withContext "Error: No matching response text parsed. Raw: $responseBodyStr"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call exception", e)
            return@withContext "Offline Mode Fallback / Connection Error: ${e.localizedMessage}"
        }
    }
}
