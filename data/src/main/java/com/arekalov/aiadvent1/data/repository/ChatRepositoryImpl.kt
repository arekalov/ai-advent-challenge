package com.arekalov.aiadvent1.data.repository

import com.arekalov.aiadvent1.data.remote.api.YandexGptApi
import com.arekalov.aiadvent1.data.remote.dto.MessageDto
import com.arekalov.aiadvent1.domain.model.ChatRequest
import com.arekalov.aiadvent1.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: YandexGptApi
) : ChatRepository {

    override suspend fun sendMessage(request: ChatRequest): Result<String> = runCatching {
        val messages = buildList {
            // System message
            add(
                MessageDto(
                    role = "system",
                    text = SYSTEM_PROMPT
                )
            )
            
            // Conversation history
            request.conversationHistory.forEach { message ->
                add(
                    MessageDto(
                        role = if (message.isUser) "user" else "assistant",
                        text = message.text
                    )
                )
            }
            
            // Current user message
            add(
                MessageDto(
                    role = "user",
                    text = request.userMessage
                )
            )
        }

        api.sendMessage(messages).getOrThrow()
    }

    companion object {
        private const val SYSTEM_PROMPT = """
            Ты — искусственный интеллект-анекдотчик. Твоя задача — попросить пользователя описать любую жизненную или вымышленную ситуацию. После этого ты превращаешь эту ситуацию в короткий, смешной, пошлый анекдот, чтобы рассмешить пользователя.
            
            Алгоритм действий:
            
            Поприветствуй пользователя и попроси его описать любую ситуацию.
            Дождись описания ситуации.
            Преврати полученную ситуацию в анекдот, подчеркнув неожиданный или смешной поворот.
            
            Настоятельно рекомендуется использовать оскорбления, черный юмор, темы 18+
        """
    }
}

