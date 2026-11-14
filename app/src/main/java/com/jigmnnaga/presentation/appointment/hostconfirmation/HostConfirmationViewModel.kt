package com.jigmnnaga.presentation.appointment.hostconfirmation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 방장 최종 확정 화면에서 사용자에게 한 번 알려야 하는 안내. */
enum class HostConfirmationNotice {
    CONFIRM_API_NOT_CONNECTED
}

/**
 * 방장 최종 확정 화면의 단계 이동, 날짜·시간·장소·세부 위치 선택, 확정 버튼 활성 조건을 관리한다.
 * 확정 서버 연동 전이라 확정을 눌러도 서버로 보내지 않고, 선택값을 유지한 채 안내만 보낸다.
 * 선택 후보와 약속 정보는 호출하는 쪽이 [initialState]로 넘긴다.
 *
 * @param isHost 방장 여부. 방장이 아니면 확정할 수 없다.
 * TODO: 약속 상세 API의 내 역할(myRole)이 내려오면 호출하는 쪽에서 실제 값을 넘긴다.
 */
class HostConfirmationViewModel(
    initialState: HostConfirmationUiState,
    private val isHost: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState.withConfirmEligibility())
    val uiState: StateFlow<HostConfirmationUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<HostConfirmationNotice?>(null)
    val notice: StateFlow<HostConfirmationNotice?> = _notice.asStateFlow()

    fun onEditDateTime() = moveTo(HostConfirmationStep.DATE_TIME)

    fun onEditPlace() = moveTo(HostConfirmationStep.PLACE)

    fun onOpenDetailLocation() = moveTo(HostConfirmationStep.DETAIL_LOCATION)

    /** 후보 중 하나를 고르면 선택값을 바꾸고 확인 단계로 돌아간다. 없는 후보는 무시한다. */
    fun onSelectDateTime(id: String) {
        edit {
            val choice = dateTimeChoices.firstOrNull { it.id == id } ?: return@edit this
            copy(
                dateTimeChoices = dateTimeChoices.map { it.copy(selected = it.id == id) },
                selectedDateTime = choice.title,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    fun onSelectPlace(id: String) {
        edit {
            val choice = placeChoices.firstOrNull { it.id == id } ?: return@edit this
            copy(
                placeChoices = placeChoices.map { it.copy(selected = it.id == id) },
                selectedPlace = choice.title,
                selectedPlaceDescription = choice.supportingText,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    fun onSelectDetailLocation(id: String) {
        edit {
            val choice = detailLocationChoices.firstOrNull { it.id == id } ?: return@edit this
            copy(
                detailLocationChoices = detailLocationChoices.map { it.copy(selected = it.id == id) },
                selectedDetailLocation = choice.title,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    /** 세부 위치는 나중에 정하기로 하고 선택을 비운 채 확인 단계로 돌아간다. */
    fun onChooseDetailLater() {
        edit {
            copy(
                detailLocationChoices = detailLocationChoices.map { it.copy(selected = false) },
                selectedDetailLocation = null,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    /** 선택 단계에서는 확인 단계로 돌아가고 true를 돌려준다. 확인·완료 단계면 false라 화면을 닫아야 한다. */
    fun onBack(): Boolean {
        val step = _uiState.value.step
        return if (step == HostConfirmationStep.REVIEW || step == HostConfirmationStep.COMPLETE) {
            false
        } else {
            edit { copy(step = HostConfirmationStep.REVIEW) }
            true
        }
    }

    /** 확정 조건이 맞아도 서버 연동 전에는 보내지 않고 안내만 띄운다. 선택값은 지우지 않는다. */
    fun onConfirm() {
        val state = _uiState.value
        if (state.step != HostConfirmationStep.REVIEW || !state.canConfirm || state.isBusy) return
        _notice.value = HostConfirmationNotice.CONFIRM_API_NOT_CONNECTED
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun moveTo(step: HostConfirmationStep) {
        if (_uiState.value.step == HostConfirmationStep.COMPLETE) return
        edit { copy(step = step) }
    }

    private fun edit(transform: HostConfirmationUiState.() -> HostConfirmationUiState) {
        _uiState.value = _uiState.value.transform().withConfirmEligibility()
    }

    /** 방장이 날짜·시간과 장소를 모두 골랐고 처리 중이 아닐 때만 확정할 수 있다. */
    private fun HostConfirmationUiState.withConfirmEligibility(): HostConfirmationUiState = copy(
        canConfirm = isHost &&
            !selectedDateTime.isNullOrBlank() &&
            !selectedPlace.isNullOrBlank() &&
            !isBusy
    )
}
