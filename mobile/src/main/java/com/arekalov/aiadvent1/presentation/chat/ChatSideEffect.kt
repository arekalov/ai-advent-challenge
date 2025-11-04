package com.arekalov.aiadvent1.presentation.chat

sealed interface ChatSideEffect {
    data class ShowError(val message: String) : ChatSideEffect
    data object ScrollToBottom : ChatSideEffect
}

