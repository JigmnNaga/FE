package com.jigmnnaga.presentation.appointment.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailResult
import com.jigmnnaga.domain.repository.AppointmentDetailRepository
import com.jigmnnaga.presentation.appointment.AppointmentRefreshSignal
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 약속 상세를 불러오는 중이거나, 불러온 결과. */
sealed interface AppointmentDetailLoadState {
    data object Loading : AppointmentDetailLoadState
    data class Loaded(val detail: AppointmentDetail) : AppointmentDetailLoadState
    data object LoginRequired : AppointmentDetailLoadState
    data object NotParticipant : AppointmentDetailLoadState
    data class Failed(val retryable: Boolean) : AppointmentDetailLoadState
}

/**
 * 화면에 들어올 때 약속 상세를 한 번 불러오고, 실패하면 다시 시도할 수 있게 한다.
 * 로그인이 필요해도 자동으로 로그인 화면으로 보내지 않고 상태만 알린다. 화면에서 버튼을 눌렀을 때만 이동한다.
 */
class AppointmentDetailLoadViewModel(
    private val appointmentId: Long,
    private val repository: AppointmentDetailRepository,
    private val refreshSignal: StateFlow<Int> = AppointmentRefreshSignal.version
) : ViewModel() {

    private val _state = MutableStateFlow<AppointmentDetailLoadState>(AppointmentDetailLoadState.Loading)
    val state: StateFlow<AppointmentDetailLoadState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
        // 약속 관리 화면에서 약속 정보를 바꾸면 이 화면의 약속 상세도 다시 불러온다.
        var seenVersion = refreshSignal.value
        viewModelScope.launch {
            refreshSignal.collect { version ->
                if (version != seenVersion) {
                    seenVersion = version
                    load()
                }
            }
        }
    }

    /** 처음 불러올 때와 "다시 시도", 로그인 뒤 다시 불러올 때 쓴다. 진행 중인 요청이 있으면 취소하고 새로 시작한다. */
    fun load() {
        loadJob?.cancel()
        _state.value = AppointmentDetailLoadState.Loading
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getAppointmentDetail(appointmentId)) {
                is AppointmentDetailResult.Success -> AppointmentDetailLoadState.Loaded(result.value)
                AppointmentDetailResult.LoginRequired -> AppointmentDetailLoadState.LoginRequired
                AppointmentDetailResult.NotParticipant -> AppointmentDetailLoadState.NotParticipant
                is AppointmentDetailResult.Failure -> AppointmentDetailLoadState.Failed(result.retryable)
            }
        }
    }
}
