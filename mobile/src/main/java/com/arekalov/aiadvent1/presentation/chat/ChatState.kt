package com.arekalov.aiadvent1.presentation.chat

import com.arekalov.aiadvent1.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = ""
)

