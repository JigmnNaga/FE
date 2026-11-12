package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.InviteAcceptDto
import com.jigmnnaga.data.remote.dto.InviteDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface InviteApi {

    @GET("api/v1/invites/{inviteCode}")
    suspend fun getInvite(
        @Header("Authorization") authorization: String?,
        @Path("inviteCode") inviteCode: String
    ): Response<ApiResponse<InviteDetailDto>>

    @POST("api/v1/invites/{inviteCode}/accept")
    suspend fun acceptInvite(
        @Header("Authorization") authorization: String?,
        @Path("inviteCode") inviteCode: String
    ): Response<ApiResponse<InviteAcceptDto>>

    @POST("api/v1/invites/{inviteCode}/decline")
    suspend fun declineInvite(
        @Header("Authorization") authorization: String?,
        @Path("inviteCode") inviteCode: String
    ): Response<Unit>
}
