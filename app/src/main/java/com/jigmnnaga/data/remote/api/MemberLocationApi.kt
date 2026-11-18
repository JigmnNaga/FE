package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.MemberLocationDto
import com.jigmnnaga.data.remote.dto.MemberLocationRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MemberLocationApi {

    @GET("api/v1/members/me/locations")
    suspend fun getLocations(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<List<MemberLocationDto>>>

    @POST("api/v1/members/me/locations")
    suspend fun createLocation(
        @Header("Authorization") authorization: String?,
        @Body request: MemberLocationRequestDto
    ): Response<ApiResponse<MemberLocationDto>>

    @PUT("api/v1/members/me/locations/{locationId}")
    suspend fun updateLocation(
        @Header("Authorization") authorization: String?,
        @Path("locationId") locationId: Long,
        @Body request: MemberLocationRequestDto
    ): Response<ApiResponse<MemberLocationDto>>

    @DELETE("api/v1/members/me/locations/{locationId}")
    suspend fun deleteLocation(
        @Header("Authorization") authorization: String?,
        @Path("locationId") locationId: Long
    ): Response<Unit>
}
