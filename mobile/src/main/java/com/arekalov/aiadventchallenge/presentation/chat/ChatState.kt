package com.arekalov.aiadventchallenge.presentation.chat

import com.arekalov.aiadventchallenge.domain.model.Message
import com.arekalov.aiadventchallenge.domain.model.ModelInfo

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val selectedTemperature: Float = 0.7f,
    val availableModels: List<ModelInfo> = emptyList(),
    val selectedModelId: String = "gemma-2-9b",
    val isSettingsExpanded: Boolean = false,
    val isTokenTestModeEnabled: Boolean = false,
    val currentTokenUsage: Int = 0, // Сумма токенов в текущей истории
    val tokenLimit: Int = 50000, // Лимит для YandexGPT
    
    // Day 8: Сжатие истории
    val isCompressing: Boolean = false, // Флаг процесса сжатия
    val tokensSaved: Int = 0 // Всего сохраненные токены
)

