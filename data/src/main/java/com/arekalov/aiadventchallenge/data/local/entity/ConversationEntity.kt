package com.arekalov.aiadventchallenge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность для хранения разговоров (conversations)
 * Каждый conversation - это отдельная сессия диалога с AI
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Название разговора (генерируется автоматически или задаётся пользователем)
     */
    val title: String,
    
    /**
     * Временная метка создания разговора
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Временная метка последнего обновления разговора
     */
    val updatedAt: Long = System.currentTimeMillis(),
    
    /**
     * Текущий этап (stage) разговора
     */
    val currentStage: String = "Сбор_ситуации",
    
    /**
     * Активен ли разговор (для возможности архивации)
     */
    val isActive: Boolean = true
)

