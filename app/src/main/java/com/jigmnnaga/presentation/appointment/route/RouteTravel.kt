package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelOption
import com.jigmnnaga.domain.model.TravelOptions
import com.jigmnnaga.domain.model.TravelStep
import com.jigmnnaga.domain.model.TravelStepType
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 이동수단 후보와 이동 중 문구에 쓰는 문구 틀. `%1\$s`, `%1\$d` 같은 자리는 [String.format]으로 채운다.
 * 화면 전체가 한국어 문구라서 기본값을 한국어로 둔다.
 */
data class RouteTravelLabels(
    val transportBus: String = "버스",
    val transportSubway: String = "지하철",
    val transportTaxi: String = "택시",
    val durationMinutes: String = "%1\$d분",
    val stepWalk: String = "도보 %1\$d분",
    val stepBus: String = "버스 %1\$d분",
    val stepSubway: String = "지하철 %1\$d분",
    val stepTaxi: String = "택시 %1\$d분",
    val stepOther: String = "이동 %1\$d분",
    val walkNone: String = "도보 없음",
    val stepStart: String = "출발",
    val stepArrival: String = "도착",
    val startDescription: String = "%1\$s에서 출발",
    val walkToStopDescription: String = "정류장까지 도보 이동",
    val walkToStationDescription: String = "지하철역까지 도보 이동",
    val walkToDestinationDescription: String = "도착지까지 도보 이동",
    val stepBusDescription: String = "버스 탑승",
    val stepSubwayDescription: String = "지하철 탑승",
    val stepTaxiDescription: String = "택시로 이동",
    val stepOtherDescription: String = "이동",
    val stepTimeRange: String = "%1\$s~%2\$s",
    val startTime: String = "%1\$s 출발",
    val arrivalTime: String = "%1\$s 도착",
    val walkBoardTimes: String = "%1\$s 출발 · %2\$s 승차",
    val walkArriveTimes: String = "%1\$s 출발 · %2\$s 도착",
    val rideTimes: String = "%1\$s 승차 · %2\$s 하차",
    val movingRideTimes: String = "%1\$s 승차 · %2\$s 하차 예정",
    val taxiTimes: String = "%1\$s 탑승 · %2\$s 하차",
    val movingTaxiTimes: String = "%1\$s 탑승 · %2\$s 하차 예정",
    val summaryArrow: String = " → ",
    val recommendedStartCaption: String = "권장 출발 시간",
    val recommendedStartNote: String = "(%1\$s에서 출발 시)",
    val arrivalCaption: String = "도착 예정",
    val minutesBeforeMeeting: String = "약속 %1\$d분 전",
    val minutesAfterMeeting: String = "약속 %1\$d분 후",
    val justInTime: String = "약속 시각에 도착",
    val estimateTitle: String = "도착 예정 %1\$s",
    val estimateNoteEarly: String = "약속 %1\$s · %2\$d분 여유",
    val estimateNoteLate: String = "약속 %1\$s · %2\$d분 늦음",
    val estimateNoteJustInTime: String = "약속 %1\$s · 여유 없음",
    val movingTitle: String = "나 · %1\$s 이동 중",
    val movingMapLabel: String = "%1\$s 이동 중",
    val movingSubtitle: String = "%1\$s 도착 · %2\$d분 남음",
    val routeTotal: String = "전체 %1\$d분",
    val movingFootnote: String = "전체 %1\$d분 중 %2\$d분이 지났어요. %3\$d분 후 도착 예정이에요.",
    val nowCaption: String = "현재 시각",
    val remainingNote: String = "%1\$d분 남음",
    val arrivedTitle: String = "나 · 도착 완료",
    val arrivedSubtitle: String = "%1\$s 도착 완료",
    val arrivedSubtitleEarly: String = "%1\$s · 약속 %2\$d분 전 도착",
    val arrivedSubtitleLate: String = "%1\$s · 약속 %2\$d분 후 도착",
    val arrivedSubtitleJustInTime: String = "%1\$s · 정시 도착",
    val arrivedTimeCaption: String = "도착 시각",
    val arrivedPlaceNote: String = "약속 장소 도착",
    val meetingCaption: String = "약속 시각",
    val arrivedNoteEarly: String = "%1\$d분 일찍 도착",
    val arrivedNoteLate: String = "%1\$d분 늦게 도착",
    val arrivedNoteJustInTime: String = "정시 도착",
    val arrivedEstimateTitle: String = "도착 완료 %1\$s",
    val arrivedEstimateNoteEarly: String = "약속 %1\$d분 전 도착",
    val arrivedEstimateNoteLate: String = "약속 %1\$d분 후 도착",
    val arrivedEstimateNoteJustInTime: String = "약속 시각에 도착"
) {
    fun transportName(mode: TravelMode): String = when (mode) {
        TravelMode.BUS -> transportBus
        TravelMode.SUBWAY -> transportSubway
        TravelMode.TAXI -> transportTaxi
    }
}

