package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.CalendarResponseDto
import com.jigmnnaga.data.remote.dto.MyAppointmentResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MyAppointmentApi {

    /**
     * 내 약속 목록 조회.
     * @param type `"UPCOMING"` 또는 `"PAST"`
     */
    @GET("api/v1/appointments/me")
    suspend fun getMyAppointments(
        @Header("Authorization") authorization: String?,
        @Query("type") type: String
    ): Response<ApiResponse<List<MyAppointmentResponseDto>>>

    /**
     * 가장 가까운 다음 확정 약속 조회.
     * 없으면 `data: null`.
     */
    @GET("api/v1/appointments/me/next")
    suspend fun getNextAppointment(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<MyAppointmentResponseDto?>>

    /**
     * 월별 캘린더 조회.
     * 해당 월에 날짜가 확정된 내 약속을 날짜별로 반환한다.
     * 약속이 없는 날짜는 응답에 포함되지 않으며, 약속이 없는 월은 `days: []`를 반환한다.
     * @param month `yyyy-MM` 형식 (예: `"2026-09"`)
     */
    @GET("api/v1/appointments/me/calendar")
    suspend fun getCalendar(
        @Header("Authorization") authorization: String?,
        @Query("month") month: String
    ): Response<ApiResponse<CalendarResponseDto>>
}
