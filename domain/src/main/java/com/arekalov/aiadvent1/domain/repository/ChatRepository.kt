package com.arekalov.aiadvent1.domain.repository

import com.arekalov.aiadvent1.domain.model.ChatRequest
import com.arekalov.aiadvent1.domain.model.ChatResponse

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse>
}

