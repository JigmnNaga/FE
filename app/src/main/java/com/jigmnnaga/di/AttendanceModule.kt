package com.jigmnnaga.di

import com.jigmnnaga.data.remote.api.AttendanceApi
import com.jigmnnaga.data.repository.AttendanceRepositoryImpl
import com.jigmnnaga.domain.repository.AttendanceRepository
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
abstract class AttendanceModule {

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        impl: AttendanceRepositoryImpl
    ): AttendanceRepository

    companion object {
        @Provides
        @Singleton
        fun provideAttendanceApi(retrofit: Retrofit): AttendanceApi =
            retrofit.create(AttendanceApi::class.java)
    }
}

/**
 * Hilt가 직접 만들지 않는 화면 ViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AttendanceEntryPoint {
    fun attendanceRepository(): AttendanceRepository
}
