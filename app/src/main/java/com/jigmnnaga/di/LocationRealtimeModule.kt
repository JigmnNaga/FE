package com.jigmnnaga.di

import com.jigmnnaga.core.websocket.StompClient
import com.jigmnnaga.data.repository.StompLocationRealtime
import com.jigmnnaga.domain.repository.LocationRealtime
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationRealtimeModule {

    @Provides
    @Singleton
    fun provideLocationRealtime(stompClient: StompClient, json: Json): LocationRealtime =
        StompLocationRealtime(stompClient, json)
}

/** Hilt가 직접 만들지 않는 이동 화면 ViewModel에 실시간 위치 공유를 넘겨 주기 위한 진입점이다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationRealtimeEntryPoint {
    fun locationRealtime(): LocationRealtime
}