/**
 * minSdk 24에서는 java.time을 쓸 수 없어서 서버 시각 문자열(`yyyy-MM-dd'T'HH:mm:ss`)을
 * [SimpleDateFormat]으로 다룬다. 기기의 시간대를 서버와 같은 한국 시간으로 본다.
 */
internal object RouteTime {
    private const val SERVER_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val SERVER_TEXT_LENGTH = 19
    private const val CLOCK_FORMAT = "a h:mm"
    private const val MILLIS_PER_MINUTE = 60_000L

    fun parseServer(value: String?): Long? {
        val text = value?.takeIf { it.length >= SERVER_TEXT_LENGTH }?.take(SERVER_TEXT_LENGTH) ?: return null
        return try {
            SimpleDateFormat(SERVER_FORMAT, Locale.KOREA).parse(text)?.time
        } catch (e: ParseException) {
            null
        }
    }

    /** 오후 4:50 같은 시각 문구. */
    fun formatClock(millis: Long): String = SimpleDateFormat(CLOCK_FORMAT, Locale.KOREA).format(Date(millis))

    /** 같은 문구 안에서 오전·오후를 한 번만 쓰고 싶을 때 쓰는 4:20 같은 시각. */
    fun formatClockShort(millis: Long): String = SimpleDateFormat("h:mm", Locale.KOREA).format(Date(millis))

    /** `HH:mm` 문자열을 오후 5:00 같은 문구로 바꾼다. 읽을 수 없으면 빈 문자열. */
    fun formatClockText(hhmm: String?): String {
        if (hhmm.isNullOrBlank()) return ""
        return try {
            val parsed = SimpleDateFormat("HH:mm", Locale.KOREA).parse(hhmm.take(5)) ?: return ""
            SimpleDateFormat(CLOCK_FORMAT, Locale.KOREA).format(parsed)
        } catch (e: ParseException) {
            ""
        }
    }

    fun minutesToMillis(minutes: Int): Long = minutes * MILLIS_PER_MINUTE

    /** [from]에서 [to]까지 분 단위. [to]가 더 나중이면 양수. 초는 버린다. */
    fun minutesBetween(from: Long, to: Long): Long = Math.floorDiv(to - from, MILLIS_PER_MINUTE)
}

/** 서버의 이동수단 후보를 경로 선택 화면(ROU-001-02)의 이동수단 줄로 바꾼다. */
internal fun TravelOptions.toRouteOptions(
    startName: String?,
    labels: RouteTravelLabels
): List<RouteOptionUiState> {
    val appointmentMillis = RouteTime.parseServer(appointmentDateTime)
    return options.map { option ->
        option.toRouteOption(appointmentMillis, startName, destinationName?.takeIf { it.isNotBlank() }, labels)
    }
}

