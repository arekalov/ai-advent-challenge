package com.arekalov.aiadventchallenge.data.local.repository

import android.util.Log
import com.arekalov.aiadventchallenge.core.di.AppScope
import com.arekalov.aiadventchallenge.data.local.dao.ConversationDao
import com.arekalov.aiadventchallenge.data.local.dao.MessageDao
import com.arekalov.aiadventchallenge.data.local.entity.ConversationEntity
import com.arekalov.aiadventchallenge.data.local.mapper.toDomainModel
import com.arekalov.aiadventchallenge.data.local.mapper.toDomainModels
import com.arekalov.aiadventchallenge.data.local.mapper.toEntities
import com.arekalov.aiadventchallenge.data.local.mapper.toEntity
import com.arekalov.aiadventchallenge.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Репозиторий для управления памятью агента
 * Day 9: Внешняя память
 * 
 * Отвечает за:
 * - Сохранение и загрузку истории сообщений
 * - Управление разговорами (conversations)
 * - Персистентность данных между перезапусками
 */
@AppScope
class MemoryRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    
    companion object {
        private const val TAG = "MemoryRepository"
        private const val MAX_MESSAGES_IN_MEMORY = 50 // Максимум сообщений для одного разговора
    }
    
    /**
     * Создать новый разговор
     * @param title название разговора
     * @return ID созданного разговора
     */
    suspend fun createConversation(title: String = "Новый разговор"): Long {
        Log.d(TAG, "Creating new conversation: $title")
        val conversation = ConversationEntity(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return conversationDao.insertConversation(conversation)
    }
    
    /**
     * Получить или создать активный разговор
     * Если нет активных разговоров, создаётся новый
     */
    suspend fun getOrCreateActiveConversation(): Long {
        val lastConversation = conversationDao.getLastActiveConversation()
        
        return if (lastConversation != null) {
            Log.d(TAG, "Found active conversation: ${lastConversation.id}")
            lastConversation.id
        } else {
            Log.d(TAG, "No active conversations found, creating new one")
            createConversation("Разговор от ${System.currentTimeMillis()}")
        }
    }
    
    /**
     * Сохранить сообщение в базу данных
     */
    suspend fun saveMessage(conversationId: Long, message: Message) {
        Log.d(TAG, "Saving message to conversation $conversationId: ${message.text.take(50)}...")
        
        val entity = message.toEntity(conversationId)
        messageDao.insertMessage(entity)
        
        // Обновляем timestamp разговора
        conversationDao.updateConversationTimestamp(conversationId)
        
        // Проверяем лимит сообщений
        val messageCount = messageDao.getMessageCount(conversationId)
        if (messageCount > MAX_MESSAGES_IN_MEMORY) {
            Log.d(TAG, "Message limit exceeded ($messageCount > $MAX_MESSAGES_IN_MEMORY), keeping last messages")
            // Оставляем только последние сообщения
            messageDao.deleteOldMessages(conversationId, MAX_MESSAGES_IN_MEMORY)
        }
    }
    
    /**
     * Сохранить несколько сообщений
     */
    suspend fun saveMessages(conversationId: Long, messages: List<Message>) {
        Log.d(TAG, "Saving ${messages.size} messages to conversation $conversationId")
        
        val entities = messages.toEntities(conversationId)
        messageDao.insertMessages(entities)
        
        // Обновляем timestamp разговора
        conversationDao.updateConversationTimestamp(conversationId)
    }
    
    /**
     * Получить все сообщения для разговора
     */
    suspend fun getMessagesForConversation(conversationId: Long): List<Message> {
        Log.d(TAG, "Loading messages for conversation $conversationId")
        return messageDao.getMessagesForConversationSync(conversationId).toDomainModels()
    }
    
    /**
     * Получить все сообщения для разговора (Flow для реактивного наблюдения)
     */
    fun getMessagesForConversationFlow(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId)
            .map { entities -> entities.toDomainModels() }
    }
    
    /**
     * Получить последние N сообщений
     */
    suspend fun getLastNMessages(conversationId: Long, limit: Int): List<Message> {
        Log.d(TAG, "Loading last $limit messages for conversation $conversationId")
        return messageDao.getLastNMessages(conversationId, limit)
            .reversed() // Переворачиваем, чтобы получить правильный порядок
            .map { it.toDomainModel() }
    }
    
    /**
     * Обновить stage разговора
     */
    suspend fun updateConversationStage(conversationId: Long, stage: String) {
        Log.d(TAG, "Updating conversation $conversationId stage to: $stage")
        conversationDao.updateConversationStage(conversationId, stage)
    }
    
    /**
     * Удалить разговор
     */
    suspend fun deleteConversation(conversationId: Long) {
        Log.d(TAG, "Deleting conversation $conversationId")
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let { conversationDao.deleteConversation(it) }
    }
    
    /**
     * Архивировать разговор
     */
    suspend fun archiveConversation(conversationId: Long) {
        Log.d(TAG, "Archiving conversation $conversationId")
        conversationDao.archiveConversation(conversationId)
    }
    
    /**
     * Очистить все сообщения разговора
     */
    suspend fun clearConversation(conversationId: Long) {
        Log.d(TAG, "Clearing all messages for conversation $conversationId")
        messageDao.deleteMessagesForConversation(conversationId)
    }
    
    /**
     * Получить статистику разговора
     */
    suspend fun getConversationStats(conversationId: Long): ConversationStats {
        val messageCount = messageDao.getMessageCount(conversationId)
        val totalTokens = messageDao.getTotalTokensForConversation(conversationId) ?: 0
        
        return ConversationStats(
            conversationId = conversationId,
            messageCount = messageCount,
            totalTokens = totalTokens
        )
    }
    
    /**
     * Получить все активные разговоры
     */
    fun getAllActiveConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllActiveConversations()
    }
    
    /**
     * Получить последнее сообщение разговора
     */
    suspend fun getLastMessage(conversationId: Long): Message? {
        return messageDao.getLastMessage(conversationId)?.toDomainModel()
    }
}

/**
 * Статистика разговора
 */
data class ConversationStats(
    val conversationId: Long,
    val messageCount: Int,
    val totalTokens: Int
)

