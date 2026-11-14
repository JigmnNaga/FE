package com.jigmnnaga.presentation.appointment.existingadd

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 기존 약속 추가 화면에서 사용자에게 한 번 알려야 하는 안내. */
enum class ExistingAppointmentAddNotice {
    REQUEST_API_NOT_CONNECTED,
    CHAT_NOT_CONNECTED,

    /** 고른 멤버를 서버 멤버 ID로 바꿀 수 없다. */
    REQUEST_MEMBERS_INVALID
}

/** "약속 요청하기"로 고른 참여자. 이 멤버들을 이전 약속 멤버로 담아 새 약속을 만든다. */
data class PreviousMembersRequest(
    val previousAppointmentId: Long,
    val memberIds: List<Long>
)

/**
 * 기존 약속에서 새 약속을 요청하는 화면의 페이지 전환, 멤버 선택, 요청 버튼 활성 조건을 관리한다.
 * 약속 정보와 멤버 목록은 호출하는 쪽이 [initialState]로 넘긴다.
 *
 * [appointmentId]가 있으면 "약속 요청하기"가 고른 참여자를 [requestedMembers]로 내보낸다.
 * 화면은 이를 받아 이전 약속 멤버를 담은 약속 생성 화면으로 이동한다.
 * [appointmentId]가 없으면 서버로 보내지 않고, 선택값을 유지한 채 안내만 보낸다.
 */
class ExistingAppointmentAddViewModel(
    initialState: ExistingAppointmentAddUiState,
    private val appointmentId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState.withRequestEligibility())
    val uiState: StateFlow<ExistingAppointmentAddUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<ExistingAppointmentAddNotice?>(null)
    val notice: StateFlow<ExistingAppointmentAddNotice?> = _notice.asStateFlow()

    private val _requestedMembers = MutableStateFlow<PreviousMembersRequest?>(null)
    val requestedMembers: StateFlow<PreviousMembersRequest?> = _requestedMembers.asStateFlow()

    /** "다시 약속 잡기"를 누르면 참여자를 고르는 페이지로 넘어가고, 이전 선택은 비운다. */
    fun onRescheduleClick() {
        _uiState.value = _uiState.value.copy(
            page = ExistingAppointmentAddPage.REQUEST_PARTICIPANTS,
            selectedMemberIds = emptySet()
        ).withRequestEligibility()
    }

    /** 참여자 선택 페이지에서, 고를 수 있는 멤버만 선택하거나 해제한다. */
    fun onMemberSelectionChange(memberId: String, selected: Boolean) {
        val current = _uiState.value
        if (current.page != ExistingAppointmentAddPage.REQUEST_PARTICIPANTS) return
        val member = current.members.firstOrNull { it.id == memberId } ?: return
        if (!member.isSelectable) return
        val updated = if (selected) {
            current.selectedMemberIds + memberId
        } else {
            current.selectedMemberIds - memberId
        }
        _uiState.value = current.copy(selectedMemberIds = updated).withRequestEligibility()
    }

    /**
     * 요청할 수 있을 때, 고른 참여자를 [requestedMembers]로 내보낸다. 선택값은 유지한다.
     * 본인과 고를 수 없는 멤버는 넣지 않는다.
     */
    fun onRequestClick() {
        val current = _uiState.value
        if (!current.canRequest) return
        val previousAppointmentId = appointmentId
        if (previousAppointmentId == null) {
            _notice.value = ExistingAppointmentAddNotice.REQUEST_API_NOT_CONNECTED
            return
        }
        val memberIds = current.members
            .filter { it.isSelectable && it.id in current.selectedMemberIds }
            .mapNotNull { it.id.toLongOrNull() }
        if (memberIds.isEmpty()) {
            _notice.value = ExistingAppointmentAddNotice.REQUEST_MEMBERS_INVALID
            return
        }
        _requestedMembers.value = PreviousMembersRequest(previousAppointmentId, memberIds)
    }

    /** 화면이 [requestedMembers]를 받아 이동한 뒤 부른다. */
    fun consumeRequestedMembers() {
        _requestedMembers.value = null
    }

    /** 채팅 화면은 아직 없어서 탭을 바꾸지 않고 안내만 보낸다. */
    fun onChatTabClick() {
        _notice.value = ExistingAppointmentAddNotice.CHAT_NOT_CONNECTED
    }

    fun onMapAndStatusTabClick() {
        _uiState.value = _uiState.value.copy(selectedRoomTab = ExistingAppointmentRoomTab.MAP_AND_STATUS)
    }

    /**
     * 뒤로 가기를 눌렀을 때 참여자 선택 페이지면 이전 페이지로 돌아가고 true를 돌려준다.
     * false면 처리할 것이 없으니 호출하는 쪽이 화면을 닫는다.
     */
    fun onBackClick(): Boolean {
        val current = _uiState.value
        if (current.page != ExistingAppointmentAddPage.REQUEST_PARTICIPANTS) return false
        _uiState.value = current.copy(
            page = ExistingAppointmentAddPage.RESCHEDULE,
            selectedMemberIds = emptySet()
        ).withRequestEligibility()
        return true
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** 참여자 선택 페이지에서 한 명 이상 골랐을 때만 요청할 수 있다. */
    private fun ExistingAppointmentAddUiState.withRequestEligibility():
        ExistingAppointmentAddUiState = copy(
        canRequest = page == ExistingAppointmentAddPage.REQUEST_PARTICIPANTS &&
            selectedMemberIds.isNotEmpty()
    )
}
