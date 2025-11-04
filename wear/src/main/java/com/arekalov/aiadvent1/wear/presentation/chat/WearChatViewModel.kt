package com.arekalov.aiadvent1.wear.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arekalov.aiadvent1.domain.model.ChatRequest
import com.arekalov.aiadvent1.domain.model.Message
import com.arekalov.aiadvent1.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WearChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showInputDialog: Boolean = false
)

class WearChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearChatUiState())
    val uiState: StateFlow<WearChatUiState> = _uiState.asStateFlow()

    init {
        val welcomeMessage = Message(
            id = UUID.randomUUID().toString(),
            text = "Привет! Я AI-анекдотчик. Расскажи ситуацию!",
            isUser = false
        )
        _uiState.update { it.copy(messages = listOf(welcomeMessage)) }
    }

    fun showInputDialog() {
        _uiState.update { it.copy(showInputDialog = true) }
    }

    fun hideInputDialog() {
        _uiState.update { it.copy(showInputDialog = false) }
    }

    fun sendMessage(text: String) {
        if (text.isEmpty() || _uiState.value.isLoading) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                showInputDialog = false,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val request = ChatRequest(
                userMessage = text,
                conversationHistory = _uiState.value.messages.dropLast(1)
            )

            repository.sendMessage(request)
                .onSuccess { response ->
                    val assistantMessage = Message(
                        id = UUID.randomUUID().toString(),
                        text = response,
                        isUser = false
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Ошибка"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

