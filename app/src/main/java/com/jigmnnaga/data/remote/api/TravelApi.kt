package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.DepartureLocationRequestDto
import com.jigmnnaga.data.remote.dto.TravelModeRequestDto
import com.jigmnnaga.data.remote.dto.TravelOptionsDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TravelApi {

    @PUT("api/v1/appointments/{appointmentId}/travel/departure-location")
    suspend fun registerDepartureLocation(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: DepartureLocationRequestDto
    ): Response<Unit>

    @GET("api/v1/appointments/{appointmentId}/travel/options")
    suspend fun getTravelOptions(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Response<ApiResponse<TravelOptionsDto>>

    @PUT("api/v1/appointments/{appointmentId}/travel/mode")
    suspend fun selectTravelMode(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: TravelModeRequestDto
    ): Response<Unit>

    @POST("api/v1/appointments/{appointmentId}/travel/departure")
    suspend fun depart(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<Unit>

    @POST("api/v1/appointments/{appointmentId}/travel/arrival")
    suspend fun arrive(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<Unit>
}
