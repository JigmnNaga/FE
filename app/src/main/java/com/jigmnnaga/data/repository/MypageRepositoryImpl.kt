package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.MemberApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.ProfileImageUploadUrlRequestDto
import com.jigmnnaga.data.remote.dto.ProfileUpdateRequestDto
import com.jigmnnaga.domain.model.MyProfile
import com.jigmnnaga.domain.model.ProfileImageUploadUrlResult
import com.jigmnnaga.domain.model.ProfileResult
import com.jigmnnaga.domain.model.ProfileUpdateResult
import com.jigmnnaga.domain.model.StatsResult
import com.jigmnnaga.domain.repository.MypageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

class MypageRepositoryImpl @Inject constructor(
    private val api: MemberApi,
    private val tokenManager: TokenManager,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) : MypageRepository {

    override suspend fun getMyProfile(): ProfileResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return ProfileResult.LoginRequired

        return try {
            val response = api.getMyInfo("Bearer $token")
            if (response.isSuccessful) {
                val profile = response.body()?.data?.toDomain()
                    ?: return ProfileResult.Failure(retryable = false)
                ProfileResult.Success(profile)
            } else {
                val code = response.errorBody()?.string()?.let { parseErrorCode(it) }
                mapProfileError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ProfileResult.Failure(retryable = true)
        } catch (e: Exception) {
            ProfileResult.Failure(retryable = false)
        }
    }

    override suspend fun getMyStats(): StatsResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return StatsResult.LoginRequired

        return try {
            val response = api.getMyStats("Bearer $token")
            if (response.isSuccessful) {
                val stats = response.body()?.data?.toDomain()
                    ?: return StatsResult.Failure(retryable = false)
                StatsResult.Success(stats)
            } else {
                val code = response.errorBody()?.string()?.let { parseErrorCode(it) }
                mapStatsError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            StatsResult.Failure(retryable = true)
        } catch (e: Exception) {
            StatsResult.Failure(retryable = false)
        }
    }

    override suspend fun updateNickname(nickname: String): ProfileUpdateResult {
        return updateProfile(ProfileUpdateRequestDto(nickname = nickname))
    }

    override suspend fun getProfileImageUploadUrl(
        contentType: String,
        contentLength: Long
    ): ProfileImageUploadUrlResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return ProfileImageUploadUrlResult.LoginRequired

        return try {
            val response = api.getProfileImageUploadUrl(
                "Bearer $token",
                ProfileImageUploadUrlRequestDto(contentType, contentLength)
            )
            if (response.isSuccessful) {
                val data = response.body()?.data
                    ?: return ProfileImageUploadUrlResult.Failure(retryable = false)
                ProfileImageUploadUrlResult.Success(
                    uploadUrl = data.uploadUrl,
                    profileImageKey = data.profileImageKey
                )
            } else {
                val code = response.errorBody()?.string()?.let { parseErrorCode(it) }
                mapImageUploadUrlError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ProfileImageUploadUrlResult.Failure(retryable = true)
        } catch (e: Exception) {
            ProfileImageUploadUrlResult.Failure(retryable = false)
        }
    }

    override suspend fun uploadToS3(
        uploadUrl: String,
        contentType: String,
        contentLength: Long,
        imageBytes: ByteArray
    ): Boolean {
        return try {
            val requestBody = imageBytes.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .header("Content-Type", contentType)
                .header("Content-Length", contentLength.toString())
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateProfileImage(imageKey: String): ProfileUpdateResult {
        return updateProfile(ProfileUpdateRequestDto(profileImageKey = imageKey))
    }

    override suspend fun deleteProfileImage(): ProfileUpdateResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return ProfileUpdateResult.LoginRequired

        return try {
            val response = api.deleteProfileImage("Bearer $token")
            if (response.isSuccessful) {
                // 삭제 후 최신 프로필을 다시 조회해서 반환한다
                return when (val result = getMyProfile()) {
                    is ProfileResult.Success -> ProfileUpdateResult.Success(result.profile)
                    ProfileResult.LoginRequired -> ProfileUpdateResult.LoginRequired
                    is ProfileResult.Failure -> ProfileUpdateResult.Failure(result.retryable)
                }
            } else {
                val code = response.errorBody()?.string()?.let { parseErrorCode(it) }
                mapProfileUpdateError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ProfileUpdateResult.Failure(retryable = true)
        } catch (e: Exception) {
            ProfileUpdateResult.Failure(retryable = false)
        }
    }

    private suspend fun updateProfile(request: ProfileUpdateRequestDto): ProfileUpdateResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return ProfileUpdateResult.LoginRequired

        return try {
            val response = api.updateProfile("Bearer $token", request)
            if (response.isSuccessful) {
                val profile = response.body()?.data?.toDomain()
                    ?: return ProfileUpdateResult.Failure(retryable = false)
                ProfileUpdateResult.Success(profile)
            } else {
                val code = response.errorBody()?.string()?.let { parseErrorCode(it) }
                mapProfileUpdateError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ProfileUpdateResult.Failure(retryable = true)
        } catch (e: Exception) {
            ProfileUpdateResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapProfileError(httpStatus: Int, errorCode: String? = null): ProfileResult = when {
    errorCode in PROFILE_LOGIN_REQUIRED_CODES -> ProfileResult.LoginRequired
    httpStatus == 401 -> ProfileResult.LoginRequired
    httpStatus >= 500 -> ProfileResult.Failure(retryable = true)
    else -> ProfileResult.Failure(retryable = false)
}

internal fun mapProfileUpdateError(httpStatus: Int, errorCode: String? = null): ProfileUpdateResult = when {
    errorCode in PROFILE_LOGIN_REQUIRED_CODES -> ProfileUpdateResult.LoginRequired
    httpStatus == 401 -> ProfileUpdateResult.LoginRequired
    httpStatus >= 500 -> ProfileUpdateResult.Failure(retryable = true)
    else -> ProfileUpdateResult.Failure(retryable = false)
}

internal fun mapImageUploadUrlError(httpStatus: Int, errorCode: String? = null): ProfileImageUploadUrlResult = when {
    errorCode in PROFILE_LOGIN_REQUIRED_CODES -> ProfileImageUploadUrlResult.LoginRequired
    httpStatus == 401 -> ProfileImageUploadUrlResult.LoginRequired
    httpStatus >= 500 -> ProfileImageUploadUrlResult.Failure(retryable = true)
    else -> ProfileImageUploadUrlResult.Failure(retryable = false)
}

internal fun mapStatsError(httpStatus: Int, errorCode: String? = null): StatsResult = when {
    errorCode in PROFILE_LOGIN_REQUIRED_CODES -> StatsResult.LoginRequired
    httpStatus == 401 -> StatsResult.LoginRequired
    httpStatus >= 500 -> StatsResult.Failure(retryable = true)
    else -> StatsResult.Failure(retryable = false)
}

private val PROFILE_LOGIN_REQUIRED_CODES = setOf(
    "UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN", "WITHDRAWN_MEMBER"
)
