package com.jigmnnaga.domain.model

data class MyProfile(
    val memberId: Long,
    val nickname: String,
    val email: String,
    val profileImageUrl: String?
)

data class MyStats(
    val totalCount: Int,
    val onTimeCount: Int,
    val lateCount: Int,
    /** 서버에서 내려준 제때 도착률 (0.0~1.0). 약속이 0개면 null. */
    val onTimeRate: Double?
)

sealed interface StatsResult {
    data class Success(val stats: MyStats) : StatsResult
    data object LoginRequired : StatsResult
    data class Failure(val retryable: Boolean) : StatsResult
}

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