private fun TravelOption.toRouteOption(
    appointmentMillis: Long?,
    startName: String?,
    destinationName: String?,
    labels: RouteTravelLabels
): RouteOptionUiState {
    val arrivalMillis = RouteTime.parseServer(estimatedArrivalTime)
    val startMillis = arrivalMillis?.let { it - RouteTime.minutesToMillis(durationMinutes) }
    val summary = when {
        mode == TravelMode.TAXI ->
            "${labels.walkNone}${labels.summaryArrow}${labels.stepTaxi.format(durationMinutes)}"
        steps.isEmpty() -> labels.durationMinutes.format(durationMinutes)
        else -> steps.joinToString(labels.summaryArrow) { it.label(labels) }
    }
    val recommendedStart = appointmentMillis?.minus(RouteTime.minutesToMillis(durationMinutes))
    val gap = if (appointmentMillis != null && arrivalMillis != null) {
        RouteTime.minutesBetween(arrivalMillis, appointmentMillis)
    } else {
        null
    }
    return RouteOptionUiState(
        transport = mode.toRouteTransport(),
        title = labels.transportName(mode),
        durationLabel = labels.durationMinutes.format(durationMinutes),
        summary = summary,
        steps = buildRouteSteps(
            steps, mode, durationMinutes, startMillis, startName, destinationName, labels, moving = false
        ),
        timeSummary = if (recommendedStart != null && arrivalMillis != null && gap != null) {
            RouteTimeSummaryUiState(
                startCaption = labels.recommendedStartCaption,
                startTime = RouteTime.formatClock(recommendedStart),
                startNote = startName?.let { labels.recommendedStartNote.format(it) }.orEmpty(),
                endCaption = labels.arrivalCaption,
                endTime = RouteTime.formatClock(arrivalMillis),
                endNote = gap.meetingGapNote(labels)
            )
        } else {
            null
        },
        durationMinutes = durationMinutes,
        estimateTitle = arrivalMillis?.let { labels.estimateTitle.format(RouteTime.formatClock(it)) },
        estimateNote = if (appointmentMillis != null && gap != null) {
            val meeting = RouteTime.formatClock(appointmentMillis)
            when {
                gap > 0 -> labels.estimateNoteEarly.format(meeting, gap.toInt())
                gap < 0 -> labels.estimateNoteLate.format(meeting, (-gap).toInt())
                else -> labels.estimateNoteJustInTime.format(meeting)
            }
        } else {
            null
        },
        travelSteps = steps,
        startName = startName,
        destinationName = destinationName
    )
}

/**
 * 이동 경로 상세의 구간 목록(Figma ROU-001-02 / 03). 시작 시각([startMillis])부터 구간 시간을 이어 붙여
 * 구간마다 시간 문구를 만든다. 택시는 출발 → 택시 → 도착으로 보여 주고, 대중교통은 서버가 준 구간 그대로 보여 준다.
 * 이동 중([moving])이면 하차 시각 뒤에 "예정"을 붙이고 도보는 시각 범위로 보여 준다.
 */
