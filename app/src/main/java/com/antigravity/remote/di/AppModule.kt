package com.antigravity.remote.di

import android.content.Context
import com.antigravity.remote.data.local.SettingsDataStore
import com.antigravity.remote.data.remote.WebSocketService
import com.antigravity.remote.data.repository.AgentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideWebSocketService(): WebSocketService = WebSocketService()

    @Provides
    @Singleton
    fun provideAgentRepository(
        webSocketService: WebSocketService,
        settingsDataStore: SettingsDataStore
    ): AgentRepository = AgentRepository(webSocketService, settingsDataStore)
}