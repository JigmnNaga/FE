package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.AttendanceRespondRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface AttendanceApi {

    /** 확정된 약속에 참석 여부를 응답한다. 약속 시간이 지나기 전까지 바꿀 수 있다. */
    @PUT("api/v1/appointments/{appointmentId}/attendance/me")
    suspend fun respond(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: AttendanceRespondRequestDto
    ): Response<Unit>
}
