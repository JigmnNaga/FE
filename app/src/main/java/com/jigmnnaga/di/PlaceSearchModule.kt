package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.PlaceSearchApi
import com.jigmnnaga.data.repository.PlaceSearchRepositoryImpl
import com.jigmnnaga.domain.repository.PlaceSearchRepository
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
abstract class PlaceSearchModule {

    @Binds
    @Singleton
    abstract fun bindPlaceSearchRepository(
        impl: PlaceSearchRepositoryImpl
    ): PlaceSearchRepository

    companion object {
        @Provides
        @Singleton
        fun providePlaceSearchApi(retrofit: Retrofit): PlaceSearchApi =
            retrofit.create(PlaceSearchApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaceSearchEntryPoint {
    fun placeSearchRepository(): PlaceSearchRepository
}
