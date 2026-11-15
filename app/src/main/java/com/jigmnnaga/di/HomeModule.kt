package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.data.repository.HomeRepositoryImpl
import com.jigmnnaga.domain.repository.HomeRepository
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
abstract class HomeModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        impl: HomeRepositoryImpl
    ): HomeRepository

    companion object {
        @Provides
        @Singleton
        fun provideMyAppointmentApi(retrofit: Retrofit): MyAppointmentApi =
            retrofit.create(MyAppointmentApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 홈 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeEntryPoint {
    fun homeRepository(): HomeRepository
}
