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
import com.arekalov.aiadventchallenge.domain.model.ModelMetrics
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

    suspend fun sendMessage(
        messages: List<MessageDto>,
        temperature: Float = 0.7f
    ): Result<ChatResponse> = sendMessageWithTools(messages, temperature, null)
    
    // Day 10: Метод для получения сырого ответа (для обработки tool calls)
    suspend fun sendMessageRaw(
        messages: List<MessageDto>,
        temperature: Float = 0.7f,
        tools: List<com.arekalov.aiadventchallenge.data.remote.dto.ToolDefinition>? = null
    ): Result<YandexGptResponse> = runCatching {
        val request = YandexGptRequest(
            modelUri = "gpt://$folderId/yandexgpt",
            completionOptions = CompletionOptions(
                stream = false,
                temperature = temperature.toDouble(),
                maxTokens = 50000
            ),
            messages = messages,
            jsonObject = tools == null,
            jsonSchema = if (tools == null) {
                JsonSchema(
                    schema = Schema(
                        type = "object",
                        properties = mapOf(
                            "response" to Property(type = "string"),
                            "category" to Property(type = "string"),
                            "stage" to Property(type = "string"),
                            "situation" to Property(type = "string"),
                            "heroes" to Property(type = "string"),
                            "humor_type" to Property(type = "string")
                        ),
                        required = listOf("response", "category", "stage", "situation", "heroes", "humor_type")
                    )
                )
            } else null,
            toolChoice = if (tools != null) {
                com.arekalov.aiadventchallenge.data.remote.dto.ToolChoice(mode = "AUTO")
            } else null,
            tools = tools
        )

        // Логируем запрос для отладки
        if (tools != null) {
            Log.d("YandexGptApi", "Sending request with ${tools.size} tools:")
            tools.forEach { tool ->
                Log.d("YandexGptApi", "  - ${tool.function.name}: ${tool.function.description}")
            }
        }

        val httpResponse = httpClient.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            bearerAuth(apiKey)
            setBody(request)
        }

        val rawResponse = httpResponse.bodyAsText()
        Log.d("YandexGptApi", "Raw API response:\n$rawResponse")
        
        json.decodeFromString(rawResponse)
    }
    
    // Day 10: Новый метод с поддержкой Function Calling
    suspend fun sendMessageWithTools(
        messages: List<MessageDto>,
        temperature: Float = 0.7f,
        tools: List<com.arekalov.aiadventchallenge.data.remote.dto.ToolDefinition>? = null
    ): Result<ChatResponse> = runCatching {
        val startTime = System.currentTimeMillis()
        
        val request = YandexGptRequest(
            modelUri = "gpt://$folderId/yandexgpt",
            completionOptions = CompletionOptions(
                stream = false,
                temperature = temperature.toDouble(),
                maxTokens = 50000
            ),
            messages = messages,
            jsonObject = tools == null, // JSON mode только если нет tools
            jsonSchema = if (tools == null) {
                JsonSchema(
                    schema = Schema(
                        type = "object",
                        properties = mapOf(
                            "response" to Property(type = "string"),
                            "category" to Property(type = "string"),
                            "stage" to Property(type = "string"),
                            "situation" to Property(type = "string"),
                            "heroes" to Property(type = "string"),
                            "humor_type" to Property(type = "string")
                        ),
                        required = listOf("response", "category", "stage", "situation", "heroes", "humor_type")
                    )
                )
            } else null,
            toolChoice = if (tools != null) {
                com.arekalov.aiadventchallenge.data.remote.dto.ToolChoice(mode = "AUTO")
            } else null,
            tools = tools
        )

        // Логируем запрос для отладки
        if (tools != null) {
            Log.d("YandexGptApi", "Sending request with ${tools.size} tools:")
            tools.forEach { tool ->
                Log.d("YandexGptApi", "  - ${tool.function.name}: ${tool.function.description}")
            }
        }

        val httpResponse = httpClient.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            bearerAuth(apiKey)
            setBody(request)
        }

        val responseTimeMs = System.currentTimeMillis() - startTime
        
        // Логируем сырой HTTP ответ
        val rawResponse = httpResponse.bodyAsText()
        Log.d("YandexGptApi", "Raw API response:\n$rawResponse")

        // Десериализуем ответ
        val response: YandexGptResponse = json.decodeFromString(rawResponse)

        val alternative = response.result.alternatives.firstOrNull()
            ?: throw Exception("No response from API")

        val messageText = alternative.message.text
        val usage = response.result.usage
        
        // Создаем метрики на основе данных от Yandex API
        val metrics = ModelMetrics(
            responseTimeMs = responseTimeMs,
            inputTokens = usage.inputTextTokens.toIntOrNull() ?: 0,
            outputTokens = usage.completionTokens.toIntOrNull() ?: 0,
            totalTokens = usage.totalTokens.toIntOrNull() ?: 0,
            modelName = "YandexGPT",
            estimatedCost = 0.0 // YandexGPT бесплатен для тестирования
        )
        
        Log.d("YandexGptApi", "Token usage: input=${metrics.inputTokens}, output=${metrics.outputTokens}, total=${metrics.totalTokens}, time=${metrics.getFormattedTime()}")

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
                
                // Проверяем, что response не пустой
                if (jsonResponse.response.isBlank()) {
                    Log.e("YandexGptApi", "Model returned empty response field! JSON: $messageText")
                    throw Exception("Model returned empty response. Please try again.")
                }
                
                ChatResponse(
                    text = jsonResponse.response.trim(),
                    category = jsonResponse.category,
                    stage = jsonResponse.stage,
                    totalTokens = metrics.totalTokens,
                    metrics = metrics
                )
            }

            alternative.status.isContentFiltered() -> {
                // Контент отфильтрован - возвращаем сообщение модели
                Log.w("YandexGptApi", "Content filtered:\n$messageText")
                ChatResponse(
                    text = messageText,
                    category = "Другое",
                    stage = "Ошибка",
                    totalTokens = metrics.totalTokens,
                    metrics = metrics
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
                    stage = "Ошибка",
                    totalTokens = metrics.totalTokens,
                    metrics = metrics
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

