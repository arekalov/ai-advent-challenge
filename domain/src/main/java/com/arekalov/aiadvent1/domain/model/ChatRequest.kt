package com.arekalov.aiadvent1.domain.model

data class ChatRequest(
    val userMessage: String,
    val conversationHistory: List<Message> = emptyList()
)

