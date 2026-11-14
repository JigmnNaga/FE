package com.jigmnnaga.presentation.appointment.manage

import com.jigmnnaga.presentation.appointment.create.AppointmentCreateStep
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentDeadlineChoice
import com.jigmnnaga.presentation.appointment.create.AppointmentLocation
import com.jigmnnaga.presentation.appointment.create.MAX_SELECTED_LOCATIONS

/** 약속 관리 화면의 단계. [MENU]가 관리 목록이고 나머지는 항목별 변경 화면이다. */
enum class AppointmentManageStep {
    MENU,
    TITLE,
    SCHEDULE,
    LOCATIONS,
    DEADLINE,

    /** 참여 멤버 목록과 내보내기. */
    MEMBERS
}

/** 되돌릴 수 없어서 한 번 더 확인받는 작업. */
enum class AppointmentManageConfirm {
    CANCEL_APPOINTMENT,
    DELETE_APPOINTMENT
}

/** 화면을 닫아야 하는 결과. */
enum class AppointmentManageFinish {
    DELETED
}

const val MAX_APPOINTMENT_TITLE_LENGTH = 30

/** 멤버 관리 화면에 보이는 참여 멤버. */
data class ManageMember(
    val memberId: Long,
    val nickname: String,
    val isHost: Boolean
)

/**
 * 약속 관리 화면 상태.
 * 날짜·장소·기한 입력은 약속 생성 화면과 같은 [AppointmentCreateUiState]를 편집용으로 재사용한다.
 *
 * @property saved 서버에 저장된 값. 관리 목록의 요약과 변경 여부 비교에 쓴다.
 * @property form 지금 편집 중인 값. 변경 화면을 나가면 [saved]로 되돌린다.
 * @property savedDeadline 서버에 저장된 응답 기한(`yyyy-MM-dd'T'HH:mm:ss`). 알 수 없으면 null이다.
 */
data class AppointmentManageUiState(
    val step: AppointmentManageStep = AppointmentManageStep.MENU,
    val saved: AppointmentCreateUiState,
    val form: AppointmentCreateUiState,
    val savedDeadline: String? = null,
    val members: List<ManageMember> = emptyList(),
    val isCanceled: Boolean = false,
    val isWorking: Boolean = false,
    val confirm: AppointmentManageConfirm? = null,

    /** 내보내기 확인 팝업에 보이는 멤버. 없으면 팝업을 띄우지 않는다. */
    val kickTarget: ManageMember? = null,

    /** 마지막으로 보낸 미응답자 알림 인원. 안내 문구에 쓴다. */
    val lastReminderSentCount: Int? = null,
    val finished: AppointmentManageFinish? = null
) {
    /** 저장 버튼을 누를 수 있는지. 값이 바뀌지 않았거나 비어 있으면 false다. */
    fun canSave(): Boolean {
        if (isWorking || isCanceled) return false
        return when (step) {
            AppointmentManageStep.MENU,
            AppointmentManageStep.MEMBERS -> false
            AppointmentManageStep.TITLE -> {
                val title = form.appointmentName.trim()
                title.isNotEmpty() &&
                    title.length <= MAX_APPOINTMENT_TITLE_LENGTH &&
                    title != saved.appointmentName
            }
            AppointmentManageStep.SCHEDULE -> form.scheduleType != null &&
                form.copy(step = AppointmentCreateStep.SCHEDULE).isCurrentStepValid() &&
                scheduleChanged()
            AppointmentManageStep.LOCATIONS -> {
                val selected = form.selectedLocations.distinct()
                selected.size in 1..MAX_SELECTED_LOCATIONS &&
                    selected != saved.selectedLocations &&
                    selected.all { it.hasPosition() || it in saved.selectedLocations }
            }
            AppointmentManageStep.DEADLINE -> when (form.deadlineChoice) {
                null -> false
                AppointmentDeadlineChoice.CUSTOM_DATE -> form.customDeadline != null
                else -> true
            }
        }
    }

    private fun scheduleChanged(): Boolean =
        form.scheduleType != saved.scheduleType ||
            form.selectedStartDate != saved.selectedStartDate ||
            form.selectedEndDate != saved.selectedEndDate ||
            form.selectedDates != saved.selectedDates
}

private fun AppointmentLocation.hasPosition(): Boolean = latitude != null && longitude != null
