package com.jigmnnaga.presentation.mypage.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.ProfileImageUploadUrlResult
import com.jigmnnaga.domain.model.ProfileResult
import com.jigmnnaga.domain.model.ProfileUpdateResult
import com.jigmnnaga.domain.repository.MypageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 프로필 편집 화면 상태를 관리한다.
 * 화면 진입 시 서버에서 최신 프로필을 불러오고, 닉네임/이미지 저장 요청을 처리한다.
 */
class ProfileEditViewModel(
    private val repository: MypageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState(isLoading = true))
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun onNicknameChange(nickname: String) {
        _uiState.update { state ->
            state.copy(
                nickname = nickname,
                profileInitial = if (state.profileImageUrl == null) nickname.firstOrNull()?.toString() else null,
                isSaveEnabled = nickname.isNotBlank() && nickname != state.originalNickname
            )
        }
    }

    fun onSaveClick() {
        val nickname = _uiState.value.nickname.trim()
        if (nickname.isBlank()) return
        viewModelScope.launch {
            when (val result = repository.updateNickname(nickname)) {
                is ProfileUpdateResult.Success -> _uiState.update { state ->
                    state.copy(
                        nickname = result.updatedProfile.nickname,
                        originalNickname = result.updatedProfile.nickname,
                        profileImageUrl = result.updatedProfile.profileImageUrl,
                        isSaveEnabled = false,
                        saveSuccess = true
                    )
                }
                ProfileUpdateResult.LoginRequired -> _uiState.update { it.copy(loginRequired = true) }
                is ProfileUpdateResult.Failure -> _uiState.update { it.copy(saveFailed = true) }
            }
        }
    }

    /**
     * 갤러리 또는 카메라에서 선택한 이미지를 업로드한다.
     * S3 PUT 업로드가 블로킹 IO이므로 Dispatchers.IO에서 실행한다.
     */
    fun onImageSelected(contentType: String, contentLength: Long, imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImageUploading = true) }

            // 1. Presigned URL 발급
            val urlResult = repository.getProfileImageUploadUrl(contentType, contentLength)
            if (urlResult !is ProfileImageUploadUrlResult.Success) {
                handleImageUploadUrlError(urlResult)
                return@launch
            }

            // 2. S3에 PUT 업로드 (블로킹 IO)
            val uploaded = withContext(Dispatchers.IO) {
                repository.uploadToS3(urlResult.uploadUrl, contentType, contentLength, imageBytes)
            }
            if (!uploaded) {
                _uiState.update { it.copy(isImageUploading = false, imageUploadFailed = true) }
                return@launch
            }

            // 3. 프로필 이미지 키 저장
            when (val result = repository.updateProfileImage(urlResult.profileImageKey)) {
                is ProfileUpdateResult.Success -> _uiState.update { state ->
                    state.copy(
                        isImageUploading = false,
                        profileImageUrl = result.updatedProfile.profileImageUrl,
                        profileInitial = if (result.updatedProfile.profileImageUrl != null) null
                                         else state.nickname.firstOrNull()?.toString()
                    )
                }
                ProfileUpdateResult.LoginRequired -> _uiState.update {
                    it.copy(isImageUploading = false, loginRequired = true)
                }
                is ProfileUpdateResult.Failure -> _uiState.update {
                    it.copy(isImageUploading = false, imageUploadFailed = true)
                }
            }
        }
    }

    fun onDeletePhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImageUploading = true) }
            when (val result = repository.deleteProfileImage()) {
                is ProfileUpdateResult.Success -> _uiState.update { state ->
                    state.copy(
                        isImageUploading = false,
                        profileImageUrl = null,
                        profileInitial = state.nickname.firstOrNull()?.toString()
                    )
                }
                ProfileUpdateResult.LoginRequired -> _uiState.update {
                    it.copy(isImageUploading = false, loginRequired = true)
                }
                is ProfileUpdateResult.Failure -> _uiState.update {
                    it.copy(isImageUploading = false, imageUploadFailed = true)
                }
            }
        }
    }

    fun consumeSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun consumeSaveFailed() = _uiState.update { it.copy(saveFailed = false) }

    fun consumeImageUploadFailed() = _uiState.update { it.copy(imageUploadFailed = false) }

    fun consumeLoginRequired() = _uiState.update { it.copy(loginRequired = false) }

    private fun loadProfile() {
        viewModelScope.launch {
            val result = repository.getMyProfile()
            _uiState.update { state ->
                when (result) {
                    is ProfileResult.Success -> {
                        val profile = result.profile
                        state.copy(
                            isLoading = false,
                            nickname = profile.nickname,
                            originalNickname = profile.nickname,
                            profileInitial = if (profile.profileImageUrl == null)
                                profile.nickname.firstOrNull()?.toString() else null,
                            profileImageUrl = profile.profileImageUrl
                        )
                    }
                    ProfileResult.LoginRequired -> state.copy(isLoading = false, loginRequired = true)
                    is ProfileResult.Failure -> state.copy(isLoading = false, loadFailed = true)
                }
            }
        }
    }

    private fun handleImageUploadUrlError(result: ProfileImageUploadUrlResult) {
        _uiState.update { state ->
            when (result) {
                is ProfileImageUploadUrlResult.Success -> state
                ProfileImageUploadUrlResult.LoginRequired -> state.copy(isImageUploading = false, loginRequired = true)
                is ProfileImageUploadUrlResult.Failure -> state.copy(isImageUploading = false, imageUploadFailed = true)
            }
        }
    }
}
