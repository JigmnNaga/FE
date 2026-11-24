package com.jigmnnaga.presentation.mypage.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.domain.model.AppointmentDetailResult
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.LogoutResult
import com.jigmnnaga.domain.model.WithdrawResult
import com.jigmnnaga.domain.repository.AppointmentDetailRepository
import com.jigmnnaga.domain.repository.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val isLoading: Boolean = false,
    val logoutSuccess: Boolean = false,
    val withdrawSuccess: Boolean = false,
    val loginRequired: Boolean = false,
    val errorMessage: String? = null,
    val rooms: List<AppointmentRoomInfo> = emptyList(),
    val isLoadingRooms: Boolean = false
)

/**
 * 로그아웃 · 회원탈퇴 API 연동을 담당하는 ViewModel.
 * 로딩 중 중복 클릭을 방지하고 결과를 [AccountUiState]로 노출한다.
 * 회원탈퇴 안내 화면에서는 [loadUpcomingAppointments]로 다가오는 약속 목록을 조회한다.
 */
class AccountViewModel(
    private val authRepository: AuthRepository,
    private val myAppointmentApi: MyAppointmentApi,
    private val tokenManager: TokenManager,
    private val appointmentDetailRepository: AppointmentDetailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun logout() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.logout()) {
                LogoutResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, logoutSuccess = true) }
                }
                LogoutResult.LoginRequired -> {
                    // 이미 로그아웃 상태 → 토큰 없음, 로그인 화면으로 이동
                    _uiState.update { it.copy(isLoading = false, loginRequired = true) }
                }
                is LogoutResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (result.retryable) "잠시 후 다시 시도해 주세요." else "로그아웃에 실패했습니다."
                        )
                    }
                }
            }
        }
    }

    fun withdraw() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.withdraw()) {
                WithdrawResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, withdrawSuccess = true) }
                }
                WithdrawResult.LoginRequired -> {
                    _uiState.update { it.copy(isLoading = false, loginRequired = true) }
                }
                is WithdrawResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (result.retryable) "잠시 후 다시 시도해 주세요." else "회원탈퇴에 실패했습니다."
                        )
                    }
                }
            }
        }
    }

    /**
     * 다가오는 약속 목록을 조회하여 [AccountUiState.rooms]에 반영한다.
     * 각 약속의 방장 여부는 [AppointmentDetailRepository]를 통해 병렬로 조회한다.
     * 조회 실패 시 [rooms]는 빈 목록으로 유지되며 탈퇴 진행에는 영향을 주지 않는다.
     */
    fun loadUpcomingAppointments() {
        if (_uiState.value.isLoadingRooms) return
        _uiState.update { it.copy(isLoadingRooms = true) }
        viewModelScope.launch {
            try {
                val token = tokenManager.accessToken.first()
                if (token.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoadingRooms = false) }
                    return@launch
                }

                val listResponse = myAppointmentApi.getMyAppointments(
                    authorization = "Bearer $token",
                    type = "UPCOMING"
                )
                if (!listResponse.isSuccessful) {
                    _uiState.update { it.copy(isLoadingRooms = false) }
                    return@launch
                }

                val appointments = listResponse.body()?.data ?: emptyList()

                // 각 약속의 내 역할(방장 여부)을 병렬로 조회한다.
                val rooms = appointments.map { appointment ->
                    async {
                        val isHost = when (val detail =
                            appointmentDetailRepository.getAppointmentDetail(appointment.appointmentId)) {
                            is AppointmentDetailResult.Success ->
                                detail.value.myRole == AppointmentMemberRole.HOST
                            else -> false
                        }
                        val status = when {
                            isHost && appointment.memberCount >= 3 -> AppointmentRoomStatus.LEADER_TRANSFER
                            isHost -> AppointmentRoomStatus.LEADER_DELETE
                            else -> AppointmentRoomStatus.MEMBER
                        }
                        AppointmentRoomInfo(
                            name = appointment.title,
                            memberCount = appointment.memberCount,
                            isLeader = isHost,
                            status = status
                        )
                    }
                }.awaitAll()

                _uiState.update { it.copy(isLoadingRooms = false, rooms = rooms) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingRooms = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
