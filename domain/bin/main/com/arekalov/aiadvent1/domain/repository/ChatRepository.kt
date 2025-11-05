package com.arekalov.aiadvent1.domain.repository

import com.arekalov.aiadvent1.domain.model.ChatRequest

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequest): Result<String>
}

