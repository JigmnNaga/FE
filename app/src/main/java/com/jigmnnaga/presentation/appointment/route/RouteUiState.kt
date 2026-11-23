package com.jigmnnaga.presentation.appointment.route

/** 이동수단. 경로 옵션 한 줄과 지도 위 배지가 같은 값을 쓴다. */
enum class RouteTransport { BUS, SUBWAY, TAXI }

/** 이동 경로 상세의 한 단계가 보여 줄 아이콘 종류. */
enum class RouteStepKind { START, WALK, BUS, SUBWAY, TAXI, ARRIVAL }

/** 이동 경로 상세의 한 단계. 시간은 화면이 계산하지 않고 만들어 둔 문구를 그대로 보여 준다. */
data class RouteStepUiState(
    val kind: RouteStepKind,
    val title: String,
    val description: String,
    val timeLabel: String
)

/** 출발·도착 요약 카드. 왼쪽은 출발(또는 현재), 오른쪽은 도착(또는 약속) 쪽 값이다. */
data class RouteTimeSummaryUiState(
    val startCaption: String,
    val startTime: String,
    val startNote: String,
    val endCaption: String,
    val endTime: String,
    val endNote: String
)

/** 경로 선택 단계의 이동수단 한 줄(ROU-001-02). */
data class RouteOptionUiState(
    val transport: RouteTransport,
    val title: String,
    val durationLabel: String,
    val summary: String,
    val steps: List<RouteStepUiState> = emptyList(),
    /** 이 옵션을 골랐을 때 아래에 보여 줄 권장 출발·도착 요약. */
    val timeSummary: RouteTimeSummaryUiState? = null,
    /** 서버가 알려 준 예상 소요 시간(분). 서버에서 받은 이동수단이면 0보다 크다. */
    val durationMinutes: Int = 0,
    /** 이 옵션을 골랐을 때 도착지 카드 오른쪽에 보여 줄 두 줄. 없으면 카드를 그대로 둔다. */
    val estimateTitle: String? = null,
    val estimateNote: String? = null,
    /** 약속 이동 단계의 구간 목록을 출발 시각 기준으로 다시 만들 때 쓰는 서버 구간과 이름. */
    val travelSteps: List<com.jigmnnaga.domain.model.TravelStep> = emptyList(),
    val startName: String? = null,
    val destinationName: String? = null
)

enum class RouteMemberStatus { NOT_STARTED, MOVING, ARRIVED }

