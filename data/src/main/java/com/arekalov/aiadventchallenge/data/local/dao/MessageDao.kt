package com.arekalov.aiadventchallenge.data.local.dao

import androidx.room.*
import com.arekalov.aiadventchallenge.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с сообщениями
 */
@Dao
interface MessageDao {
    
    /**
     * Получить все сообщения для конкретного разговора, отсортированные по времени
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>>
    
    /**
     * Получить все сообщения для конкретного разговора (suspend для одноразового получения)
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversationSync(conversationId: Long): List<MessageEntity>
    
    /**
     * Получить последние N сообщений для разговора
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastNMessages(conversationId: Long, limit: Int): List<MessageEntity>
    
    /**
     * Получить сообщение по ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    /**
     * Вставить новое сообщение
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    /**
     * Вставить несколько сообщений
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    /**
     * Обновить сообщение
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    /**
     * Удалить сообщение
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    /**
     * Удалить все сообщения для конкретного разговора
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)
    
    /**
     * Получить количество сообщений в разговоре
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int
    
    /**
     * Получить последнее сообщение в разговоре
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(conversationId: Long): MessageEntity?
    
    /**
     * Получить все summary-сообщения для разговора
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isSummary = 1 ORDER BY timestamp ASC")
    suspend fun getSummaryMessages(conversationId: Long): List<MessageEntity>
    
    /**
     * Удалить старые сообщения, оставив только последние N
     * Используется для очистки истории после компрессии
     */
    @Query("""
        DELETE FROM messages 
        WHERE conversationId = :conversationId 
        AND id NOT IN (
            SELECT id FROM messages 
            WHERE conversationId = :conversationId 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun deleteOldMessages(conversationId: Long, keepCount: Int)
    
    /**
     * Получить общее количество токенов для разговора
     */
    @Query("SELECT SUM(totalTokens) FROM messages WHERE conversationId = :conversationId AND totalTokens IS NOT NULL")
    suspend fun getTotalTokensForConversation(conversationId: Long): Int?
}

