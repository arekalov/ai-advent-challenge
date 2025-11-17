package com.arekalov.aiadventchallenge.domain.repository

import com.arekalov.aiadventchallenge.domain.model.ChatRequest
import com.arekalov.aiadventchallenge.domain.model.ChatResponse
import com.arekalov.aiadventchallenge.domain.model.Message

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse>
    suspend fun compressHistory(messages: List<Message>): Result<Message> // Day 8: Сжатие истории
    
    // Day 9: Методы для работы с памятью
    suspend fun saveMessage(conversationId: Long, message: Message)
    suspend fun getConversationHistory(conversationId: Long): List<Message>
    suspend fun createConversation(title: String = "Новый разговор"): Long
    suspend fun getOrCreateActiveConversation(): Long
    suspend fun clearConversation(conversationId: Long) // Очистка истории разговора
}

