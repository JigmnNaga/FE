package com.jigmnnaga.core.datastore

import kotlinx.coroutines.flow.Flow

interface TokenManager {

    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String
    )

    suspend fun clearTokens()

    suspend fun isAccessTokenExpired(): Boolean

    suspend fun isRefreshTokenExpired(): Boolean

    suspend fun validateAndClearInvalidTokens(): Boolean
}