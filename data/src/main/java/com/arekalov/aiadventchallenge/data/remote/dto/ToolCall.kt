package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: FunctionCall
)

