package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AppointmentDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface AppointmentDetailApi {

    /** 약속 정보, 후보 날짜·장소, 내 역할, 참여 멤버와 일정 제출 여부를 한 번에 조회한다. */
    @GET("api/v1/appointments/{appointmentId}")
    suspend fun getAppointmentDetail(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<ApiResponse<AppointmentDetailDto>>
}
