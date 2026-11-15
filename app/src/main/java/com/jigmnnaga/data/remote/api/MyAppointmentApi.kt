package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
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
}
