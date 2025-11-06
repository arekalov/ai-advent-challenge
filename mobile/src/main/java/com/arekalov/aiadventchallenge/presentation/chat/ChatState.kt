package com.arekalov.aiadventchallenge.presentation.chat

import com.arekalov.aiadventchallenge.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = ""
)

