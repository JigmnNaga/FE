package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.PlaceSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PlaceSearchApi {

    /** [type]은 `STATION`(지하철역) 또는 `PLACE`(상호·장소)다. */
    @GET("api/v1/places/search")
    suspend fun search(
        @Header("Authorization") authorization: String?,
        @Query("query") query: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<PlaceSearchResponseDto>>
}
