package com.arekalov.aiadventchallenge.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arekalov.aiadventchallenge.data.local.converters.Converters
import com.arekalov.aiadventchallenge.data.local.dao.ConversationDao
import com.arekalov.aiadventchallenge.data.local.dao.MessageDao
import com.arekalov.aiadventchallenge.data.local.entity.ConversationEntity
import com.arekalov.aiadventchallenge.data.local.entity.MessageEntity

/**
 * Основная база данных приложения для хранения истории диалогов
 * 
 * Day 9: Внешняя память агента
 * - Сохранение всех сообщений и разговоров
 * - Персистентность между перезапусками приложения
 * - Возможность загрузки истории для контекста
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    
    /**
     * DAO для работы с разговорами
     */
    abstract fun conversationDao(): ConversationDao
    
    /**
     * DAO для работы с сообщениями
     */
    abstract fun messageDao(): MessageDao
    
    companion object {
        const val DATABASE_NAME = "ai_advent_chat_database"
    }
}

