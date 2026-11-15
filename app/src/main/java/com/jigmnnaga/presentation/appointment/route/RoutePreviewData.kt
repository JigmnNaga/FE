package com.jigmnnaga.presentation.appointment.route

/** 미리보기와 테스트가 함께 쓰는 예시 데이터. 값은 Figma ROU-001 프레임의 문구를 그대로 옮겼다. */
internal object RouteSampleData {
    private val busSteps = listOf(
        RouteStepUiState(RouteStepKind.WALK, "도보 5분", "정류장까지 도보 이동", "오후 4:15 출발 · 4:20 승차"),
        RouteStepUiState(RouteStepKind.BUS, "버스 24분", "버스 탑승", "오후 4:20 승차 · 4:44 하차"),
        RouteStepUiState(RouteStepKind.WALK, "도보 6분", "도착지까지 도보 이동", "오후 4:44 출발 · 4:50 도착")
    )

    private val subwaySteps = listOf(
        RouteStepUiState(RouteStepKind.WALK, "도보 6분", "지하철역까지 도보 이동", "오후 4:15 출발 · 4:21 승차"),
        RouteStepUiState(RouteStepKind.SUBWAY, "지하철 20분", "지하철 탑승", "오후 4:21 승차 · 4:41 하차"),
        RouteStepUiState(RouteStepKind.WALK, "도보 6분", "도착지까지 도보 이동", "오후 4:41 출발 · 4:47 도착")
    )

    private val taxiSteps = listOf(
        RouteStepUiState(RouteStepKind.START, "출발", "현재 위치에서 출발", "오후 4:15 출발"),
        RouteStepUiState(RouteStepKind.TAXI, "택시 22분", "택시로 이동", "오후 4:15 탑승 · 4:37 하차"),
        RouteStepUiState(RouteStepKind.ARRIVAL, "도착", "강남역 카공카페", "오후 4:37 도착")
    )

    val options = listOf(
        RouteOptionUiState(
            transport = RouteTransport.BUS,
            title = "버스",
            durationLabel = "35분",
            summary = "도보 5분 → 버스 24분 → 도보 6분",
            steps = busSteps,
            timeSummary = RouteTimeSummaryUiState(
                startCaption = "권장 출발 시간",
                startTime = "오후 4:15",
                startNote = "(현재 위치에서 출발 시)",
                endCaption = "도착 예정",
                endTime = "오후 4:50",
                endNote = "약속 10분 전"
            )
        ),
        RouteOptionUiState(
            transport = RouteTransport.SUBWAY,
            title = "지하철",
            durationLabel = "32분",
            summary = "도보 6분 → 지하철 20분 → 도보 6분",
            steps = subwaySteps,
            timeSummary = RouteTimeSummaryUiState(
                startCaption = "권장 출발 시간",
                startTime = "오후 4:15",
                startNote = "(현재 위치에서 출발 시)",
                endCaption = "도착 예정",
                endTime = "오후 4:47",
                endNote = "약속 13분 전"
            )
        ),
        RouteOptionUiState(
            transport = RouteTransport.TAXI,
            title = "택시",
            durationLabel = "22분",
            summary = "도보 없음 → 택시 22분",
            steps = taxiSteps,
            timeSummary = RouteTimeSummaryUiState(
                startCaption = "권장 출발 시간",
                startTime = "오후 4:15",
                startNote = "(현재 위치에서 출발 시)",
                endCaption = "도착 예정",
                endTime = "오후 4:37",
                endNote = "약속 23분 전"
            )
        )
    )

    val movingBus = RouteMovingUiState(
        myTransport = RouteTransport.BUS,
        myTitle = "나 · 버스 이동 중",
        myMapLabel = "버스 이동 중",
        mySubtitle = "오후 4:50 도착 · 25분 남음",
        routeTotalLabel = "전체 35분",
        steps = listOf(
            RouteStepUiState(RouteStepKind.WALK, "도보 5분", "정류장까지 도보 이동", "오후 4:15~4:20"),
            RouteStepUiState(RouteStepKind.BUS, "버스 24분", "버스 탑승", "오후 4:20 승차 · 4:44 하차 예정"),
            RouteStepUiState(RouteStepKind.WALK, "도보 6분", "도착지까지 도보 이동", "오후 4:44~4:50")
        ),
        footnote = "전체 35분 중 10분이 지났어요. 25분 후 도착 예정이에요.",
        timeSummary = RouteTimeSummaryUiState(
            startCaption = "현재 시각",
            startTime = "오후 4:25",
            startNote = "버스 이동 중",
            endCaption = "도착 예정",
            endTime = "오후 4:50",
            endNote = "25분 남음"
        )
    )

