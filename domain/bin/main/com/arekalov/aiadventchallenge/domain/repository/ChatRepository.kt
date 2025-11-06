package com.arekalov.aiadventchallenge.domain.repository

import com.arekalov.aiadventchallenge.domain.model.ChatRequest
import com.arekalov.aiadventchallenge.domain.model.ChatResponse

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse>
}

