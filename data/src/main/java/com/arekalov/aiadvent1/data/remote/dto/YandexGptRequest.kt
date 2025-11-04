package com.arekalov.aiadvent1.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class YandexGptRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<MessageDto>,
    val jsonObject: Boolean = true,
    val jsonSchema: JsonSchema? = null,
    val parallelToolCalls: Boolean = false,
    val toolChoice: ToolChoice = ToolChoice(mode = "NONE")
)

