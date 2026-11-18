package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.MemberLocationApi
import com.jigmnnaga.data.repository.MemberLocationRepositoryImpl
import com.jigmnnaga.domain.repository.MemberLocationRepository
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
abstract class MemberLocationModule {

    @Binds
    @Singleton
    abstract fun bindMemberLocationRepository(
        impl: MemberLocationRepositoryImpl
    ): MemberLocationRepository

    companion object {
        @Provides
        @Singleton
        fun provideMemberLocationApi(retrofit: Retrofit): MemberLocationApi =
            retrofit.create(MemberLocationApi::class.java)
    }
}

/** Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MemberLocationEntryPoint {
    fun memberLocationRepository(): MemberLocationRepository
}
