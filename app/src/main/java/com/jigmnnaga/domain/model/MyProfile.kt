package com.jigmnnaga.domain.model

data class MyProfile(
    val memberId: Long,
    val nickname: String,
    val email: String,
    val profileImageUrl: String?
)

sealed interface ProfileResult {
    data class Success(val profile: MyProfile) : ProfileResult
    data object LoginRequired : ProfileResult
    data class Failure(val retryable: Boolean) : ProfileResult
}

sealed interface ProfileUpdateResult {
    data class Success(val updatedProfile: MyProfile) : ProfileUpdateResult
    data object LoginRequired : ProfileUpdateResult
    data class Failure(val retryable: Boolean) : ProfileUpdateResult
}

sealed interface ProfileImageUploadUrlResult {
    data class Success(
        val uploadUrl: String,
        val profileImageKey: String
    ) : ProfileImageUploadUrlResult
    data object LoginRequired : ProfileImageUploadUrlResult
    data class Failure(val retryable: Boolean) : ProfileImageUploadUrlResult
}
