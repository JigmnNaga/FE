package com.jigmnnaga.di

import com.jigmnnaga.BuildConfig
import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.core.websocket.OkHttpStompTransportFactory
import com.jigmnnaga.core.websocket.StompClient
import com.jigmnnaga.core.websocket.stompServerUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebSocketModule {

    private const val PING_INTERVAL_SECONDS = 25L

    /** 앱 전체에서 STOMP 연결 하나를 같이 쓴다. 연결이 죽은 걸 알아채도록 주기적으로 ping을 보낸다. */
    @Provides
    @Singleton
    fun provideStompClient(okHttpClient: OkHttpClient, tokenManager: TokenManager): StompClient =
        StompClient(
            serverUrl = stompServerUrl(BuildConfig.BASE_URL),
            accessToken = { tokenManager.accessToken.first() },
            transportFactory = OkHttpStompTransportFactory(
                okHttpClient.newBuilder().pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS).build()
            ),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            accessTokens = tokenManager.accessToken
        )
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 STOMP 클라이언트를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StompEntryPoint {
    fun stompClient(): StompClient
}
