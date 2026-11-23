package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.ProfileImageUploadUrlResult
import com.jigmnnaga.domain.model.ProfileResult
import com.jigmnnaga.domain.model.ProfileUpdateResult

interface MypageRepository {
    suspend fun getMyProfile(): ProfileResult
    suspend fun updateNickname(nickname: String): ProfileUpdateResult
    suspend fun getProfileImageUploadUrl(contentType: String, contentLength: Long): ProfileImageUploadUrlResult
    suspend fun uploadToS3(uploadUrl: String, contentType: String, contentLength: Long, imageBytes: ByteArray): Boolean
    suspend fun updateProfileImage(imageKey: String): ProfileUpdateResult
    suspend fun deleteProfileImage(): ProfileUpdateResult
}
