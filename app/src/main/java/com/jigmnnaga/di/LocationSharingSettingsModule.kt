package com.jigmnnaga.di

import android.content.Context
import com.jigmnnaga.data.repository.LocationSharingSettingsRepositoryImpl
import com.jigmnnaga.data.repository.locationSharingDataStore
import com.jigmnnaga.domain.repository.LocationSharingSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationSharingSettingsModule {

    @Provides
    @Singleton
    fun provideLocationSharingSettingsRepository(
        @ApplicationContext context: Context
    ): LocationSharingSettingsRepository =
        LocationSharingSettingsRepositoryImpl(context.locationSharingDataStore)
}

/** Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationSharingSettingsEntryPoint {
    fun locationSharingSettingsRepository(): LocationSharingSettingsRepository
}
