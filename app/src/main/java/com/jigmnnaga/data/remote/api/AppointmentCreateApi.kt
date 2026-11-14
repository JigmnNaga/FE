package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AppointmentCreateRequestDto
import com.jigmnnaga.data.remote.dto.AppointmentCreateResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AppointmentCreateApi {

    @POST("api/v1/appointments")
    suspend fun createAppointment(
        @Header("Authorization") authorization: String?,
        @Body request: AppointmentCreateRequestDto
    ): Response<ApiResponse<AppointmentCreateResponseDto>>
}
