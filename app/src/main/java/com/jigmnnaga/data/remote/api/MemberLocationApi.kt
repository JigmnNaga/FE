package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.MemberLocationDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface MemberLocationApi {

    @GET("api/v1/members/me/locations")
    suspend fun getLocations(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<List<MemberLocationDto>>>
}
