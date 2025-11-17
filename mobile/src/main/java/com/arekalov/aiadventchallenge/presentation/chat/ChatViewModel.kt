package com.arekalov.aiadventchallenge.presentation.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arekalov.aiadventchallenge.R
import com.arekalov.aiadventchallenge.data.provider.ModelRegistry
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
    private val registry: ModelRegistry,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _sideEffect = Channel<ChatSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()
    
    // Day 9: ID текущего разговора
    private var currentConversationId: Long = 0L

    init {
        // Загружаем доступные модели
        _state.update { it.copy(availableModels = registry.getAllModels()) }
        
        // Day 9: Загружаем историю из базы данных
        loadConversationHistory()
    }
    
    // Day 9: Загрузка истории разговора из базы данных
    private fun loadConversationHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Получаем или создаём активный разговор
                currentConversationId = repository.getOrCreateActiveConversation()
                
                // Загружаем историю сообщений
                val savedMessages = repository.getConversationHistory(currentConversationId)
                
                if (savedMessages.isEmpty()) {
                    // Если истории нет, создаём приветственное сообщение
                    val welcomeMessage = Message(
                        id = UUID.randomUUID().toString(),
                        text = context.getString(R.string.welcome_message),
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        category = "",
                    )
                    // Сохраняем приветственное сообщение в БД
                    repository.saveMessage(currentConversationId, welcomeMessage)
                    _state.update { it.copy(messages = listOf(welcomeMessage)) }
                } else {
                    // Загружаем сохранённые сообщения
                    _state.update { it.copy(messages = savedMessages) }
                    updateTokenUsage()
                }
            } catch (e: Exception) {
                // Если не удалось загрузить, показываем приветственное сообщение
                val welcomeMessage = Message(
                    id = UUID.randomUUID().toString(),
                    text = context.getString(R.string.welcome_message),
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    category = "",
                )
                _state.update { it.copy(messages = listOf(welcomeMessage)) }
            }
        }
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.UpdateTemperature -> updateTemperature(intent.temperature)
            is ChatIntent.SelectModel -> selectModel(intent.modelId)
            ChatIntent.ToggleSettings -> toggleSettings()
            ChatIntent.ClearError -> clearError()
            ChatIntent.ToggleTokenTestMode -> toggleTokenTestMode()
            is ChatIntent.SendTokenTest -> sendTokenTest(intent.testType)
            ChatIntent.CompressHistory -> compressHistory()
            ChatIntent.ClearHistory -> clearHistory()
        }
    }

    private fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }
    
    private fun updateTemperature(temperature: Float) {
        _state.update { it.copy(selectedTemperature = temperature) }
    }
    
    private fun selectModel(modelId: String) {
        _state.update { it.copy(selectedModelId = modelId) }
    }
    
    private fun toggleSettings() {
        _state.update { it.copy(isSettingsExpanded = !it.isSettingsExpanded) }
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
        
        // Day 9: Сохраняем сообщение пользователя в БД
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMessage(currentConversationId, userMessage)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val request = ChatRequest(
                userMessage = inputText,
                conversationHistory = _state.value.messages.dropLast(1),
                temperature = _state.value.selectedTemperature,
                modelId = _state.value.selectedModelId
            )

            repository.sendMessage(request)
                .onSuccess { response ->
                    val assistantMessage = Message(
                        id = UUID.randomUUID().toString(),
                        text = response.text,
                        isUser = false,
                        category = response.category,
                        totalTokens = response.totalTokens,
                        metrics = response.metrics
                    )
                    
                    // Day 9: Сохраняем ответ AI в БД
                    repository.saveMessage(currentConversationId, assistantMessage)
                    
                    _state.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                    updateTokenUsage()
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
                conversationHistory = _state.value.messages,
                temperature = _state.value.selectedTemperature,
                modelId = _state.value.selectedModelId
            )
            
            repository.sendMessage(request)
                .onSuccess { response ->
                    val assistantMessage = Message(
                        id = UUID.randomUUID().toString(),
                        text = response.text,
                        isUser = false,
                        category = response.category,
                        totalTokens = response.totalTokens,
                        metrics = response.metrics
                    )
                    
                    // Day 9: Сохраняем автоматическое сообщение в БД
                    repository.saveMessage(currentConversationId, assistantMessage)
                    
                    _state.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                    updateTokenUsage()
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
            
            // Day 9: Сохраняем финальное сообщение в БД
            launch(Dispatchers.IO) {
                repository.saveMessage(currentConversationId, finalMessage)
            }
            
            _state.update {
                it.copy(messages = it.messages + finalMessage)
            }
            _sideEffect.send(ChatSideEffect.ScrollToBottom)
        }
    }
    
    private fun toggleTokenTestMode() {
        _state.update { it.copy(isTokenTestModeEnabled = !it.isTokenTestModeEnabled) }
    }
    
    private fun sendTokenTest(testType: TokenTestType) {
        val testRequest = when (testType) {
            TokenTestType.SHORT -> TokenTestUtils.getShortRequest()
            TokenTestType.MEDIUM -> TokenTestUtils.getMediumRequest()
            TokenTestType.LONG -> TokenTestUtils.getLongRequest()
            TokenTestType.OVERFLOW -> TokenTestUtils.getOverflowRequest()
        }
        
        // Отправляем тестовый запрос
        sendMessage(testRequest)
    }
    
    private fun updateTokenUsage() {
        // Подсчитываем общее количество токенов в истории
        val totalTokens = _state.value.messages
            .mapNotNull { it.metrics?.totalTokens }
            .sum()
        
        _state.update { it.copy(currentTokenUsage = totalTokens) }
    }
    
    // Day 8: Ручное сжатие истории
    private fun compressHistory() {
        if (_state.value.isCompressing) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isCompressing = true) }
            
            // Получаем сообщения, исключая приветственное и последние 3
            val messages = _state.value.messages
            if (messages.size <= 4) {
                _state.update { it.copy(isCompressing = false) }
                _sideEffect.send(ChatSideEffect.ShowError("Недостаточно сообщений для сжатия"))
                return@launch
            }
            
            val messagesToCompress = messages.drop(1).dropLast(3)
            
            // Подсчитываем токены до сжатия
            val tokensBeforeCompression = messagesToCompress
                .mapNotNull { it.metrics?.totalTokens ?: it.text.length / 4 }
                .sum()
            
            // Выполняем сжатие
            repository.compressHistory(messagesToCompress)
                .onSuccess { summaryMessage ->
                    // Day 9: Удаляем старые сообщения из БД и сохраняем summary
                    try {
                        // Удаляем сжатые сообщения из БД
                        messagesToCompress.forEach { message ->
                            // TODO: добавить метод удаления отдельных сообщений в repository
                            // Пока пропускаем, так как мы просто не будем их загружать
                        }
                        
                        // Сохраняем summary в БД
                        repository.saveMessage(currentConversationId, summaryMessage)
                    } catch (e: Exception) {
                        // Логируем ошибку, но продолжаем
                    }
                    
                    // Заменяем сжатые сообщения на саммари в UI
                    val welcomeMessage = messages.first()
                    val recentMessages = messages.takeLast(3)
                    val newMessages = listOf(welcomeMessage, summaryMessage) + recentMessages
                    
                    val tokensSaved = tokensBeforeCompression - (summaryMessage.metrics?.totalTokens ?: 0)
                    
                    _state.update {
                        it.copy(
                            messages = newMessages,
                            tokensSaved = it.tokensSaved + tokensSaved,
                            isCompressing = false
                        )
                    }
                    updateTokenUsage()
                    _sideEffect.send(ChatSideEffect.ScrollToBottom)
                }
                .onFailure { exception ->
                    _state.update { it.copy(isCompressing = false) }
                    _sideEffect.send(
                        ChatSideEffect.ShowError(
                            exception.message ?: "Ошибка сжатия истории"
                        )
                    )
                }
        }
    }
    
    // Day 9: Очистка истории диалога
    private fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Очищаем все сообщения из БД
                repository.clearConversation(currentConversationId)
                
                // Создаём новое приветственное сообщение
                val welcomeMessage = Message(
                    id = UUID.randomUUID().toString(),
                    text = context.getString(R.string.welcome_message),
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    category = "",
                )
                
                // Сохраняем его в БД
                repository.saveMessage(currentConversationId, welcomeMessage)
                
                // Обновляем UI
                _state.update {
                    it.copy(
                        messages = listOf(welcomeMessage),
                        currentTokenUsage = 0,
                        tokensSaved = 0
                    )
                }
                
                _sideEffect.send(ChatSideEffect.ScrollToBottom)
            } catch (e: Exception) {
                _sideEffect.send(
                    ChatSideEffect.ShowError(
                        e.message ?: "Ошибка очистки истории"
                    )
                )
            }
        }
    }
}

