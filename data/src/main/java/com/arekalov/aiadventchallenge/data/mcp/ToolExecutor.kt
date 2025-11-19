package com.arekalov.aiadventchallenge.data.mcp

import android.util.Log
import com.arekalov.aiadventchallenge.data.remote.dto.ToolCall
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

class ToolExecutor @Inject constructor(
    private val httpClient: HttpClient,
    private val mcpServerUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Выполнить вызов инструмента через MCP сервер
     */
    suspend fun executeToolCall(toolCall: ToolCall): Result<String> = runCatching {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "Executing tool call")
        Log.d(TAG, "Tool name: ${toolCall.function.name}")
        Log.d(TAG, "Tool ID: ${toolCall.id}")
        Log.d(TAG, "Raw arguments: ${toolCall.function.arguments}")
        
        // Парсим аргументы из строки в JsonObject
        val arguments: JsonObject? = if (toolCall.function.arguments.isNotBlank()) {
            try {
                val parsed = json.parseToJsonElement(toolCall.function.arguments).jsonObject
                Log.d(TAG, "Parsed arguments: $parsed")
                parsed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse arguments: ${e.message}", e)
                null
            }
        } else {
            Log.d(TAG, "No arguments provided")
            null
        }
        
        // Формируем запрос к MCP серверу
        val request = MCPToolCallRequest(
            name = toolCall.function.name,
            arguments = arguments
        )
        
        val requestUrl = "$mcpServerUrl/mcp/tools/call"
        Log.d(TAG, "Sending POST request to: $requestUrl")
        Log.d(TAG, "Request body: ${json.encodeToString(MCPToolCallRequest.serializer(), request)}")
        
        val startTime = System.currentTimeMillis()
        
        // Отправляем запрос
        val response: MCPToolCallResponse = try {
            httpClient.post(requestUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "HTTP request failed after ${duration}ms", e)
            Log.e(TAG, "Error type: ${e::class.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            throw e
        }
        
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Received response in ${duration}ms")
        Log.d(TAG, "Response: ${json.encodeToString(MCPToolCallResponse.serializer(), response)}")
        
        // Проверяем наличие ошибки
        if (response.error != null) {
            Log.e(TAG, "Tool execution error: ${response.error}")
            throw Exception("Tool execution failed: ${response.error}")
        }
        
        Log.d(TAG, "Tool result length: ${response.result.length} characters")
        Log.d(TAG, "Tool result preview: ${response.result.take(100)}${if (response.result.length > 100) "..." else ""}")
        Log.d(TAG, "Tool execution completed successfully")
        Log.d(TAG, "═══════════════════════════════════════")
        
        response.result
    }.onFailure { e ->
        Log.e(TAG, "═══════════════════════════════════════")
        Log.e(TAG, "Failed to execute tool call", e)
        Log.e(TAG, "MCP Server URL: $mcpServerUrl")
        Log.e(TAG, "Tool name: ${toolCall.function.name}")
        Log.e(TAG, "Error details: ${e.message}")
        Log.e(TAG, "═══════════════════════════════════════")
    }
    
    companion object {
        private const val TAG = "ToolExecutor"
    }
    
    @Serializable
    private data class MCPToolCallRequest(
        val name: String,
        val arguments: JsonObject? = null
    )
    
    @Serializable
    private data class MCPToolCallResponse(
        val result: String,
        val error: String? = null
    )
}

