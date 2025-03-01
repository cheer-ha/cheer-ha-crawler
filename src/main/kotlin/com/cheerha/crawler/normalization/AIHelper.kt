package com.cheerha.crawler.normalization

import io.github.cdimascio.dotenv.Dotenv
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AIHelper {
    private val dotenv = Dotenv.load()
    private val API_KEY = dotenv["API_KEY"]
    private const val API_URL = "https://api.openai.com/v1/chat/completions"

    fun normalizeText(input: String, task: String): String {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("model", "gpt-3.5-turbo")
        json.put("messages", listOf(
            JSONObject().put("role", "system").put("content", "데이터 정규화 작업을 도와줘. 출력: output: 같은 단어로 너의 응답결과를 나타내줄 필요는 없어. 그냥 딱 '단어' 로만 응답해줘"),
            JSONObject().put("role", "user").put("content", "Task: $task. Keep it in Korean. Input: $input")
        ))
        json.put("temperature", 0.3)

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "Error: ${response.code}"
                }

                val responseBody = response.body?.string() ?: return "Error: Empty response"
                val jsonResponse = JSONObject(responseBody)

                jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
