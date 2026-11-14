package com.jigmnnaga.presentation.appointment.hostconfirmation

/** The visible part of the host-only final appointment confirmation flow. */
enum class HostConfirmationStep {
    REVIEW,
    DATE_TIME,
    PLACE,
    DETAIL_LOCATION,
    COMPLETE
}

/** A choice supplied by the screen owner; this UI does not apply selection rules. */
data class HostConfirmationChoice(
    val id: String,
    val title: String,
    val supportingText: String? = null,
    val selected: Boolean = false
)

/** UI data only. Confirmation eligibility and all domain rules remain external. */
data class HostConfirmationUiState(
    val step: HostConfirmationStep = HostConfirmationStep.REVIEW,
    val appointmentTitle: String,
    val selectedDateTime: String?,
    val selectedPlace: String?,
    val selectedPlaceDescription: String? = null,
    val selectedDetailLocation: String? = null,
    val dateTimeChoices: List<HostConfirmationChoice> = emptyList(),
    val placeChoices: List<HostConfirmationChoice> = emptyList(),
    val detailLocationChoices: List<HostConfirmationChoice> = emptyList(),
    /** 세부 위치(상호명) 검색어. */
    val detailSearchQuery: String = "",
    val canConfirm: Boolean = false,
    val isBusy: Boolean = false
)

/** User actions are forwarded; this layer never confirms or persists an appointment itself. */
data class HostConfirmationActions(
    val onBack: () -> Unit,
    val onEditDateTime: () -> Unit,
    val onEditPlace: () -> Unit,
    val onSelectDateTime: (String) -> Unit,
    val onSelectPlace: (String) -> Unit,
    val onOpenDetailLocation: () -> Unit,
    val onSelectDetailLocation: (String) -> Unit,
    val onChooseDetailLater: () -> Unit,
    val onConfirm: () -> Unit,
    val onComplete: () -> Unit,
    val onDetailSearchQueryChange: (String) -> Unit = {}
)
