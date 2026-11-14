package com.jigmnnaga.di

import com.jigmnnaga.core.websocket.StompClient
import com.jigmnnaga.data.remote.api.ChatApi
import com.jigmnnaga.data.repository.ChatRepositoryImpl
import com.jigmnnaga.data.repository.StompChatRealtime
import com.jigmnnaga.data.repository.TokenMyMemberIdProvider
import com.jigmnnaga.domain.repository.ChatRealtime
import com.jigmnnaga.domain.repository.ChatRepository
import com.jigmnnaga.domain.repository.MyMemberIdProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMyMemberIdProvider(impl: TokenMyMemberIdProvider): MyMemberIdProvider

    companion object {
        @Provides
        @Singleton
        fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

        @Provides
        @Singleton
        fun provideChatRealtime(stompClient: StompClient, json: Json): ChatRealtime =
            StompChatRealtime(stompClient, json)
    }
}

/**
 * Hilt가 직접 만들지 않는 채팅 ViewModel에 저장소와 실시간 연결을 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatEntryPoint {
    fun chatRepository(): ChatRepository
    fun chatRealtime(): ChatRealtime
    fun myMemberIdProvider(): MyMemberIdProvider
}
