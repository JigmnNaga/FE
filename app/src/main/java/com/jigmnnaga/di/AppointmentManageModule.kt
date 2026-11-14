package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.AppointmentManageApi
import com.jigmnnaga.data.repository.AppointmentManageRepositoryImpl
import com.jigmnnaga.domain.repository.AppointmentManageRepository
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
abstract class AppointmentManageModule {

    @Binds
    @Singleton
    abstract fun bindAppointmentManageRepository(
        impl: AppointmentManageRepositoryImpl
    ): AppointmentManageRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppointmentManageApi(retrofit: Retrofit): AppointmentManageApi =
            retrofit.create(AppointmentManageApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppointmentManageEntryPoint {
    fun appointmentManageRepository(): AppointmentManageRepository
}
