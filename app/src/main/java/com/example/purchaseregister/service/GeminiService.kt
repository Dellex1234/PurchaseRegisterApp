// com.example.purchaseregister.service.GeminiService.kt
package com.example.purchaseregister.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun optimizeBitmap(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val scale = maxSize.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun extractJsonFromResponse(response: String): String {
        return try {
            val json = JSONObject(response)
            val candidates = json.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val firstPart = parts.getJSONObject(0)
            var text = firstPart.getString("text").trim()

            text = cleanMarkdown(text)

            if (!text.startsWith("{") || !text.endsWith("}")) {
                val start = text.indexOf("{")
                val end = text.lastIndexOf("}")
                if (start != -1 && end != -1) {
                    text = text.substring(start, end + 1)
                } else {
                    throw Exception("No JSON found")
                }
            }
            text
        } catch (e: Exception) {
            val start = response.indexOf("{")
            val end = response.lastIndexOf("}")
            if (start != -1 && end != -1) {
                response.substring(start, end + 1).trim()
            } else {
                throw e
            }
        }
    }

    private fun cleanMarkdown(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7).trim()
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3).trim()
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length - 3).trim()

        listOf("JSON:", "json:", "Response:", "response:").forEach { prefix ->
            if (cleaned.startsWith(prefix)) cleaned = cleaned.substring(prefix.length).trim()
        }

        return cleaned
    }

    fun analyzeInvoice(
        bitmap: Bitmap,
        prompt: String,
        apiKey: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val optimizedBitmap = optimizeBitmap(bitmap)
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val escapedPrompt = prompt.replace("\n", "\\n").replace("\"", "\\\"")
        val imageBase64 = bitmapToBase64(optimizedBitmap)

        val json = """
        {
          "contents": [{
            "parts": [
              { "text": "$escapedPrompt" },
              {
                "inline_data": {
                  "mime_type": "image/jpeg",
                  "data": "$imageBase64"
                }
              }
            ]
          }]
        }
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())
        val models = listOf("gemini-flash-latest", "gemini-2.0-flash-latest", "gemini-1.5-flash")

        tryModel(0, models, client, body, apiKey, onSuccess, onError)
    }

    private fun tryModel(
        index: Int,
        models: List<String>,
        client: OkHttpClient,
        body: RequestBody,
        apiKey: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (index >= models.size) {
            onError("No hay modelos disponibles")
            return
        }

        val modelName = models[index]
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                tryModel(index + 1, models, client, body, apiKey, onSuccess, onError)
            }

            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string()
                when {
                    response.isSuccessful -> onSuccess(text ?: "")
                    response.code == 429 || response.code == 404 ->
                        tryModel(index + 1, models, client, body, apiKey, onSuccess, onError)
                    else -> tryModel(index + 1, models, client, body, apiKey, onSuccess, onError)
                }
            }
        })
    }
}