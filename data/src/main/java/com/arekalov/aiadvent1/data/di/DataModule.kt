package com.arekalov.aiadvent1.data.di

import com.arekalov.aiadvent1.core.di.AppScope
import com.arekalov.aiadvent1.data.remote.api.YandexGptApi
import com.arekalov.aiadvent1.data.repository.ChatRepositoryImpl
import com.arekalov.aiadvent1.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient

@Module(includes = [NetworkModule::class])
abstract class DataModule {

    @Binds
    @AppScope
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    companion object {
        @Provides
        @AppScope
        fun provideYandexGptApi(
            httpClient: HttpClient,
            @ApiKey apiKey: String,
            @FolderId folderId: String
        ): YandexGptApi {
            return YandexGptApi(httpClient, apiKey, folderId)
        }

        @Provides
        @AppScope
        @ApiKey
        fun provideApiKey(): String {
            return com.arekalov.aiadvent1.data.BuildConfig.YANDEX_API_KEY
        }

        @Provides
        @AppScope
        @FolderId
        fun provideFolderId(): String {
            return com.arekalov.aiadvent1.data.BuildConfig.YANDEX_FOLDER_ID
        }
    }
}

