package com.arekalov.aiadventchallenge.data.local.dao

import androidx.room.*
import com.arekalov.aiadventchallenge.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с разговорами
 */
@Dao
interface ConversationDao {
    
    /**
     * Получить все активные разговоры, отсортированные по времени последнего обновления
     */
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveConversations(): Flow<List<ConversationEntity>>
    
    /**
     * Получить последний активный разговор (suspend для прямого вызова)
     */
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastActiveConversation(): ConversationEntity?
    
    /**
     * Получить разговор по ID
     */
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: Long): ConversationEntity?
    
    /**
     * Получить разговор по ID (Flow для реактивного наблюдения)
     */
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationByIdFlow(conversationId: Long): Flow<ConversationEntity?>
    
    /**
     * Вставить новый разговор
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long
    
    /**
     * Обновить разговор
     */
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    /**
     * Удалить разговор
     */
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    /**
     * Архивировать разговор (установить isActive = false)
     */
    @Query("UPDATE conversations SET isActive = 0 WHERE id = :conversationId")
    suspend fun archiveConversation(conversationId: Long)
    
    /**
     * Обновить временную метку последнего обновления разговора
     */
    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :conversationId")
    suspend fun updateConversationTimestamp(conversationId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Обновить текущий stage разговора
     */
    @Query("UPDATE conversations SET currentStage = :stage, updatedAt = :timestamp WHERE id = :conversationId")
    suspend fun updateConversationStage(
        conversationId: Long, 
        stage: String, 
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Получить количество активных разговоров
     */
    @Query("SELECT COUNT(*) FROM conversations WHERE isActive = 1")
    suspend fun getActiveConversationsCount(): Int
    
    /**
     * Очистить все архивированные разговоры
     */
    @Query("DELETE FROM conversations WHERE isActive = 0")
    suspend fun clearArchivedConversations()
}

