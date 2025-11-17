package com.arekalov.aiadventchallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Метрики производительности модели
 */
@Serializable
data class ModelMetrics(
    val responseTimeMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int = inputTokens + outputTokens,
    val modelName: String,
    val estimatedCost: Double = 0.0 // В долларах
) {
    fun getFormattedTime(): String {
        return when {
            responseTimeMs < 1000 -> "${responseTimeMs}ms"
            responseTimeMs < 60000 -> "%.2fs".format(responseTimeMs / 1000.0)
            else -> "%.2fm".format(responseTimeMs / 60000.0)
        }
    }

    fun getFormattedTokens(): String {
        return "$totalTokens tokens (↓$inputTokens ↑$outputTokens)"
    }

    fun getFormattedCost(): String {
        return if (estimatedCost > 0) {
            "$%.4f".format(estimatedCost)
        } else {
            "Free"
        }
    }
}