internal fun buildRouteSteps(
    steps: List<TravelStep>,
    mode: TravelMode,
    durationMinutes: Int,
    startMillis: Long?,
    startName: String?,
    destinationName: String?,
    labels: RouteTravelLabels,
    moving: Boolean
): List<RouteStepUiState> {
    fun times(template: String, from: Long?, to: Long?): String =
        if (from != null && to != null) {
            template.format(RouteTime.formatClock(from), RouteTime.formatClockShort(to))
        } else {
            ""
        }

    if (mode == TravelMode.TAXI) {
        if (steps.isEmpty()) return emptyList()
        val end = startMillis?.plus(RouteTime.minutesToMillis(durationMinutes))
        return listOf(
            RouteStepUiState(
                kind = RouteStepKind.START,
                title = labels.stepStart,
                description = startName?.let { labels.startDescription.format(it) }.orEmpty(),
                timeLabel = startMillis?.let { labels.startTime.format(RouteTime.formatClock(it)) }.orEmpty()
            ),
            RouteStepUiState(
                kind = RouteStepKind.TAXI,
                title = labels.stepTaxi.format(durationMinutes),
                description = labels.stepTaxiDescription,
                timeLabel = times(if (moving) labels.movingTaxiTimes else labels.taxiTimes, startMillis, end)
            ),
            RouteStepUiState(
                kind = RouteStepKind.ARRIVAL,
                title = labels.stepArrival,
                description = destinationName.orEmpty(),
                timeLabel = end?.let { labels.arrivalTime.format(RouteTime.formatClock(it)) }.orEmpty()
            )
        )
    }

    var cursor = startMillis
    return steps.mapIndexed { index, step ->
        val from = cursor
        val to = from?.plus(RouteTime.minutesToMillis(step.minutes))
        cursor = to
        val next = steps.getOrNull(index + 1)
        val boardsNext = next?.type == TravelStepType.BUS || next?.type == TravelStepType.SUBWAY
        val description: String
        val timeLabel: String
        when (step.type) {
            TravelStepType.WALK -> {
                description = when {
                    boardsNext && next?.type == TravelStepType.SUBWAY -> labels.walkToStationDescription
                    boardsNext -> labels.walkToStopDescription
                    else -> labels.walkToDestinationDescription
                }
                timeLabel = when {
                    moving -> times(labels.stepTimeRange, from, to)
                    boardsNext -> times(labels.walkBoardTimes, from, to)
                    else -> times(labels.walkArriveTimes, from, to)
                }
            }
            TravelStepType.BUS, TravelStepType.SUBWAY -> {
                description = if (step.type == TravelStepType.BUS) {
                    labels.stepBusDescription
                } else {
                    labels.stepSubwayDescription
                }
                timeLabel = times(if (moving) labels.movingRideTimes else labels.rideTimes, from, to)
            }
            TravelStepType.TAXI -> {
                description = labels.stepTaxiDescription
                timeLabel = times(if (moving) labels.movingTaxiTimes else labels.taxiTimes, from, to)
            }
            TravelStepType.OTHER -> {
                description = labels.stepOtherDescription
                timeLabel = times(labels.stepTimeRange, from, to)
            }
        }
        RouteStepUiState(
            kind = step.toRouteStepKind(),
            title = step.label(labels),
            description = description,
            timeLabel = timeLabel
        )
    }
}

private fun Long.meetingGapNote(labels: RouteTravelLabels): String = when {
    this > 0 -> labels.minutesBeforeMeeting.format(toInt())
    this < 0 -> labels.minutesAfterMeeting.format((-this).toInt())
    else -> labels.justInTime
}

private fun TravelMode.toRouteTransport(): RouteTransport = when (this) {
    TravelMode.BUS -> RouteTransport.BUS
    TravelMode.SUBWAY -> RouteTransport.SUBWAY
    TravelMode.TAXI -> RouteTransport.TAXI
}

internal fun RouteTransport.toTravelMode(): TravelMode = when (this) {
    RouteTransport.BUS -> TravelMode.BUS
    RouteTransport.SUBWAY -> TravelMode.SUBWAY
    RouteTransport.TAXI -> TravelMode.TAXI
}

private fun TravelStep.label(labels: RouteTravelLabels): String = when (type) {
    TravelStepType.WALK -> labels.stepWalk
    TravelStepType.BUS -> labels.stepBus
    TravelStepType.SUBWAY -> labels.stepSubway
    TravelStepType.TAXI -> labels.stepTaxi
    TravelStepType.OTHER -> labels.stepOther
}.format(minutes)

private fun TravelStep.toRouteStepKind(): RouteStepKind = when (type) {
    TravelStepType.BUS -> RouteStepKind.BUS
    TravelStepType.SUBWAY -> RouteStepKind.SUBWAY
    TravelStepType.TAXI -> RouteStepKind.TAXI
    else -> RouteStepKind.WALK
}

/**
 * 출발한 시각([departedAt])과 지금([now])으로 약속 이동 단계(ROU-001-03)의 값을 만든다.
 * 도착까지 1분 이하로 남으면 "도착" 버튼을 켜고, 한 번 켜진 버튼은 도착하기 전까지 끄지 않는다.
 */
