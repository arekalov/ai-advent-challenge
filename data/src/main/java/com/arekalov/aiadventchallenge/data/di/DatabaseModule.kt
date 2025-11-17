package com.arekalov.aiadventchallenge.data.di

import android.content.Context
import androidx.room.Room
import com.arekalov.aiadventchallenge.core.di.AppScope
import com.arekalov.aiadventchallenge.data.local.dao.ConversationDao
import com.arekalov.aiadventchallenge.data.local.dao.MessageDao
import com.arekalov.aiadventchallenge.data.local.database.ChatDatabase
import dagger.Module
import dagger.Provides

/**
 * Dagger модуль для предоставления зависимостей Room database
 * Day 9: Внешняя память агента
 */
@Module
class DatabaseModule {
    
    /**
     * Предоставляет экземпляр ChatDatabase
     */
    @Provides
    @AppScope
    fun provideChatDatabase(context: Context): ChatDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ChatDatabase::class.java,
            ChatDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Для dev-версии, в проде нужны миграции
            .build()
    }
    
    /**
     * Предоставляет ConversationDao
     */
    @Provides
    @AppScope
    fun provideConversationDao(database: ChatDatabase): ConversationDao {
        return database.conversationDao()
    }
    
    /**
     * Предоставляет MessageDao
     */
    @Provides
    @AppScope
    fun provideMessageDao(database: ChatDatabase): MessageDao {
        return database.messageDao()
    }
}

