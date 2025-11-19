package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition
)