internal fun RouteOptionUiState.toMoving(
    departedAt: Long,
    now: Long,
    labels: RouteTravelLabels,
    canArriveAlready: Boolean = false
): RouteMovingUiState {
    val arrival = departedAt + RouteTime.minutesToMillis(durationMinutes)
    val remaining = ((arrival - now + 59_999L) / 60_000L).coerceAtLeast(0L).toInt()
    val elapsed = (durationMinutes - remaining).coerceIn(0, durationMinutes)
    val arrivalClock = RouteTime.formatClock(arrival)
    val movingSteps = if (travelSteps.isNotEmpty()) {
        buildRouteSteps(
            travelSteps, transport.toTravelMode(), durationMinutes, departedAt,
            startName, destinationName, labels, moving = true
        )
    } else {
        steps
    }
    return RouteMovingUiState(
        myTransport = transport,
        myTitle = labels.movingTitle.format(title),
        myMapLabel = labels.movingMapLabel.format(title),
        mySubtitle = labels.movingSubtitle.format(arrivalClock, remaining),
        routeTotalLabel = labels.routeTotal.format(durationMinutes),
        steps = movingSteps,
        footnote = labels.movingFootnote.format(durationMinutes, elapsed, remaining),
        timeSummary = RouteTimeSummaryUiState(
            startCaption = labels.nowCaption,
            startTime = RouteTime.formatClock(now),
            startNote = labels.movingMapLabel.format(title),
            endCaption = labels.arrivalCaption,
            endTime = arrivalClock,
            endNote = labels.remainingNote.format(remaining)
        ),
        canArrive = canArriveAlready || remaining <= 1
    )
}

/**
 * 도착을 마친 뒤의 약속 이동 값. 약속 시각([appointmentMillis])을 알면 얼마나 일찍·늦게 도착했는지도 보여 준다.
 */
internal fun RouteMovingUiState.toArrived(
    now: Long,
    appointmentMillis: Long?,
    labels: RouteTravelLabels
): RouteMovingUiState {
    val clock = RouteTime.formatClock(now)
    val gap = appointmentMillis?.let { RouteTime.minutesBetween(now, it) }
    return copy(
        myTitle = labels.arrivedTitle,
        mySubtitle = when {
            gap == null -> labels.arrivedSubtitle.format(clock)
            gap > 0 -> labels.arrivedSubtitleEarly.format(clock, gap.toInt())
            gap < 0 -> labels.arrivedSubtitleLate.format(clock, (-gap).toInt())
            else -> labels.arrivedSubtitleJustInTime.format(clock)
        },
        canArrive = false,
        hasArrived = true,
        timeSummary = RouteTimeSummaryUiState(
            startCaption = labels.arrivedTimeCaption,
            startTime = clock,
            startNote = labels.arrivedPlaceNote,
            endCaption = if (appointmentMillis != null) labels.meetingCaption else timeSummary.endCaption,
            endTime = appointmentMillis?.let { RouteTime.formatClock(it) } ?: timeSummary.endTime,
            endNote = when {
                gap == null -> ""
                gap > 0 -> labels.arrivedNoteEarly.format(gap.toInt())
                gap < 0 -> labels.arrivedNoteLate.format((-gap).toInt())
                else -> labels.arrivedNoteJustInTime
            }
        )
    )
}

/** 도착을 마친 뒤 도착지 카드 오른쪽 두 번째 줄. 약속 시각을 모르면 빈 문자열. */
internal fun arrivedEstimateNote(now: Long, appointmentMillis: Long?, labels: RouteTravelLabels): String {
    val gap = appointmentMillis?.let { RouteTime.minutesBetween(now, it) } ?: return ""
    return when {
        gap > 0 -> labels.arrivedEstimateNoteEarly.format(gap.toInt())
        gap < 0 -> labels.arrivedEstimateNoteLate.format((-gap).toInt())
        else -> labels.arrivedEstimateNoteJustInTime
    }
}
