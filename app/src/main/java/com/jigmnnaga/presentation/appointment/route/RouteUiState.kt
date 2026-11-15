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
    val timeSummary: RouteTimeSummaryUiState? = null
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
    val transport: RouteTransport? = null
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

/** 화면이 보여 주는 단계. 초기화면 → 내 이동 경로 설정 → 약속 이동 순으로 넘어간다. */
enum class RouteStage { OVERVIEW, SELECT, MOVING }

/**
 * 이동 경로 화면(ROU-001) 상태. 서버 연동과 위치 공유는 후속 작업이라, 화면에 보일 문구와 시간은
 * 모두 바깥에서 만들어 넘기고 이 클래스는 단계 전환 규칙만 가진다.
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
    val isMyRouteExpanded: Boolean = false
) {
    val selectedOption: RouteOptionUiState?
        get() = options.firstOrNull { it.transport == selectedTransport }

    /** 경로 선택 단계에서 "지금 출발하기"를 누를 수 있는지. 이동수단을 골라야 하고 이동 정보가 있어야 한다. */
    fun canStart(): Boolean =
        stage == RouteStage.SELECT && selectedOption != null && moving != null

    /** 약속 이동 단계의 "도착" 버튼을 누를 수 있는지. */
    fun canArrive(): Boolean {
        val moving = moving ?: return false
        return stage == RouteStage.MOVING && moving.canArrive && !moving.hasArrived
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

    /** 줄 전체를 누르면 이동수단을 고른다. 목록에 없는 이동수단은 무시한다. */
    fun selectTransport(transport: RouteTransport): RouteUiState =
        if (stage == RouteStage.SELECT && options.any { it.transport == transport }) {
            copy(selectedTransport = transport)
        } else {
            this
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

    fun toggleMyRouteExpanded(): RouteUiState =
        if (stage == RouteStage.MOVING) copy(isMyRouteExpanded = !isMyRouteExpanded) else this

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
