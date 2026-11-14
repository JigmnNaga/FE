package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.NotificationApi
import com.jigmnnaga.data.repository.NotificationRepositoryImpl
import com.jigmnnaga.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository

    companion object {
        @Provides
        @Singleton
        fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
            retrofit.create(NotificationApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 [com.jigmnnaga.presentation.notification.NotificationViewModel]에
 * 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun notificationRepository(): NotificationRepository
}
