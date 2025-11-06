package com.arekalov.aiadventchallenge.domain.model

data class ChatRequest(
    val userMessage: String,
    val conversationHistory: List<Message> = emptyList()
)

