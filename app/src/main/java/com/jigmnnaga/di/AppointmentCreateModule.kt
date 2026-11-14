package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.AppointmentCreateApi
import com.jigmnnaga.data.repository.AppointmentCreateRepositoryImpl
import com.jigmnnaga.domain.repository.AppointmentCreateRepository
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
abstract class AppointmentCreateModule {

    @Binds
    @Singleton
    abstract fun bindAppointmentCreateRepository(
        impl: AppointmentCreateRepositoryImpl
    ): AppointmentCreateRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppointmentCreateApi(retrofit: Retrofit): AppointmentCreateApi =
            retrofit.create(AppointmentCreateApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppointmentCreateEntryPoint {
    fun appointmentCreateRepository(): AppointmentCreateRepository
}