    private val friends = listOf(
        RouteMemberUiState(
            id = "2",
            name = "태양",
            status = RouteMemberStatus.NOT_STARTED,
            statusLabel = "미출발",
            description = "아직 출발 전이에요"
        ),
        RouteMemberUiState(
            id = "3",
            name = "하은",
            status = RouteMemberStatus.NOT_STARTED,
            statusLabel = "미출발",
            description = "아직 출발 전이에요"
        )
    )

    private val myMemberBeforeRoute = RouteMemberUiState(
        id = "1",
        name = "나",
        isMe = true,
        status = RouteMemberStatus.NOT_STARTED,
        statusLabel = "미출발",
        description = "이동 경로를 설정해주세요."
    )

    /** 초기화면(ROU-001-01). 내 경로가 아직 없다. */
    val overview = RouteUiState(
        appointmentTitle = "중간고사 카공 모임",
        memberSummary = "3명 · 태양, 하은, 나",
        destinationName = "강남역 카공카페",
        meetingTimeLabel = "오후 5:00",
        meetingTimeNote = "모두 도착 예정",
        destinationEstimateTitle = "도착 예정 오후 4:50",
        destinationEstimateNote = "약속 오후 5:00 · 10분 여유",
        members = listOf(myMemberBeforeRoute) + friends,
        options = options,
        moving = movingBus
    )

    /** 내 이동 경로 설정(ROU-001-02), 이동수단을 고르기 전. */
    val selecting = overview.openRouteSelection()

    /** 약속 이동(ROU-001-03). 버스를 골라 출발한 상태. */
    val moving: RouteUiState = selecting.selectTransport(RouteTransport.BUS).startMoving().copy(
        members = listOf(
            myMemberBeforeRoute.copy(
                status = RouteMemberStatus.MOVING,
                statusLabel = null,
                description = movingBus.mySubtitle,
                transport = RouteTransport.BUS
            ),
            friends[0].copy(
                status = RouteMemberStatus.MOVING,
                statusLabel = null,
                description = "지하철로 이동 중",
                transport = RouteTransport.SUBWAY
            ),
            friends[1].copy(
                status = RouteMemberStatus.MOVING,
                statusLabel = null,
                description = "버스로 이동 중",
                transport = RouteTransport.BUS
            )
        )
    )

    /** 도착 직전. 남은 시간이 1분이고 "도착" 버튼이 켜진다. */
    val nearArrival: RouteUiState = moving.copy(
        moving = movingBus.copy(
            mySubtitle = "오후 4:50 도착 · 1분 남음",
            timeSummary = movingBus.timeSummary.copy(
                startTime = "오후 4:49",
                endNote = "1분 남음"
            ),
            canArrive = true
        ),
        members = moving.members.map { member ->
            if (member.isMe) member.copy(description = "오후 4:50 도착 · 1분 남음") else member
        }
    )

    /** 도착 완료. 버튼은 다시 꺼진다. */
    val arrived: RouteUiState = nearArrival.arrive().let { state ->
        state.copy(
            destinationEstimateTitle = "도착 완료 오후 4:50",
            destinationEstimateNote = "약속 10분 전 도착",
            moving = state.moving?.copy(
                myTitle = "나 · 도착 완료",
                mySubtitle = "오후 4:50 · 약속 10분 전 도착",
                timeSummary = RouteTimeSummaryUiState(
                    startCaption = "도착 시각",
                    startTime = "오후 4:50",
                    startNote = "약속 장소 도착",
                    endCaption = "약속 시각",
                    endTime = "오후 5:00",
                    endNote = "10분 일찍 도착"
                )
            ),
            members = state.members.map { member ->
                if (member.isMe) {
                    member.copy(description = "오후 4:50 · 약속 10분 전 도착")
                } else {
                    member
                }
            }
        )
    }
}
