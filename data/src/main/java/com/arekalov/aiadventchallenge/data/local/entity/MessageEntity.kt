package com.arekalov.aiadventchallenge.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.arekalov.aiadventchallenge.domain.model.ModelMetrics

/**
 * Сущность для хранения сообщений в разговорах
 * Каждое сообщение привязано к конкретному разговору
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE // При удалении разговора удаляются все сообщения
        )
    ],
    indices = [
        Index(value = ["conversationId"]), // Индекс для быстрого поиска сообщений по conversationId
        Index(value = ["timestamp"]) // Индекс для сортировки по времени
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    
    /**
     * ID разговора, к которому относится сообщение
     */
    val conversationId: Long,
    
    /**
     * Текст сообщения
     */
    val text: String,
    
    /**
     * Является ли сообщение от пользователя
     */
    val isUser: Boolean,
    
    /**
     * Временная метка создания сообщения
     */
    val timestamp: Long,
    
    /**
     * Категория сообщения (этап в процессе генерации анекдотов)
     */
    val category: String,
    
    /**
     * Количество токенов (если доступно)
     */
    val totalTokens: Int? = null,
    
    /**
     * Метрики модели (сохраняются в JSON формате)
     */
    val metrics: ModelMetrics? = null,
    
    /**
     * Флаг, что это сжатое сообщение (summary)
     */
    val isSummary: Boolean = false,
    
    /**
     * Количество сообщений, сжатых в это summary
     */
    val summarizedCount: Int = 0
)

