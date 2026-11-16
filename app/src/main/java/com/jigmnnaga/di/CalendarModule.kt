package com.jigmnnaga.di

import com.jigmnnaga.data.repository.CalendarRepositoryImpl
import com.jigmnnaga.domain.repository.CalendarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 캘린더 기능의 Hilt DI 모듈.
 *
 * [com.jigmnnaga.data.remote.api.MyAppointmentApi]는 이미 [HomeModule]에서 Singleton으로 제공하므로
 * 여기서 중복 등록하지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarModule {

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        impl: CalendarRepositoryImpl
    ): CalendarRepository
}

/**
 * Hilt가 직접 만들지 않는 CalendarViewModel에 저장소를 넘겨 주기 위한 진입점이다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CalendarEntryPoint {
    fun calendarRepository(): CalendarRepository
}
