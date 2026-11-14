package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AppointmentTitleUpdateRequestDto
import com.jigmnnaga.data.remote.dto.CandidateDatesUpdateRequestDto
import com.jigmnnaga.data.remote.dto.CandidateLocationsUpdateRequestDto
import com.jigmnnaga.data.remote.dto.HostDelegateRequestDto
import com.jigmnnaga.data.remote.dto.ReminderSendResponseDto
import com.jigmnnaga.data.remote.dto.ResponseDeadlineUpdateRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AppointmentManageApi {

    @PATCH("api/v1/appointments/{appointmentId}/title")
    suspend fun updateTitle(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: AppointmentTitleUpdateRequestDto
    ): Response<Unit>

    @PUT("api/v1/appointments/{appointmentId}/candidate-dates")
    suspend fun updateCandidateDates(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: CandidateDatesUpdateRequestDto
    ): Response<Unit>

    @PUT("api/v1/appointments/{appointmentId}/candidate-locations")
    suspend fun updateCandidateLocations(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: CandidateLocationsUpdateRequestDto
    ): Response<Unit>

    @PATCH("api/v1/appointments/{appointmentId}/response-deadline")
    suspend fun updateResponseDeadline(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: ResponseDeadlineUpdateRequestDto
    ): Response<Unit>

    @POST("api/v1/appointments/{appointmentId}/cancel")
    suspend fun cancel(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<Unit>

    @DELETE("api/v1/appointments/{appointmentId}")
    suspend fun delete(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<Unit>

    @PATCH("api/v1/appointments/{appointmentId}/host")
    suspend fun delegateHost(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: HostDelegateRequestDto
    ): Response<Unit>

    @POST("api/v1/appointments/{appointmentId}/participants/{targetMemberId}/kick")
    suspend fun kickParticipant(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Path("targetMemberId") targetMemberId: Long
    ): Response<Unit>

    @POST("api/v1/appointments/{appointmentId}/reminders")
    suspend fun sendReminders(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<ApiResponse<ReminderSendResponseDto>>
}
