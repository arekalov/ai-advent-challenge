package com.arekalov.aiadventchallenge.presentation.chat

sealed interface ChatSideEffect {
    data class ShowError(val message: String) : ChatSideEffect
    data object ScrollToBottom : ChatSideEffect
}

