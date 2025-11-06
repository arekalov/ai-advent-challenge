package com.arekalov.aiadventchallenge.data.remote.api

import android.util.Log
import com.arekalov.aiadventchallenge.data.remote.dto.CompletionOptions
import com.arekalov.aiadventchallenge.data.remote.dto.JsonResponse
import com.arekalov.aiadventchallenge.data.remote.dto.JsonSchema
import com.arekalov.aiadventchallenge.data.remote.dto.MessageDto
import com.arekalov.aiadventchallenge.data.remote.dto.Property
import com.arekalov.aiadventchallenge.data.remote.dto.Schema
import com.arekalov.aiadventchallenge.data.remote.dto.YandexGptRequest
import com.arekalov.aiadventchallenge.data.remote.dto.YandexGptResponse
import com.arekalov.aiadventchallenge.domain.model.ChatResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
    private val prettyJson = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    suspend fun sendMessage(messages: List<MessageDto>): Result<ChatResponse> = runCatching {
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
                    properties = mapOf(
                        "response" to Property(type = "string"),
                        "category" to Property(type = "string"),
                    ),
                    required = listOf("response", "category")
                )
            )
        )

        val httpResponse = httpClient.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            bearerAuth(apiKey)
            setBody(request)
        }

        // Логируем сырой HTTP ответ
        val rawResponse = httpResponse.bodyAsText()

        // Десериализуем ответ
        val response: YandexGptResponse = json.decodeFromString(rawResponse)

        val alternative = response.result.alternatives.firstOrNull()
            ?: throw Exception("No response from API")

        val messageText = alternative.message.text
        val totalTokens = response.result.usage.totalTokens.toIntOrNull()

        // Обрабатываем статус ответа
        when {
            alternative.status.isSuccess() -> {
                // Нормальный ответ - парсим JSON
                try {
                    val prettyMessage = prettyJson.parseToJsonElement(messageText)
                    Log.d("YandexGptApi", "JSON from model:\n${prettyJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), prettyMessage)}")
                } catch (e: Exception) {
                    Log.d("YandexGptApi", "JSON from model: $messageText")
                }

                val jsonResponse = json.decodeFromString<JsonResponse>(messageText)
                ChatResponse(
                    text = jsonResponse.response,
                    category = jsonResponse.category,
                    totalTokens = totalTokens
                )
            }

            alternative.status.isContentFiltered() -> {
                // Контент отфильтрован - возвращаем сообщение модели
                Log.w("YandexGptApi", "Content filtered:\n$messageText")
                ChatResponse(
                    text = messageText,
                    category = "Другое",
                    totalTokens = totalTokens
                )
            }

            else -> {
                // Другие статусы - логируем и возвращаем текст
                Log.e(
                    "YandexGptApi",
                    "Unexpected status: ${alternative.status}\nMessage: $messageText"
                )
                ChatResponse(
                    text = messageText,
                    category = "Другое",
                    totalTokens = totalTokens
                )
            }
        }
    }.onFailure { e ->
        Log.e("YandexGptApi", "Error sending message $e", e)
    }

    companion object {
        private const val BASE_URL =
            "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    }
}

