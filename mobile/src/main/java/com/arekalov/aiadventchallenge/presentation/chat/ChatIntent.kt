package com.arekalov.aiadventchallenge.presentation.chat

sealed interface ChatIntent {
    data class SendMessage(val text: String) : ChatIntent
    data class UpdateInputText(val text: String) : ChatIntent
    data object ClearError : ChatIntent
}

