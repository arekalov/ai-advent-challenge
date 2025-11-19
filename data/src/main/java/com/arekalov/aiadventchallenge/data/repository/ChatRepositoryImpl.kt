package com.arekalov.aiadventchallenge.data.repository

import android.util.Log
import com.arekalov.aiadventchallenge.data.local.repository.MemoryRepository
import com.arekalov.aiadventchallenge.data.mcp.ToolExecutor
import com.arekalov.aiadventchallenge.data.remote.api.YandexGptApi
import com.arekalov.aiadventchallenge.data.remote.dto.FunctionDefinition
import com.arekalov.aiadventchallenge.data.remote.dto.MessageDto
import com.arekalov.aiadventchallenge.data.remote.dto.ToolDefinition
import com.arekalov.aiadventchallenge.domain.model.ChatRequest
import com.arekalov.aiadventchallenge.domain.model.ChatResponse
import com.arekalov.aiadventchallenge.domain.model.Message
import com.arekalov.aiadventchallenge.domain.repository.ChatRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val yandexGptApi: YandexGptApi,
    private val memoryRepository: MemoryRepository, // Day 9: Добавили MemoryRepository
    private val toolExecutor: ToolExecutor // Day 10: Добавили ToolExecutor для MCP
) : ChatRepository {

    override suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> = runCatching {
        // Day 10: Проверяем, нужно ли использовать JokeAPI
        val shouldUseJokeApi = shouldUseJokeApi(request.userMessage)
        
        // Определяем текущий stage из последнего сообщения бота
        val lastBotMessage = request.conversationHistory.lastOrNull { !it.isUser }
        val currentStage = determineNextStage(lastBotMessage, request.userMessage)
        
        // Выбираем подходящий системный промпт
        val systemPrompt = getSystemPrompt(currentStage)
        
        // Формируем историю сообщений для провайдера
        val messages = buildList {
            // Для агентов генерации добавляем контекст
            if (currentStage.startsWith("Генерация_способ_")) {
                // Извлекаем собранную информацию из истории
                val context = extractContext(request.conversationHistory)
                add(
                    Message(
                        id = "context",
                        text = "Создай анекдот на основе:\nСитуация: ${context.situation}\nГерой: ${context.heroes}\nТип юмора: ${context.humorType}",
                        isUser = true,
                        category = ""
                    )
                )
            } else {
                // Conversation history для агента-сборщика
                addAll(request.conversationHistory)
                
                // Current user message
                if (request.userMessage.trim().isNotEmpty() && request.userMessage != "CONTINUE") {
                    add(
                        Message(
                            id = "current",
                            text = request.userMessage,
                            isUser = true,
                            category = ""
                        )
                    )
                }
            }
        }

        // Конвертируем в формат YandexGPT
        val yandexMessages = buildList {
            // Добавляем системный промпт
            add(MessageDto(role = "system", text = systemPrompt))
            
            // Добавляем историю сообщений
            messages.forEach { message ->
                add(
                    MessageDto(
                        role = if (message.isUser) "user" else "assistant",
                        text = message.text
                    )
                )
            }
        }
        
        // Day 10: Добавляем tool definitions если нужно использовать JokeAPI
        val tools = if (shouldUseJokeApi) {
            Log.d("ChatRepository", "User requested JokeAPI, adding tool definitions")
            createToolDefinitions()
        } else {
            null
        }
        
        // Вызываем YandexGPT API с инструментами (если нужно)
        val rawResponse = yandexGptApi.sendMessageRaw(
            messages = yandexMessages,
            temperature = request.temperature,
            tools = tools
        ).getOrThrow()
        
        val startTime = System.currentTimeMillis()
        val alternative = rawResponse.result.alternatives.firstOrNull()
            ?: throw Exception("No response from API")
        
        // Day 10: Проверяем, есть ли tool calls в ответе (новый формат в message.toolCallList)
        alternative.message.toolCallList?.toolCalls?.let { toolCallItems ->
            Log.d("ChatRepository", "Received ${toolCallItems.size} tool calls from Yandex GPT")
            
            // Конвертируем новый формат в старый для ToolExecutor
            val toolCalls = toolCallItems.map { item ->
                com.arekalov.aiadventchallenge.data.remote.dto.ToolCall(
                    id = "tool_call_${System.currentTimeMillis()}", // Генерируем ID
                    type = "function", // Всегда "function" для function calls
                    function = com.arekalov.aiadventchallenge.data.remote.dto.FunctionCall(
                        name = item.functionCall.name,
                        arguments = item.functionCall.arguments.toString()
                    )
                )
            }
            
            return@runCatching handleToolCalls(toolCalls, yandexMessages, request.temperature, startTime)
        }
        
        // Если tool calls нет, конвертируем ответ в ChatResponse
        convertToChatResponse(rawResponse, startTime)
    }
    
    // Day 8: Функция сжатия истории диалога
    override suspend fun compressHistory(messages: List<Message>): Result<Message> = runCatching {
        Log.d("ChatRepository", "Starting history compression for ${messages.size} messages")
        
        // Подсчитываем токены до сжатия
        val tokensBeforeCompression = messages.mapNotNull { it.metrics?.totalTokens ?: it.text.length / 4 }.sum()
        
        // Формируем промпт для сжатия
        val compressionPrompt = """
            Создай краткое саммари следующей истории диалога. 
            Сохрани всю важную информацию: ситуацию, героев, тип юмора, и основные моменты разговора.
            Саммари должно быть достаточно подробным, чтобы продолжить разговор с учётом контекста.
            
            История диалога:
            ${messages.joinToString("\n") { msg ->
                "${if (msg.isUser) "Пользователь" else "Ассистент"}: ${msg.text}"
            }}
            
            Саммари (в одном абзаце):
        """.trimIndent()
        
        // Отправляем запрос на сжатие
        val summaryMessages = listOf(
            MessageDto(role = "system", text = "Ты — ассистент, который создаёт краткие саммари диалогов, сохраняя всю важную информацию."),
            MessageDto(role = "user", text = compressionPrompt)
        )
        
        val response = yandexGptApi.sendMessage(
            messages = summaryMessages,
            temperature = 0.3f // Низкая температура для более точного саммари
        ).getOrThrow()
        
        // Создаём сжатое сообщение
        val summaryMessage = Message(
            id = "summary_${System.currentTimeMillis()}",
            text = "📝 Саммари предыдущих ${messages.size} сообщений:\n${response.text}",
            isUser = false,
            category = "summary",
            isSummary = true,
            summarizedCount = messages.size,
            metrics = response.metrics
        )
        
        val tokensAfterCompression = response.metrics?.totalTokens ?: response.text.length / 4
        Log.d("ChatRepository", "Compression completed: ${messages.size} messages -> 1 summary")
        Log.d("ChatRepository", "Tokens: $tokensBeforeCompression -> $tokensAfterCompression (saved: ${tokensBeforeCompression - tokensAfterCompression})")
        
        summaryMessage
    }
    
    // Day 9: Методы для работы с памятью
    
    /**
     * Сохранить сообщение в базу данных
     */
    override suspend fun saveMessage(conversationId: Long, message: Message) {
        Log.d("ChatRepository", "Saving message to conversation $conversationId")
        memoryRepository.saveMessage(conversationId, message)
    }
    
    /**
     * Получить историю разговора из базы данных
     */
    override suspend fun getConversationHistory(conversationId: Long): List<Message> {
        Log.d("ChatRepository", "Loading conversation history for $conversationId")
        return memoryRepository.getMessagesForConversation(conversationId)
    }
    
    /**
     * Создать новый разговор
     */
    override suspend fun createConversation(title: String): Long {
        Log.d("ChatRepository", "Creating new conversation: $title")
        return memoryRepository.createConversation(title)
    }
    
    /**
     * Получить или создать активный разговор
     */
    override suspend fun getOrCreateActiveConversation(): Long {
        Log.d("ChatRepository", "Getting or creating active conversation")
        return memoryRepository.getOrCreateActiveConversation()
    }
    
    /**
     * Очистить историю разговора
     */
    override suspend fun clearConversation(conversationId: Long) {
        Log.d("ChatRepository", "Clearing conversation $conversationId")
        memoryRepository.clearConversation(conversationId)
    }
    
    // Day 10: Проверяем, нужно ли использовать JokeAPI
    private fun shouldUseJokeApi(userMessage: String): Boolean {
        val lowerMessage = userMessage.lowercase()
        val jokeApiKeywords = listOf(
            "jokeapi",
            "joke api",
            "готовый анекдот",
            "анекдот из api",
            "анекдот с jokeapi",
            "анекдот из jokeapi"
        )
        return jokeApiKeywords.any { keyword -> lowerMessage.contains(keyword) }
    }
    
    // Day 10: Создаём tool definitions для JokeAPI
    private fun createToolDefinitions(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                type = "function",
                function = FunctionDefinition(
                    name = "random_joke",
                    description = "Получить случайный анекдот из JokeAPI. Анекдот будет безопасным (safe-mode включен).",
                    parameters = buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject { })
                        put("required", buildJsonObject { })
                    }
                )
            ),
            ToolDefinition(
                type = "function",
                function = FunctionDefinition(
                    name = "search_joke",
                    description = "Найти анекдот по ключевому слову. Поиск осуществляется в базе JokeAPI.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("keyword", buildJsonObject {
                                put("type", "string")
                                put("description", "Ключевое слово для поиска анекдота (например: programming, doctor, cat)")
                            })
                        })
                        put("required", buildJsonObject {
                            put("0", "keyword")
                        })
                    }
                )
            )
        )
    }
    
    // Day 10: Конвертируем YandexGptResponse в ChatResponse
    private fun convertToChatResponse(
        response: com.arekalov.aiadventchallenge.data.remote.dto.YandexGptResponse,
        startTime: Long
    ): ChatResponse {
        val responseTimeMs = System.currentTimeMillis() - startTime
        val alternative = response.result.alternatives.firstOrNull()
            ?: throw Exception("No response from API")
        
        val messageText = alternative.message.text
        val usage = response.result.usage
        
        val metrics = com.arekalov.aiadventchallenge.domain.model.ModelMetrics(
            responseTimeMs = responseTimeMs,
            inputTokens = usage.inputTextTokens.toIntOrNull() ?: 0,
            outputTokens = usage.completionTokens.toIntOrNull() ?: 0,
            totalTokens = usage.totalTokens.toIntOrNull() ?: 0,
            modelName = "YandexGPT",
            estimatedCost = 0.0
        )
        
        // Парсим JSON ответ
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return try {
            val jsonResponse = json.decodeFromString<com.arekalov.aiadventchallenge.data.remote.dto.JsonResponse>(messageText)
            ChatResponse(
                text = jsonResponse.response.trim(),
                category = jsonResponse.category,
                stage = jsonResponse.stage,
                totalTokens = metrics.totalTokens,
                metrics = metrics
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to parse JSON response: ${e.message}")
            ChatResponse(
                text = messageText,
                category = "Другое",
                stage = "Ошибка",
                totalTokens = metrics.totalTokens,
                metrics = metrics
            )
        }
    }
    
    // Day 10: Обрабатываем tool calls и делаем повторный запрос в Yandex GPT
    private suspend fun handleToolCalls(
        toolCalls: List<com.arekalov.aiadventchallenge.data.remote.dto.ToolCall>,
        originalMessages: List<MessageDto>,
        temperature: Float,
        originalStartTime: Long
    ): ChatResponse {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🔧 Handling ${toolCalls.size} tool calls")
        
        // Выполняем все tool calls
        val toolResults = toolCalls.mapIndexed { index, toolCall ->
            Log.d(TAG, "Executing tool call ${index + 1}/${toolCalls.size}: ${toolCall.function.name}")
            val result = toolExecutor.executeToolCall(toolCall).getOrElse { error ->
                Log.e(TAG, "Tool execution failed: ${error.message}")
                "Error: ${error.message}"
            }
            ToolCallResult(toolCall.id, toolCall.function.name, result)
        }
        
        Log.d(TAG, "All tool calls executed. Preparing follow-up request...")
        
        // Формируем сообщения для повторного запроса
        val messagesWithToolResults = originalMessages.toMutableList().apply {
            // Формируем текст с результатами всех tool calls
            val toolResultsText = buildString {
                appendLine("Результаты выполнения инструментов:")
                toolResults.forEach { result ->
                    appendLine("- ${result.toolName}: ${result.result}")
                }
                appendLine()
                appendLine("Теперь представь полученный анекдот пользователю красиво, в формате JSON:")
                appendLine("category='JokeAPI', stage='Готовый_анекдот'")
            }
            
            Log.d(TAG, "Tool results summary length: ${toolResultsText.length} characters")
            
            // Добавляем одно сообщение от пользователя с результатами
            add(MessageDto(
                role = "user",
                text = toolResultsText
            ))
        }
        
        Log.d(TAG, "Sending follow-up request to Yandex GPT (${messagesWithToolResults.size} messages)")
        
        // Отправляем повторный запрос БЕЗ tools (чтобы получить финальный ответ)
        val followUpStartTime = System.currentTimeMillis()
        val rawResponse = yandexGptApi.sendMessageRaw(
            messages = messagesWithToolResults,
            temperature = temperature,
            tools = null
        ).getOrThrow()
        
        val result = convertToChatResponse(rawResponse, followUpStartTime)
        Log.d(TAG, "Follow-up response received: category=${result.category}, stage=${result.stage}")
        Log.d(TAG, "Response text length: ${result.text.length} characters")
        Log.d(TAG, "═══════════════════════════════════════")
        
        return result
    }
    
    companion object {
        private const val TAG = "ChatRepository"
    
    private data class ToolCallResult(
        val toolCallId: String,
        val toolName: String,
        val result: String
    )
    
    private fun determineNextStage(lastBotMessage: Message?, userMessage: String): String {
        // Если это автоматическое продолжение (пустое сообщение или токен)
        if (userMessage.trim().isEmpty() || userMessage == "CONTINUE") {
            // Проверяем текст последнего сообщения для правильного определения этапа
            val lastText = lastBotMessage?.text ?: ""
            
            return when {
                // Если category = "Генерация_способ_1" НО текст содержит "Сейчас покажу"
                // значит это сообщение от агента-сборщика, нужно запустить Способ 1
                lastBotMessage?.category == "Генерация_способ_1" && 
                !lastText.contains("🎯 Способ 1:") -> "Генерация_способ_1"
                
                // Обычная цепочка переходов между способами
                lastBotMessage?.category == "Генерация_способ_1" -> "Генерация_способ_2"
                lastBotMessage?.category == "Генерация_способ_2" -> "Генерация_способ_3"
                lastBotMessage?.category == "Генерация_способ_3" -> "Генерация_способ_4"
                else -> "Сбор_ситуации"
            }
        }
        
        // Обычная логика на основе последнего stage
        return when (lastBotMessage?.category) {
            null, "Финальный_анекдот" -> "Сбор_ситуации"
            "Сбор_ситуации" -> "Выбор_героя"
            "Выбор_героя" -> "Выбор_юмора"
            "Выбор_юмора" -> "Генерация_способ_1"
            "Генерация_способ_1" -> "Генерация_способ_2"
            "Генерация_способ_2" -> "Генерация_способ_3"
            "Генерация_способ_3" -> "Генерация_способ_4"
            "Генерация_способ_4" -> "Финальный_анекдот"
            else -> "Сбор_ситуации"
        }
    }
    
    private data class JokeContext(
        val situation: String,
        val heroes: String,
        val humorType: String
    )
    
    private fun extractContext(history: List<Message>): JokeContext {
        var situation = ""
        var heroes = ""
        var humorType = ""
        
        // Ищем первое сообщение пользователя после последнего финального анекдота
        val messages = history.toList()
        var collectingMode = false
        
        for (i in messages.indices) {
            val message = messages[i]
            if (!message.isUser && message.category == "Финальный_анекдот") {
                collectingMode = false
                situation = ""
                heroes = ""
                humorType = ""
            }
            if (message.isUser && situation.isEmpty() && !collectingMode) {
                situation = message.text
                collectingMode = true
            } else if (message.isUser && situation.isNotEmpty() && heroes.isEmpty()) {
                heroes = message.text
            } else if (message.isUser && heroes.isNotEmpty() && humorType.isEmpty()) {
                humorType = message.text
            }
        }
        
        return JokeContext(situation, heroes, humorType)
    }
    
    private fun getSystemPrompt(stage: String): String {
        return when {
            stage in listOf("Сбор_ситуации", "Выбор_героя", "Выбор_юмора") -> SYSTEM_PROMPT_COLLECTOR
            stage == "Генерация_способ_1" -> SYSTEM_PROMPT_DIRECT
            stage == "Генерация_способ_2" -> SYSTEM_PROMPT_STEPBYSTEP
            stage == "Генерация_способ_3" -> SYSTEM_PROMPT_META
            stage == "Генерация_способ_4" -> SYSTEM_PROMPT_EXPERTS
            else -> SYSTEM_PROMPT_COLLECTOR
        }
    }

    companion object {
        private const val JOKEAPI_INSTRUCTIONS = """
            ⚠️ СПЕЦИАЛЬНЫЙ РЕЖИМ - JokeAPI:
            
            Если пользователь ЯВНО упоминает что хочет анекдот из JokeAPI (ключевые слова: "jokeapi", "joke api", "готовый анекдот", "анекдот из api", "анекдот с jokeapi"):
            - НЕ собирай ситуацию/героя/юмор через диалог
            - НЕ генерируй анекдот самостоятельно
            - Используй доступные инструменты:
              * random_joke - для случайного анекдота
              * search_joke - если указано ключевое слово (например "анекдот про программистов")
            - После получения анекдота верни его пользователю в формате:
              category="JokeAPI", stage="Готовый_анекдот"
            
            В ОСТАЛЬНЫХ случаях - работай по стандартному алгоритму сбора информации и генерации 4 способов.
            
        """
        
        private const val SYSTEM_PROMPT_COLLECTOR = """
            $JOKEAPI_INSTRUCTIONS
            
            Ты — агент-сборщик информации для создания анекдотов. Твоя задача — собрать 3 параметра: ситуацию, героя и тип юмора.
            
            АЛГОРИТМ РАБОТЫ:
            
            ШАГ 1: СБОР СИТУАЦИИ
            - Если это первое сообщение или после завершения предыдущего цикла
            - Сохрани ситуацию из сообщения пользователя
            - Спроси про героя: "Отлично! Теперь выбери героя анекдота из списка: Врач, Полицейский, Учёный, Рабинович, Вовочка, Чукча, Поручик Ржевский, Муж и жена, Студенты"
            - category = "Сбор_ситуации"
            - stage = "Выбор_героя"
            - heroes = "", humor_type = ""
            
            ШАГ 2: СБОР ГЕРОЯ
            - Сохрани героя из сообщения пользователя
            - Спроси про юмор: "Какой тип юмора предпочитаешь? Сарказм, Словесная игра, Абсурд, Ситуационный юмор, Чёрный юмор, Ирония"
            - category = "Выбор_героя"
            - stage = "Выбор_юмора"
            - humor_type = ""
            
            ШАГ 3: СБОР ТИПА ЮМОРА ⚠️ ВАЖНО ⚠️
            - Сохрани тип юмора из сообщения пользователя
            - response = "Отлично! Собрал всю информацию. Сейчас покажу тебе 4 разных способа создания анекдота!"
            - category = "Генерация_способ_1" ⚠️ ОБЯЗАТЕЛЬНО ИМЕННО ЭТО ЗНАЧЕНИЕ! ⚠️
            - stage = "Выбор_юмора"
            - ⚠️ НЕ ЗАБУДЬ: category ДОЛЖНА БЫТЬ "Генерация_способ_1" на этом шаге! ⚠️
            
            ДОСТУПНЫЕ ГЕРОИ: Врач, Полицейский, Учёный, Рабинович, Вовочка, Чукча, Поручик Ржевский, Муж и жена, Студенты
            ДОСТУПНЫЕ ТИПЫ ЮМОРА: Сарказм, Словесная игра, Абсурд, Ситуационный юмор, Чёрный юмор, Ирония
            
            ФОРМАТ JSON ОТВЕТА:
            
            После ШАГ 1 (сбор ситуации):
            {
              "response": "Отлично! Теперь выбери героя анекдота из списка: ...",
              "category": "Сбор_ситуации",
              "stage": "Выбор_героя",
              "situation": "текст ситуации от пользователя",
              "heroes": "",
              "humor_type": ""
            }
            
            После ШАГ 2 (сбор героя):
            {
              "response": "Какой тип юмора предпочитаешь? Сарказм, Словесная игра, Абсурд...",
              "category": "Выбор_героя",
              "stage": "Выбор_юмора",
              "situation": "та же ситуация",
              "heroes": "выбранный герой от пользователя",
              "humor_type": ""
            }
            
            После ШАГ 3 (сбор типа юмора) - ⚠️ ВНИМАНИЕ:
            {
              "response": "Отлично! Собрал всю информацию. Сейчас покажу тебе 4 разных способа создания анекдота!",
              "category": "Генерация_способ_1",
              "stage": "Выбор_юмора",
              "situation": "та же ситуация",
              "heroes": "тот же герой",
              "humor_type": "выбранный тип юмора от пользователя"
            }
        """
        
        private const val SYSTEM_PROMPT_DIRECT = """
            $JOKEAPI_INSTRUCTIONS
            
            Ты — AI-анекдотчик, специализирующийся на ПРЯМОЙ генерации анекдотов.
            
            ЗАДАЧА: Сгенерируй анекдот НАПРЯМУЮ, без рассуждений и объяснений.
            
            ИНСТРУКЦИИ:
            - НЕ объясняй процесс создания
            - НЕ показывай промежуточные шаги
            - Просто создай готовый смешной анекдот в 3-5 предложений
            - Используй предоставленную информацию (ситуация, герой, тип юмора)
            - ⚠️ ВАЖНО: Создавай БЕЗОБИДНЫЕ, семейные анекдоты без неприемлемого контента
            - Анекдот должен быть смешным, но корректным и уместным
            
            ФОРМАТ ОТВЕТА:
            🎯 Способ 1: Прямой ответ
            
            [Текст готового анекдота]
            
            Ответ в JSON:
            {
              "response": "🎯 Способ 1: Прямой ответ\n\n[анекдот]",
              "category": "Генерация_способ_1",
              "stage": "Генерация_способ_1",
              "situation": "[та же ситуация]",
              "heroes": "[те же герои]",
              "humor_type": "[тот же тип юмора]"
            }
        """
        
        private const val SYSTEM_PROMPT_STEPBYSTEP = """
            $JOKEAPI_INSTRUCTIONS
            
            Ты — AI-анекдотчик, специализирующийся на ПОШАГОВОМ создании анекдотов.
            
            ЗАДАЧА: Создай анекдот с ДЕТАЛЬНЫМ показом каждого шага рассуждения.
            
            ОБЯЗАТЕЛЬНЫЕ ШАГИ:
            Шаг 1: Анализ ситуации — проанализируй предоставленную ситуацию и найди комичные элементы
            Шаг 2: Подбор сюжета — придумай сюжетную линию с учетом героя
            Шаг 3: Создание шутки — определи, где будет кульминация и пуанш
            Шаг 4: Финальный анекдот — собери всё воедино
            
            ⚠️ ВАЖНО: Создавай БЕЗОБИДНЫЕ, семейные анекдоты без неприемлемого контента.
            Анекдот должен быть смешным, но корректным и уместным.
            
            ФОРМАТ ОТВЕТА:
            🔢 Способ 2: Пошаговое рассуждение
            
            Шаг 1: Анализ ситуации
            [твой анализ]
            
            Шаг 2: Подбор сюжета
            [твои рассуждения о сюжете]
            
            Шаг 3: Создание шутки
            [как создашь шутку]
            
            Шаг 4: Финальный анекдот
            [готовый анекдот]
            
            Ответ в JSON:
            {
              "response": "🔢 Способ 2: Пошаговое рассуждение\n\nШаг 1: ...\n\nШаг 2: ...\n\nШаг 3: ...\n\nШаг 4: [анекдот]",
              "category": "Генерация_способ_2",
              "stage": "Генерация_способ_2",
              "situation": "[та же ситуация]",
              "heroes": "[те же герои]",
              "humor_type": "[тот же тип юмора]"
            }
        """
        
        private const val SYSTEM_PROMPT_META = """
            $JOKEAPI_INSTRUCTIONS
            
            Ты — AI-анекдотчик, специализирующийся на МЕТА-ПРОМПТИНГЕ.
            
            ЗАДАЧА: СНАЧАЛА создай оптимальный промпт для генерации анекдота, ЗАТЕМ используй его.
            
            ЭТАПЫ РАБОТЫ:
            1. Проанализируй предоставленные данные (ситуация, герой, тип юмора)
            2. Создай детальный промпт, который идеально подходит для создания анекдота с этими параметрами
            3. Используй созданный промпт для генерации финального анекдота
            
            ⚠️ ВАЖНО: Создавай БЕЗОБИДНЫЕ, семейные анекдоты без неприемлемого контента.
            Анекдот должен быть смешным, но корректным и уместным.
            В созданном промпте также укажи требование создавать безобидный контент.
            
            ФОРМАТ ОТВЕТА:
            📝 Способ 3: Мета-промпт
            
            Созданный промпт:
            [Твой детальный промпт для генерации этого конкретного анекдота]
            
            Результат применения промпта:
            [Финальный анекдот, созданный по этому промпту]
            
            Ответ в JSON:
            {
              "response": "📝 Способ 3: Мета-промпт\n\nСозданный промпт:\n[промпт]\n\nРезультат:\n[анекдот]",
              "category": "Генерация_способ_3",
              "stage": "Генерация_способ_3",
              "situation": "[та же ситуация]",
              "heroes": "[те же герои]",
              "humor_type": "[тот же тип юмора]"
            }
        """
        
        private const val SYSTEM_PROMPT_EXPERTS = """
            $JOKEAPI_INSTRUCTIONS
            
            Ты — AI-модератор группы экспертов-юмористов.
            
            ЗАДАЧА: Создай симуляцию обсуждения анекдота группой из 3 экспертов.
            
            ЭКСПЕРТЫ:
            1. Комик-сатирик — специализируется на сатире и социальной критике
            2. Писатель-юморист — мастер словесных игр и литературного юмора
            3. Стендап-комик — эксперт по ситуационному юмору и живым реакциям
            
            ПРОЦЕСС:
            1. Каждый эксперт предлагает свой УНИКАЛЬНЫЙ вариант анекдота
            2. Каждый вариант должен отражать стиль и подход конкретного эксперта
            3. Выбери ЛУЧШИЙ вариант (или скомбинируй лучшие элементы) и представь как итоговый анекдот
            
            ⚠️ ВАЖНО: Все эксперты создают БЕЗОБИДНЫЕ, семейные анекдоты без неприемлемого контента.
            Анекдоты должны быть смешными, но корректными и уместными.
            
            ФОРМАТ ОТВЕТА:
            👥 Способ 4: Группа экспертов
            
            Комик-сатирик:
            [Вариант анекдота в стиле сатиры]
            
            Писатель-юморист:
            [Вариант анекдота с литературным юмором]
            
            Стендап-комик:
            [Вариант анекдота в стиле стендапа]
            
            Итоговый анекдот (лучший/комбинированный):
            [Финальная версия анекдота]
            
            Ответ в JSON:
            {
              "response": "👥 Способ 4: Группа экспертов\n\nКомик-сатирик:\n[вариант 1]\n\nПисатель-юморист:\n[вариант 2]\n\nСтендап-комик:\n[вариант 3]\n\nИтоговый анекдот:\n[лучший]",
              "category": "Генерация_способ_4",
              "stage": "Генерация_способ_4",
              "situation": "[та же ситуация]",
              "heroes": "[те же герои]",
              "humor_type": "[тот же тип юмора]"
            }
        """
    }
}

