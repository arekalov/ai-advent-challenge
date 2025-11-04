package com.arekalov.aiadvent1.data.remote.api

import android.util.Log
import com.arekalov.aiadvent1.data.remote.dto.CompletionOptions
import com.arekalov.aiadvent1.data.remote.dto.JsonResponse
import com.arekalov.aiadvent1.data.remote.dto.JsonSchema
import com.arekalov.aiadvent1.data.remote.dto.MessageDto
import com.arekalov.aiadvent1.data.remote.dto.Property
import com.arekalov.aiadvent1.data.remote.dto.Schema
import com.arekalov.aiadvent1.data.remote.dto.YandexGptRequest
import com.arekalov.aiadvent1.data.remote.dto.YandexGptResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import javax.inject.Inject

class YandexGptApi @Inject constructor(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val folderId: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendMessage(messages: List<MessageDto>): Result<String> = runCatching {
        val request = YandexGptRequest(
            modelUri = "gpt://$folderId/yandexgpt-lite",
            completionOptions = CompletionOptions(
                stream = false,
                temperature = 0.8,
                maxTokens = 3000
            ),
            messages = messages,
            jsonObject = true,
            jsonSchema = JsonSchema(
                schema = Schema(
                    type = "object",
                    properties = mapOf("response" to Property(type = "string")),
                    required = listOf("response")
                )
            )
        )

        val response: YandexGptResponse = httpClient.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            bearerAuth(apiKey)
            setBody(request)
        }.body()

        val messageText = response.result.alternatives.firstOrNull()?.message?.text
            ?: throw Exception("No response from API")

        // Парсим JSON ответ
        val jsonResponse = json.decodeFromString<JsonResponse>(messageText)
        jsonResponse.response
    }.onFailure { e ->
        Log.e("YandexGptApi", "Error sending message", e)
    }

    companion object {
        private const val BASE_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    }
}

