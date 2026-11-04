
package com.jigmnnaga.core.network

sealed interface NetworkResult<out T> {

    data class Success<T>(
        val data: T
    ) : NetworkResult<T>

    data class HttpError(
        val code: Int,
        val message: String?
    ) : NetworkResult<Nothing>

    data class NetworkError(
        val message: String?
    ) : NetworkResult<Nothing>

    data class UnknownError(
        val message: String?
    ) : NetworkResult<Nothing>
}