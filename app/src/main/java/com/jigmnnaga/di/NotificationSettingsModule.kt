package com.jigmnnaga.di

import android.content.Context
import com.jigmnnaga.data.repository.NotificationSettingsLocalStore
import com.jigmnnaga.data.repository.NotificationSettingsRepositoryImpl
import com.jigmnnaga.data.repository.notificationSettingsDataStore
import com.jigmnnaga.domain.repository.NotificationSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationSettingsModule {

    @Provides
    @Singleton
    fun provideNotificationSettingsRepository(
        @ApplicationContext context: Context
    ): NotificationSettingsRepository = NotificationSettingsRepositoryImpl(
        localStore = NotificationSettingsLocalStore(context.notificationSettingsDataStore)
    )
}

/** Hilt가 직접 만들지 않는 [com.jigmnnaga.presentation.mypage.notification.NotificationSettingsViewModel]에 저장소를 넘겨 주기 위한 진입점이다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationSettingsEntryPoint {
    fun notificationSettingsRepository(): NotificationSettingsRepository
}
