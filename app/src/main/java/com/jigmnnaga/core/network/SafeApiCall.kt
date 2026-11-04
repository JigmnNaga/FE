
package com.jigmnnaga.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

suspend fun <T : Any> safeApiCall(
    apiCall: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = apiCall()

        if (response.isSuccessful) {
            val body = response.body()

            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.UnknownError(
                    "응답 데이터가 없습니다."
                )
            }
        } else {
            NetworkResult.HttpError(
                code = response.code(),
                message = response.message()
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        NetworkResult.NetworkError(e.message)
    } catch (e: HttpException) {
        NetworkResult.HttpError(
            code = e.code(),
            message = e.message()
        )
    } catch (e: Exception) {
        NetworkResult.UnknownError(e.message)
    }
}