package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null
)
