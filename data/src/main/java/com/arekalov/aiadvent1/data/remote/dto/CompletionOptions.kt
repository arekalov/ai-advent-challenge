package com.arekalov.aiadvent1.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.8,
    val maxTokens: Int = 3000,
    val reasoningOptions: ReasoningOptions = ReasoningOptions(mode = "DISABLED")
)

