package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.MemberApi
import com.jigmnnaga.data.repository.MypageRepositoryImpl
import com.jigmnnaga.domain.repository.MypageRepository
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
abstract class MypageModule {

    @Binds
    @Singleton
    abstract fun bindMypageRepository(
        impl: MypageRepositoryImpl
    ): MypageRepository

    companion object {
        @Provides
        @Singleton
        fun provideMemberApi(retrofit: Retrofit): MemberApi =
            retrofit.create(MemberApi::class.java)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MypageEntryPoint {
    fun mypageRepository(): MypageRepository
}
