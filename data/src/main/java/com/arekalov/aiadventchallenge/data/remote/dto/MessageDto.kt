package com.arekalov.aiadventchallenge.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val role: String,
    val text: String = "",  // Сделаем опциональным с дефолтным значением
    val toolCallList: ToolCallList? = null  // Для ответов с tool calls
)

