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

    /**
     * Executes a multimodal Gemini-3.5-flash request with structured JSON response config
     * to extract golfer's name, handicap, date, individual scores, and a notes summary.
     */
    suspend fun analyzeScorecard(
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext "API_MOCK"
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        try {
            val root = JSONObject()

            // System instructions to enforce rigid JSON compliance
            val sysInstObj = JSONObject()
            val sysPartsArray = JSONArray()
            sysPartsArray.put(JSONObject().put("text", "You are an expert handwritten golf scorecard digitizer. You must analyze the image and output ONLY a single valid JSON object representing the scorecard fields, without any markdown formatting or ticks. Always return exactly 18 hole scores, individual hole PARs, and individual handicap indices."))
            sysInstObj.put("parts", sysPartsArray)
            root.put("systemInstruction", sysInstObj)

            // Contents array (user prompt + image inlineData)
            val contentsArray = JSONArray()
            val promptObj = JSONObject()
            val promptParts = JSONArray()

            val promptText = """
                Extract the player's name, handicap (if noted), date (e.g. 24/8), and individual hole PAR guidelines, handicap Index guidelines, and the sequence of 18 hole scores for the player.
                - pars: 18-element array of PAR values for each hole. If not found, use standard par 72 defaults: [4,4,3,4,5,4,3,4,5, 4,3,4,4,5,3,4,4,5].
                - indices: 18-element array of difficulty handicap indices for each hole. If not found, use defaults: [9,15,11,1,13,5,17,3,7, 10,18,12,2,14,6,16,4,8].
                - scores: 18-element array of sequential scores/strokes. Use 4 if empty or unreadable.
                - totalScore: sum of the scores array.
                - Then generate notes detailing milestones.
                
                Format the result as this JSON structure:
                {
                  "playerName": "Extracted Player Name",
                  "handicap": "Handicap if found, else empty",
                  "date": "Date if found, else empty",
                  "scores": [18 numbers for holes 1 to 18 sequential scores],
                  "pars": [18 numbers for holes 1 to 18 sequential PARs],
                  "indices": [18 numbers for holes 1 to 18 sequential handicap indices],
                  "totalScore": 102,
                  "notes": "Brief bulleted or styled recap"
                }
            """.trimIndent()

            promptParts.put(JSONObject().put("text", promptText))

            // Inline image bytes
            val imagePart = JSONObject()
            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", mimeType)
            inlineDataObj.put("data", base64Image)
            imagePart.put("inlineData", inlineDataObj)
            promptParts.put(imagePart)

            promptObj.put("role", "user")
            promptObj.put("parts", promptParts)
            contentsArray.put(promptObj)
            root.put("contents", contentsArray)

            // Generation and response schema controls
            val configObj = JSONObject()
            configObj.put("temperature", 0.1)
            
            val responseFormatObj = JSONObject()
            responseFormatObj.put("type", "JSON_OBJECT") // Set type for raw JSON response constraint
            responseFormatObj.put("mimeType", "application/json")
            configObj.put("responseFormat", responseFormatObj)

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
                    Log.e(TAG, "Scorecard generation failed with code ${response.code}: $responseBodyStr")
                    return@withContext "Error: Server returned code ${response.code}"
                }

                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val partsArr = contentObj.optJSONArray("parts")
                        if (partsArr != null && partsArr.length() > 0) {
                            return@withContext partsArr.getJSONObject(0).optString("text", "")
                        }
                    }
                }
                return@withContext "ErrorLog: Response format was unrecognized: $responseBodyStr"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scorecard generation failed", e)
            return@withContext "Error: ${e.localizedMessage}"
        }
    }
}