/** 멤버 한 명의 현황 카드. [isMe]인 카드가 내 이동 경로 설정과 상세 펼침의 진입점이다. */
data class RouteMemberUiState(
    val id: String,
    val name: String,
    val isMe: Boolean = false,
    val status: RouteMemberStatus = RouteMemberStatus.NOT_STARTED,
    /** "미출발"처럼 이름 옆 칩에 보여 줄 문구. 없으면 칩을 그리지 않는다. */
    val statusLabel: String? = null,
    val description: String,
    val transport: RouteTransport? = null,
    /** 약속 상세가 알려 준 이 멤버의 이동수단. 이동을 시작했다는 소식을 받으면 [transport]로 보여 준다. */
    val plannedTransport: RouteTransport? = null,
    /** 실시간 위치 공유로 받은 마지막 위치. 받은 적이 없으면 null이다. */
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** 약속 이동 단계(ROU-001-03)에서만 쓰는 값. */
data class RouteMovingUiState(
    val myTransport: RouteTransport,
    val myTitle: String,
    /** 지도 위 내 배지에 보일 문구. 예: "버스 이동 중" */
    val myMapLabel: String,
    val mySubtitle: String,
    val routeTotalLabel: String,
    val steps: List<RouteStepUiState>,
    val footnote: String,
    val timeSummary: RouteTimeSummaryUiState,
    /** 도착 직전이라 "도착" 버튼을 누를 수 있다. */
    val canArrive: Boolean = false,
    val hasArrived: Boolean = false
)

/** 출발 위치를 어디서 골랐는지. */
enum class RouteStartKind { BASIC, CURRENT, SEARCH }

/**
 * 출발 위치 후보 한 곳. 경로를 조회하려면 좌표가 있어야 해서, 좌표가 없는 후보는 고를 수 없다.
 * [detail]은 후보 이름 아래에 보이는 주소 또는 노선 문구다.
 */
data class RouteStartLocation(
    val id: String,
    val kind: RouteStartKind,
    val name: String,
    val detail: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

/** 현재 위치를 가져오는 중이거나 실패한 이유. */
enum class RouteCurrentLocationStatus { IDLE, LOADING, FAILED, PERMISSION_DENIED }

/** 장소 검색 진행 상태. */
enum class RouteSearchStatus { IDLE, LOADING, EMPTY, FAILED }

/** 출발 위치 선택 화면 상태. 내 기본 위치, 현재 위치, 검색 결과 중 하나를 골라 "여기에서 출발하기"를 누른다. */
data class RouteStartPickerUiState(
    val isOpen: Boolean = false,
    val basicLocations: List<RouteStartLocation> = emptyList(),
    val basicLocationsFailed: Boolean = false,
    val current: RouteStartLocation? = null,
    val currentStatus: RouteCurrentLocationStatus = RouteCurrentLocationStatus.IDLE,
    val query: String = "",
    val searchResults: List<RouteStartLocation> = emptyList(),
    val searchStatus: RouteSearchStatus = RouteSearchStatus.IDLE,
    val selected: RouteStartLocation? = null
) {
    fun isSelected(id: String): Boolean = selected?.id == id

    /** "여기에서 출발하기"를 누를 수 있는지. 좌표가 있는 위치를 골라야 한다. */
    val canConfirm: Boolean get() = selected?.hasCoordinates == true
}

/** 서버 요청이 진행 중인지 실패했는지. */
enum class RouteRequestStatus { IDLE, LOADING, FAILED }

/** 화면이 보여 주는 단계. 초기화면 → 내 이동 경로 설정 → 약속 이동 순으로 넘어간다. */
enum class RouteStage { OVERVIEW, SELECT, MOVING }

/**
 * 이동 경로 화면(ROU-001) 상태. 화면에 보일 문구와 시간은 모두 바깥에서 만들어 넘기고
 * 이 클래스는 단계 전환 규칙만 가진다. 서버 요청은 [RouteViewModel]이 맡는다.
 */
data class RouteUiState(
    val appointmentTitle: String = "",
    val memberSummary: String = "",
    val destinationName: String = "",
    val stage: RouteStage = RouteStage.OVERVIEW,
    val meetingTimeLabel: String = "",
    val meetingTimeNote: String = "",
    /** 도착지 카드 오른쪽 두 줄. 예: "도착 예정 오후 4:50" / "약속 오후 5:00 · 10분 여유" */
    val destinationEstimateTitle: String = "",
    val destinationEstimateNote: String = "",
    val members: List<RouteMemberUiState> = emptyList(),
    val options: List<RouteOptionUiState> = emptyList(),
    val selectedTransport: RouteTransport? = null,
    val expandedTransport: RouteTransport? = null,
    val moving: RouteMovingUiState? = null,
    val isMyRouteExpanded: Boolean = false,
    /** 내가 고른 출발 위치. 아직 고르지 않았으면 null이다. */
    val startLocation: RouteStartLocation? = null,
    val startPicker: RouteStartPickerUiState = RouteStartPickerUiState(),
    /** 고른 출발 위치에서 이동수단 후보를 불러오는 상태. */
    val optionsStatus: RouteRequestStatus = RouteRequestStatus.IDLE,
    /** "지금 출발하기"와 "도착" 요청 상태. */
    val actionStatus: RouteRequestStatus = RouteRequestStatus.IDLE,
    /** 위치 접근 권한을 허용했는지. 화면이 앞에 올 때마다 새로 확인한다. */
    val locationPermissionGranted: Boolean = true,
    /** 약속이 확정된 날짜(`yyyy-MM-dd`). 알 수 없으면 null이다. */
    val meetingDate: String? = null,
    /** 오늘이 약속 당일이라 "지금 출발하기"를 누를 수 있는지. 약속 날짜를 모르면 막지 않는다. */
    val departureOpen: Boolean = true
) {
    /**
     * 위치 권한 없음 화면(COM-003-03)을 보여 줄지. 실시간 위치 공유가 이어지는 이동 중에만 해당하며,
     * 도착한 뒤에는 위치를 더 보내지 않으므로 권한이 없어도 보여 주지 않는다.
     */
    val needsLocationPermission: Boolean
        get() = stage == RouteStage.MOVING && moving?.hasArrived != true && !locationPermissionGranted

    val selectedOption: RouteOptionUiState?
        get() = options.firstOrNull { it.transport == selectedTransport }

    /**
     * 경로 선택 단계에서 "지금 출발하기"를 누를 수 있는지. 이동수단을 골라야 하고 이동 정보가 있어야 한다.
     * 서버에서 받은 이동수단(소요 시간이 있는 옵션)은 출발한 뒤에 이동 정보를 만들고,
     * 이 후보는 출발 위치를 고른 뒤에만 불러오므로 출발 위치 없이는 고를 수 없다.
     * 요청이 진행 중일 때는 누를 수 없고, 약속 당일이 아니면([departureOpen]) 경로를 골라도 누를 수 없다.
     */
    fun canStart(): Boolean {
        val option = selectedOption ?: return false
        val hasMovingInfo = moving != null || (option.durationMinutes > 0 && startLocation != null)
        return stage == RouteStage.SELECT && hasMovingInfo && departureOpen &&
            actionStatus != RouteRequestStatus.LOADING
    }

    /** 약속 이동 단계의 "도착" 버튼을 누를 수 있는지. */
    fun canArrive(): Boolean {
        val moving = moving ?: return false
        return stage == RouteStage.MOVING && moving.canArrive && !moving.hasArrived &&
            actionStatus != RouteRequestStatus.LOADING
    }

    /** 초기화면에서 내 카드를 눌러 경로 선택으로 들어간다. 이미 다른 단계면 그대로 둔다. */
    fun openRouteSelection(): RouteUiState =
        if (stage == RouteStage.OVERVIEW) copy(stage = RouteStage.SELECT) else this

    /** 경로 선택에서 초기화면으로 돌아간다. 펼친 상세는 접고, 고른 이동수단은 남겨 둔다. */
    fun closeRouteSelection(): RouteUiState =
        if (stage == RouteStage.SELECT) {
            copy(stage = RouteStage.OVERVIEW, expandedTransport = null)
        } else {
            this
        }

    /** 경로 선택에서 도착지 카드를 눌러 출발 위치 선택 화면을 연다. 이미 고른 위치가 있으면 선택된 채로 보인다. */
    fun openStartPicker(): RouteUiState =
        if (stage == RouteStage.SELECT) {
            copy(startPicker = startPicker.copy(isOpen = true, selected = startLocation))
        } else {
            this
        }

    /** 위치를 고르지 않고 출발 위치 선택 화면을 닫는다. */
    fun closeStartPicker(): RouteUiState =
        if (startPicker.isOpen) copy(startPicker = startPicker.copy(isOpen = false)) else this

    /** 후보를 고른다. 열려 있고 좌표가 있는 후보만 고를 수 있다. */
    fun selectStartCandidate(candidate: RouteStartLocation): RouteUiState =
        if (startPicker.isOpen && candidate.hasCoordinates) {
            copy(startPicker = startPicker.copy(selected = candidate))
        } else {
            this
        }

    /** "여기에서 출발하기". 고른 위치를 출발 위치로 확정하고 화면을 닫는다. */
    fun confirmStartLocation(): RouteUiState =
        if (startPicker.isOpen && startPicker.canConfirm) {
            copy(startLocation = startPicker.selected, startPicker = startPicker.copy(isOpen = false))
        } else {
            this
        }

    /** 줄 전체를 누르면 이동수단을 고른다. 목록에 없는 이동수단은 무시한다. */
    fun selectTransport(transport: RouteTransport): RouteUiState {
        val option = options.firstOrNull { it.transport == transport }
        return if (stage == RouteStage.SELECT && option != null) {
            copy(
                selectedTransport = transport,
                destinationEstimateTitle = option.estimateTitle ?: destinationEstimateTitle,
                destinationEstimateNote = option.estimateNote ?: destinationEstimateNote
            )
        } else {
            this
        }
    }

    /** 줄 오른쪽 화살표를 누르면 상세를 펼치거나 접는다. 한 번에 하나만 펼친다. */
    fun toggleOptionExpanded(transport: RouteTransport): RouteUiState =
        if (stage == RouteStage.SELECT && options.any { it.transport == transport }) {
            copy(expandedTransport = if (expandedTransport == transport) null else transport)
        } else {
            this
        }

    /** 경로 선택을 마치고 약속 이동으로 넘어간다. [canStart]가 아니면 그대로 둔다. */
    fun startMoving(): RouteUiState =
        if (canStart()) copy(stage = RouteStage.MOVING, expandedTransport = null) else this

    /**
     * 서버에 출발을 알린 뒤 약속 이동 단계로 넘어간다. 이동 정보는 서버에서 받은 이동수단으로 만든 값이다.
     * 내 멤버 카드는 이동 중으로 바꾸고, 선택 단계에서 열어 둔 상세는 접는다.
     */
    fun departed(moving: RouteMovingUiState): RouteUiState =
        if (stage == RouteStage.SELECT) {
            copy(
                stage = RouteStage.MOVING,
                expandedTransport = null,
                moving = moving,
                actionStatus = RouteRequestStatus.IDLE,
                members = members.map { member ->
                    if (member.isMe) {
                        member.copy(
                            status = RouteMemberStatus.MOVING,
                            statusLabel = null,
                            description = moving.mySubtitle,
                            transport = moving.myTransport
                        )
                    } else {
                        member
                    }
                }
            )
        } else {
            this
        }

    /** 이동 중에 남은 시간이 바뀌었을 때. 내 카드의 한 줄도 같이 바꾼다. */
    fun withMoving(moving: RouteMovingUiState): RouteUiState =
        if (stage == RouteStage.MOVING) {
            copy(
                moving = moving,
                members = members.map { member ->
                    if (member.isMe) member.copy(description = moving.mySubtitle) else member
                }
            )
        } else {
            this
        }

    /** [id] 멤버 한 명(나 제외)의 카드를 [transform]으로 바꾼다. 없는 멤버면 그대로 둔다. */
    fun updateFriend(id: String, transform: (RouteMemberUiState) -> RouteMemberUiState): RouteUiState =
        if (members.any { it.id == id && !it.isMe }) {
            copy(members = members.map { if (it.id == id && !it.isMe) transform(it) else it })
        } else {
            this
        }

    fun toggleMyRouteExpanded(): RouteUiState =
        if (stage == RouteStage.MOVING) copy(isMyRouteExpanded = !isMyRouteExpanded) else this

    /** 서버가 위치로 도착을 알아챘을 때. 버튼이 아직 꺼져 있어도 이동 중이면 도착 완료로 바꾼다. */
    fun forceArrive(): RouteUiState {
        val current = moving
        return if (stage == RouteStage.MOVING && current != null && !current.hasArrived) {
            copy(
                moving = current.copy(canArrive = false, hasArrived = true),
                members = members.map { member ->
                    if (member.isMe) member.copy(status = RouteMemberStatus.ARRIVED) else member
                }
            )
        } else {
            this
        }
    }

    /** 도착 버튼. 도착 직전이 아니면 그대로 둔다. */
    fun arrive(): RouteUiState {
        val current = moving
        return if (canArrive() && current != null) {
            copy(
                moving = current.copy(canArrive = false, hasArrived = true),
                members = members.map { member ->
                    if (member.isMe) member.copy(status = RouteMemberStatus.ARRIVED) else member
                }
            )
        } else {
            this
        }
    }
}
