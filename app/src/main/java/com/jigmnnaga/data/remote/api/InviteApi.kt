package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.InviteAcceptDto
import com.jigmnnaga.data.remote.dto.InviteDetailDto
import com.jigmnnaga.data.remote.dto.ReceivedInviteDto
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

    /** 수신한 초대 목록 조회. 홈 화면에서 미읽은 초대 수 뱃지에 사용. */
    @GET("api/v1/invites/me")
    suspend fun getReceivedInvites(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<List<ReceivedInviteDto>>>
}
