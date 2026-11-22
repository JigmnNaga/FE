package com.jigmnnaga.di

import android.content.Context
import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.LocationSharingSettingsApi
import com.jigmnnaga.data.repository.LocationSharingLocalStore
import com.jigmnnaga.data.repository.LocationSharingSettingsRepositoryImpl
import com.jigmnnaga.data.repository.locationSharingDataStore
import com.jigmnnaga.domain.repository.LocationSharingSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationSharingSettingsModule {

    @Provides
    @Singleton
    fun provideLocationSharingSettingsApi(retrofit: Retrofit): LocationSharingSettingsApi =
        retrofit.create(LocationSharingSettingsApi::class.java)

    @Provides
    @Singleton
    fun provideLocationSharingSettingsRepository(
        @ApplicationContext context: Context,
        api: LocationSharingSettingsApi,
        tokenManager: TokenManager,
        json: Json
    ): LocationSharingSettingsRepository = LocationSharingSettingsRepositoryImpl(
        api = api,
        tokenManager = tokenManager,
        json = json,
        localStore = LocationSharingLocalStore(context.locationSharingDataStore)
    )
}

/** Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationSharingSettingsEntryPoint {
    fun locationSharingSettingsRepository(): LocationSharingSettingsRepository
}
