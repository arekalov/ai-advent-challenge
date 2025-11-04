package com.arekalov.aiadvent1.presentation.chat

sealed interface ChatIntent {
    data class SendMessage(val text: String) : ChatIntent
    data class UpdateInputText(val text: String) : ChatIntent
    data object ClearError : ChatIntent
}

