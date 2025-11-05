package com.arekalov.aiadvent1.domain.model

data class Message(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val totalTokens: Int? = null
)

