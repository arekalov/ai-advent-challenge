package com.arekalov.aiadventchallenge.data.di

import com.arekalov.aiadventchallenge.core.di.AppScope
import com.arekalov.aiadventchallenge.data.remote.api.YandexGptApi
import com.arekalov.aiadventchallenge.data.repository.ChatRepositoryImpl
import com.arekalov.aiadventchallenge.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient

@Module(includes = [NetworkModule::class, LLMProviderModule::class, DatabaseModule::class])
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
            return com.arekalov.aiadventchallenge.data.BuildConfig.YANDEX_API_KEY
        }

        @Provides
        @AppScope
        @FolderId
        fun provideFolderId(): String {
            return com.arekalov.aiadventchallenge.data.BuildConfig.YANDEX_FOLDER_ID
        }
    }
}

