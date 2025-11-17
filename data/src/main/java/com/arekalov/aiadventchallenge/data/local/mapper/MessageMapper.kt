package com.arekalov.aiadventchallenge.data.local.mapper

import com.arekalov.aiadventchallenge.data.local.entity.MessageEntity
import com.arekalov.aiadventchallenge.domain.model.Message

/**
 * Маппер для преобразования между доменными моделями и сущностями базы данных
 */

/**
 * Преобразовать MessageEntity в доменную модель Message
 */
fun MessageEntity.toDomainModel(): Message = Message(
    id = id,
    text = text,
    isUser = isUser,
    timestamp = timestamp,
    category = category,
    totalTokens = totalTokens,
    metrics = metrics,
    isSummary = isSummary,
    summarizedCount = summarizedCount
)

/**
 * Преобразовать доменную модель Message в MessageEntity
 * @param conversationId ID разговора, к которому относится сообщение
 */
fun Message.toEntity(conversationId: Long): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    text = text,
    isUser = isUser,
    timestamp = timestamp,
    category = category,
    totalTokens = totalTokens,
    metrics = metrics,
    isSummary = isSummary,
    summarizedCount = summarizedCount
)

/**
 * Преобразовать список MessageEntity в список доменных моделей Message
 */
fun List<MessageEntity>.toDomainModels(): List<Message> = map { it.toDomainModel() }

/**
 * Преобразовать список доменных моделей Message в список MessageEntity
 */
fun List<Message>.toEntities(conversationId: Long): List<MessageEntity> = 
    map { it.toEntity(conversationId) }

