package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.ConfirmationApi
import com.jigmnnaga.data.repository.ConfirmationRepositoryImpl
import com.jigmnnaga.domain.repository.ConfirmationRepository
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
abstract class ConfirmationModule {

    @Binds
    @Singleton
    abstract fun bindConfirmationRepository(
        impl: ConfirmationRepositoryImpl
    ): ConfirmationRepository

    companion object {
        @Provides
        @Singleton
        fun provideConfirmationApi(retrofit: Retrofit): ConfirmationApi =
            retrofit.create(ConfirmationApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConfirmationEntryPoint {
    fun confirmationRepository(): ConfirmationRepository
}
