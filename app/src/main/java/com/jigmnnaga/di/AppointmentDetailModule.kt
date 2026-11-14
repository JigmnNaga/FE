package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.AppointmentDetailApi
import com.jigmnnaga.data.repository.AppointmentDetailRepositoryImpl
import com.jigmnnaga.domain.repository.AppointmentDetailRepository
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
abstract class AppointmentDetailModule {

    @Binds
    @Singleton
    abstract fun bindAppointmentDetailRepository(
        impl: AppointmentDetailRepositoryImpl
    ): AppointmentDetailRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppointmentDetailApi(retrofit: Retrofit): AppointmentDetailApi =
            retrofit.create(AppointmentDetailApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppointmentDetailEntryPoint {
    fun appointmentDetailRepository(): AppointmentDetailRepository
}
