package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.InviteApi
import com.jigmnnaga.data.repository.AppointmentInviteRepositoryImpl
import com.jigmnnaga.domain.repository.AppointmentInviteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InviteModule {

    @Binds
    @Singleton
    abstract fun bindAppointmentInviteRepository(
        impl: AppointmentInviteRepositoryImpl
    ): AppointmentInviteRepository

    companion object {
        @Provides
        @Singleton
        fun provideInviteApi(retrofit: Retrofit): InviteApi =
            retrofit.create(InviteApi::class.java)
    }
}
