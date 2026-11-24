package com.jigmnnaga.di

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AuthApi
import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.data.repository.AuthRepositoryImpl
import com.jigmnnaga.domain.repository.AppointmentDetailRepository
import com.jigmnnaga.domain.repository.AuthRepository
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
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideAuthApi(retrofit: Retrofit): AuthApi =
            retrofit.create(AuthApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 [com.jigmnnaga.presentation.common.login.LoginViewModel]에
 * 인증 저장소를 넘겨 주기 위한 진입점입니다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthEntryPoint {
    fun authRepository(): AuthRepository
}

/**
 * 로그아웃 · 회원탈퇴 화면의 [com.jigmnnaga.presentation.mypage.account.AccountViewModel]에
 * 필요한 의존성을 넘겨 주기 위한 진입점입니다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccountEntryPoint {
    fun authRepository(): AuthRepository
    fun myAppointmentApi(): MyAppointmentApi
    fun tokenManager(): TokenManager
    fun appointmentDetailRepository(): AppointmentDetailRepository
}
