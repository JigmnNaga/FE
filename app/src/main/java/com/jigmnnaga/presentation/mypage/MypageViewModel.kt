package com.jigmnnaga.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.ProfileResult
import com.jigmnnaga.domain.model.StatsResult
import com.jigmnnaga.domain.repository.MypageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MypageViewModel(
    private val repository: MypageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MypageUiState())
    val uiState: StateFlow<MypageUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadProfile()
        loadStats()
    }

    private fun loadProfile() {
        _uiState.update { it.copy(isProfileLoading = true, profileLoadFailed = false) }
        viewModelScope.launch {
            when (val result = repository.getMyProfile()) {
                is ProfileResult.Success -> {
                    val profile = result.profile
                    _uiState.update {
                        it.copy(
                            isProfileLoading = false,
                            nickname = profile.nickname,
                            email = profile.email,
                            profileImageUrl = profile.profileImageUrl,
                            profileInitial = profile.nickname.firstOrNull()?.toString(),
                            profileLoadFailed = false,
                            loginRequired = false
                        )
                    }
                }
                ProfileResult.LoginRequired -> {
                    _uiState.update {
                        it.copy(isProfileLoading = false, loginRequired = true)
                    }
                }
                is ProfileResult.Failure -> {
                    _uiState.update {
                        it.copy(isProfileLoading = false, profileLoadFailed = true)
                    }
                }
            }
        }
    }

    private fun loadStats() {
        _uiState.update { it.copy(isStatsLoading = true, statsLoadFailed = false) }
        viewModelScope.launch {
            when (val result = repository.getMyStats()) {
                is StatsResult.Success -> {
                    val stats = result.stats
                    _uiState.update {
                        it.copy(
                            isStatsLoading = false,
                            totalAppointments = stats.totalCount,
                            onTimeCount = stats.onTimeCount,
                            lateCount = stats.lateCount,
                            statsLoadFailed = false
                        )
                    }
                }
                StatsResult.LoginRequired -> {
                    _uiState.update {
                        it.copy(isStatsLoading = false, loginRequired = true)
                    }
                }
                is StatsResult.Failure -> {
                    _uiState.update {
                        it.copy(isStatsLoading = false, statsLoadFailed = true)
                    }
                }
            }
        }
    }
}
