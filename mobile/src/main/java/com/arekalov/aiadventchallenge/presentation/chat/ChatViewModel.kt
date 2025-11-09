package com.arekalov.aiadventchallenge.presentation.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arekalov.aiadventchallenge.R
import com.arekalov.aiadventchallenge.domain.model.ChatRequest
import com.arekalov.aiadventchallenge.domain.model.Message
import com.arekalov.aiadventchallenge.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _sideEffect = Channel<ChatSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        // Добавляем приветственное сообщение от агента
        val welcomeMessage = Message(
            id = UUID.randomUUID().toString(),
            text = context.getString(R.string.welcome_message),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            category = "",
        )
        _state.update { it.copy(messages = listOf(welcomeMessage)) }
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            ChatIntent.ClearError -> clearError()
        }
    }

    private fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun sendMessage(text: String) {
        val inputText = text.trim()
        if (inputText.isEmpty() || _state.value.isLoading) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            text = inputText,
            isUser = true,
            category = "",
        )

        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val request = ChatRequest(
                userMessage = inputText,
                conversationHistory = _state.value.messages.dropLast(1)
            )

            repository.sendMessage(request)
                .onSuccess { response ->
                    val assistantMessage = Message(
                        id = UUID.randomUUID().toString(),
                        text = response.text,
                        isUser = false,
                        category = response.category,
                        totalTokens = response.totalTokens
                    )
                    _state.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                    _sideEffect.send(ChatSideEffect.ScrollToBottom)
                    
                    // Автоматически запускаем цепочку генерации
                    if (shouldStartGeneration(response.category)) {
                        // После сбора информации запускаем Способ 1
                        viewModelScope.launch(Dispatchers.IO) {
                            kotlinx.coroutines.delay(500)
                            continueGeneration()
                        }
                    } else if (shouldContinueGeneration(response.category)) {
                        // Продолжаем цепочку способов 2-4
                        viewModelScope.launch(Dispatchers.IO) {
                            kotlinx.coroutines.delay(500)
                            continueGeneration()
                        }
                    }
                }
                .onFailure { exception ->
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(
                        ChatSideEffect.ShowError(
                            exception.message ?: context.getString(R.string.error_sending_message)
                        )
                    )
                }
        }
    }

    private fun clearError() {
        // Error теперь обрабатывается через SideEffect, ничего не нужно
    }
    
    private fun shouldStartGeneration(category: String): Boolean {
        // Запускаем цепочку генерации после того, как собрали всю информацию
        return category == "Генерация_способ_1"
    }
    
    private fun shouldContinueGeneration(category: String): Boolean {
        return category in listOf(
            "Генерация_способ_1",
            "Генерация_способ_2",
            "Генерация_способ_3"
        )
    }
    
    private fun continueGeneration() {
        viewModelScope.launch(Dispatchers.IO) {
            // Показываем, что бот думает
            _state.update { it.copy(isLoading = true) }
            
            val request = ChatRequest(
                userMessage = "CONTINUE",
                conversationHistory = _state.value.messages
            )
            
            repository.sendMessage(request)
                .onSuccess { response ->
                    val assistantMessage = Message(
                        id = UUID.randomUUID().toString(),
                        text = response.text,
                        isUser = false,
                        category = response.category,
                        totalTokens = response.totalTokens
                    )
                    _state.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                    _sideEffect.send(ChatSideEffect.ScrollToBottom)
                    
                    // Проверяем, нужно ли продолжить дальше
                    if (shouldContinueGeneration(response.category)) {
                        kotlinx.coroutines.delay(500)
                        continueGeneration()
                    } else if (response.category == "Генерация_способ_4") {
                        // После 4-го способа показываем финальное сообщение
                        kotlinx.coroutines.delay(500)
                        showFinalMessage()
                    }
                }
                .onFailure { exception ->
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(
                        ChatSideEffect.ShowError(
                            exception.message ?: context.getString(R.string.error_sending_message)
                        )
                    )
                }
        }
    }
    
    private fun showFinalMessage() {
        viewModelScope.launch(Dispatchers.Main) {
            val finalMessage = Message(
                id = UUID.randomUUID().toString(),
                text = "Вот и все 4 способа! Какой тебе понравился больше? 😊\n\nХочешь ещё анекдот? Опиши новую ситуацию!",
                isUser = false,
                category = "Финальный_анекдот",
                timestamp = System.currentTimeMillis()
            )
            _state.update {
                it.copy(messages = it.messages + finalMessage)
            }
            _sideEffect.send(ChatSideEffect.ScrollToBottom)
        }
    }
}

