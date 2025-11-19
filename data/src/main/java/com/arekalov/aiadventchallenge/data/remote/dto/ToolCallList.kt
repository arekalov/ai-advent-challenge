package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ToolCallList(
    val toolCalls: List<ToolCallItem>
)

@Serializable
data class ToolCallItem(
    val functionCall: FunctionCallItem
)

@Serializable
data class FunctionCallItem(
    val name: String,
    val arguments: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.buildJsonObject {}
)

