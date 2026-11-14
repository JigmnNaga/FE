package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.ScheduleApi
import com.jigmnnaga.data.repository.ScheduleRepositoryImpl
import com.jigmnnaga.domain.repository.ScheduleRepository
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
abstract class ScheduleModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        impl: ScheduleRepositoryImpl
    ): ScheduleRepository

    companion object {
        @Provides
        @Singleton
        fun provideScheduleApi(retrofit: Retrofit): ScheduleApi =
            retrofit.create(ScheduleApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScheduleEntryPoint {
    fun scheduleRepository(): ScheduleRepository
}
