package com.jigmnnaga.di

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.core.datastore.TokenManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenModule {

    @Binds
    @Singleton
    abstract fun bindTokenManager(
        tokenManagerImpl: TokenManagerImpl
    ): TokenManager
}

/**
 * Hilt가 직접 만들지 않는 [com.jigmnnaga.presentation.common.splash.SplashViewModel]에
 * 토큰 관리자를 넘겨 주기 위한 진입점입니다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TokenEntryPoint {
    fun tokenManager(): TokenManager
}