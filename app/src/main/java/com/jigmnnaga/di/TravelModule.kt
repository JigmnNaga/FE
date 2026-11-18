package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.TravelApi
import com.jigmnnaga.data.repository.TravelRepositoryImpl
import com.jigmnnaga.domain.repository.TravelRepository
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
abstract class TravelModule {

    @Binds
    @Singleton
    abstract fun bindTravelRepository(impl: TravelRepositoryImpl): TravelRepository

    companion object {
        @Provides
        @Singleton
        fun provideTravelApi(retrofit: Retrofit): TravelApi = retrofit.create(TravelApi::class.java)
    }
}

/** Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TravelEntryPoint {
    fun travelRepository(): TravelRepository
}
