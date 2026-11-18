package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelOption
import com.jigmnnaga.domain.model.TravelOptions
import com.jigmnnaga.domain.model.TravelStep
import com.jigmnnaga.domain.model.TravelStepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTravelTest {
    private val labels = RouteTravelLabels()

    private fun clock(server: String) = RouteTime.formatClock(RouteTime.parseServer(server)!!)

    private fun short(server: String) = RouteTime.formatClockShort(RouteTime.parseServer(server)!!)

    private val bus = TravelOption(
        mode = TravelMode.BUS,
        durationMinutes = 35,
        distanceMeters = 8000,
        estimatedArrivalTime = "2026-11-20T16:50:00",
        steps = listOf(
            TravelStep(TravelStepType.WALK, 5),
            TravelStep(TravelStepType.BUS, 24, "146"),
            TravelStep(TravelStepType.WALK, 6)
        )
    )

    private fun options(vararg list: TravelOption, appointment: String? = "2026-11-20T17:00:00") =
        TravelOptions(appointmentDateTime = appointment, options = list.toList())

    @Test
    fun `시각 문자열을 읽고 오전 오후 문구로 바꾼다`() {
        val millis = RouteTime.parseServer("2026-11-20T16:50:00.123456")
        assertNotNull(millis)
        assertTrue(RouteTime.formatClock(millis!!).endsWith("4:50"))
        assertNull(RouteTime.parseServer(null))
        assertNull(RouteTime.parseServer("abcdefghijklmnopqrstuvwxyz"))
        assertEquals("", RouteTime.formatClockText(null))
        assertEquals("", RouteTime.formatClockText("abc"))
    }

    @Test
    fun `이동수단 한 줄에 소요 시간과 구간 요약을 채운다`() {
        val option = options(bus).toRouteOptions("강남역", labels).single()

        assertEquals(RouteTransport.BUS, option.transport)
        assertEquals("버스", option.title)
        assertEquals("35분", option.durationLabel)
        assertEquals("도보 5분 → 버스 24분 → 도보 6분", option.summary)
        assertEquals(35, option.durationMinutes)
    }

    @Test
    fun `구간 상세는 Figma 문구대로 출발 시각부터 이어서 시간을 계산한다`() {
        val steps = options(bus).toRouteOptions("강남역", labels).single().steps

        assertEquals(listOf(RouteStepKind.WALK, RouteStepKind.BUS, RouteStepKind.WALK), steps.map { it.kind })
        assertEquals(listOf("도보 5분", "버스 24분", "도보 6분"), steps.map { it.title })
        assertEquals(
            listOf("정류장까지 도보 이동", "버스 탑승", "도착지까지 도보 이동"),
            steps.map { it.description }
        )
        assertEquals(
            "${clock("2026-11-20T16:15:00")} 출발 · ${short("2026-11-20T16:20:00")} 승차",
            steps[0].timeLabel
        )
        assertEquals(
            "${clock("2026-11-20T16:20:00")} 승차 · ${short("2026-11-20T16:44:00")} 하차",
            steps[1].timeLabel
        )
        assertEquals(
            "${clock("2026-11-20T16:44:00")} 출발 · ${short("2026-11-20T16:50:00")} 도착",
            steps[2].timeLabel
        )
    }

    @Test
    fun `지하철 앞 도보는 지하철역까지 도보 이동이라고 보여 준다`() {
        val subway = TravelOption(
            TravelMode.SUBWAY, 32, 9000, "2026-11-20T16:47:00",
            steps = listOf(
                TravelStep(TravelStepType.WALK, 6),
                TravelStep(TravelStepType.SUBWAY, 20, "2호선"),
                TravelStep(TravelStepType.WALK, 6)
            )
        )
        val steps = options(subway).toRouteOptions("집", labels).single().steps

        assertEquals("지하철역까지 도보 이동", steps[0].description)
        assertEquals("지하철 탑승", steps[1].description)
        assertEquals(RouteStepKind.SUBWAY, steps[1].kind)
    }

    @Test
    fun `택시는 출발과 택시와 도착 세 구간으로 보여 준다`() {
        val taxi = TravelOption(
            TravelMode.TAXI, 22, 9000, "2026-11-20T16:37:00",
            steps = listOf(TravelStep(TravelStepType.TAXI, 22))
        )
        val option = TravelOptions(
            destinationName = "강남역 카공카페",
            appointmentDateTime = "2026-11-20T17:00:00",
            options = listOf(taxi)
        ).toRouteOptions("현재 위치", labels).single()

        assertEquals("도보 없음 → 택시 22분", option.summary)
        assertEquals(
            listOf(RouteStepKind.START, RouteStepKind.TAXI, RouteStepKind.ARRIVAL),
            option.steps.map { it.kind }
        )
        assertEquals(listOf("출발", "택시 22분", "도착"), option.steps.map { it.title })
        assertEquals(
            listOf("현재 위치에서 출발", "택시로 이동", "강남역 카공카페"),
            option.steps.map { it.description }
        )
        assertEquals(
            listOf(
                "${clock("2026-11-20T16:15:00")} 출발",
                "${clock("2026-11-20T16:15:00")} 탑승 · ${short("2026-11-20T16:37:00")} 하차",
                "${clock("2026-11-20T16:37:00")} 도착"
            ),
            option.steps.map { it.timeLabel }
        )
    }

    @Test
    fun `권장 출발과 도착 예정과 약속까지 여유를 계산한다`() {
        val option = options(bus).toRouteOptions("강남역", labels).single()
        val summary = option.timeSummary!!

        assertEquals(clock("2026-11-20T16:25:00"), summary.startTime)
        assertEquals("(강남역에서 출발 시)", summary.startNote)
        assertEquals(clock("2026-11-20T16:50:00"), summary.endTime)
        assertEquals("약속 10분 전", summary.endNote)
        assertEquals("도착 예정 ${clock("2026-11-20T16:50:00")}", option.estimateTitle)
        assertEquals("약속 ${clock("2026-11-20T17:00:00")} · 10분 여유", option.estimateNote)
    }

    @Test
    fun `약속 시각보다 늦게 도착하면 늦는 시간을 보여 준다`() {
        val late = bus.copy(estimatedArrivalTime = "2026-11-20T17:07:00")
        val option = options(late).toRouteOptions("집", labels).single()

        assertEquals("약속 7분 후", option.timeSummary!!.endNote)
        assertEquals("약속 ${clock("2026-11-20T17:00:00")} · 7분 늦음", option.estimateNote)
    }

    @Test
    fun `약속 시각이나 도착 예정 시각을 모르면 요약은 빼고 줄만 보여 준다`() {
        val option = options(bus, appointment = null).toRouteOptions("집", labels).single()

        assertNull(option.timeSummary)
        assertNull(option.estimateNote)
        assertEquals("35분", option.durationLabel)

        val noArrival = options(bus.copy(estimatedArrivalTime = null)).toRouteOptions("집", labels).single()
        assertNull(noArrival.timeSummary)
        assertEquals("", noArrival.steps.first().timeLabel)
        assertNull(noArrival.estimateTitle)
    }

    @Test
    fun `구간 정보가 없으면 대중교통 요약은 소요 시간만 보여 주고 구간 상세는 비운다`() {
        val noSteps = TravelOption(TravelMode.BUS, 22, 9000, "2026-11-20T16:37:00")
        val option = options(noSteps).toRouteOptions("집", labels).single()

        assertEquals("22분", option.summary)
        assertTrue(option.steps.isEmpty())
    }

    @Test
    fun `출발한 직후에는 전체 시간이 남아 있고 도착 버튼은 꺼져 있다`() {
        val option = options(bus).toRouteOptions("집", labels).single()
        val start = 1_000_000L

        val moving = option.toMoving(departedAt = start, now = start, labels = labels)

        assertEquals("나 · 버스 이동 중", moving.myTitle)
        assertEquals("버스 이동 중", moving.myMapLabel)
        assertEquals("전체 35분", moving.routeTotalLabel)
        assertTrue(moving.mySubtitle.endsWith("35분 남음"))
        assertEquals("전체 35분 중 0분이 지났어요. 35분 후 도착 예정이에요.", moving.footnote)
        assertEquals("35분 남음", moving.timeSummary.endNote)
        assertFalse(moving.canArrive)
        assertFalse(moving.hasArrived)
    }

    @Test
    fun `이동 중 구간 상세는 출발한 시각 기준으로 하차 예정까지 보여 준다`() {
        val option = options(bus).toRouteOptions("집", labels).single()
        val departed = RouteTime.parseServer("2026-11-20T16:20:00")!!

        val steps = option.toMoving(departed, departed, labels).steps

        assertEquals("${clock("2026-11-20T16:20:00")}~${short("2026-11-20T16:25:00")}", steps[0].timeLabel)
        assertEquals(
            "${clock("2026-11-20T16:25:00")} 승차 · ${short("2026-11-20T16:49:00")} 하차 예정",
            steps[1].timeLabel
        )
        assertEquals("${clock("2026-11-20T16:49:00")}~${short("2026-11-20T16:55:00")}", steps[2].timeLabel)
    }

    @Test
    fun `도착까지 1분 이하로 남으면 도착 버튼이 켜진다`() {
        val option = options(bus).toRouteOptions("집", labels).single()
        val start = 1_000_000L

        val almost = option.toMoving(start, start + RouteTime.minutesToMillis(34), labels)
        assertTrue(almost.canArrive)
        assertEquals("전체 35분 중 34분이 지났어요. 1분 후 도착 예정이에요.", almost.footnote)

        val past = option.toMoving(start, start + RouteTime.minutesToMillis(50), labels)
        assertTrue(past.canArrive)
        assertEquals("0분 남음", past.timeSummary.endNote)
        assertEquals("전체 35분 중 35분이 지났어요. 0분 후 도착 예정이에요.", past.footnote)
    }

    @Test
    fun `한 번 켜진 도착 버튼은 시간이 다시 계산돼도 꺼지지 않는다`() {
        val option = options(bus).toRouteOptions("집", labels).single()
        val start = 1_000_000L

        val moving = option.toMoving(start, start, labels, canArriveAlready = true)

        assertTrue(moving.canArrive)
    }

    @Test
    fun `도착을 마치면 약속보다 얼마나 일찍 도착했는지 보여 준다`() {
        val option = options(bus).toRouteOptions("집", labels).single()
        val start = RouteTime.parseServer("2026-11-20T16:15:00")!!
        val appointment = RouteTime.parseServer("2026-11-20T17:00:00")!!
        val now = start + RouteTime.minutesToMillis(35)
        val moving = option.toMoving(start, now, labels)

        val arrived = moving.toArrived(now, appointment, labels)

        assertEquals("나 · 도착 완료", arrived.myTitle)
        assertEquals("${clock("2026-11-20T16:50:00")} · 약속 10분 전 도착", arrived.mySubtitle)
        assertTrue(arrived.hasArrived)
        assertFalse(arrived.canArrive)
        assertEquals("도착 시각", arrived.timeSummary.startCaption)
        assertEquals(clock("2026-11-20T16:50:00"), arrived.timeSummary.startTime)
        assertEquals("약속 장소 도착", arrived.timeSummary.startNote)
        assertEquals("약속 시각", arrived.timeSummary.endCaption)
        assertEquals(clock("2026-11-20T17:00:00"), arrived.timeSummary.endTime)
        assertEquals("10분 일찍 도착", arrived.timeSummary.endNote)
        assertEquals("약속 10분 전 도착", arrivedEstimateNote(now, appointment, labels))
    }

    @Test
    fun `약속보다 늦게 도착하거나 정시에 도착한 경우도 문구를 나눈다`() {
        val option = options(bus).toRouteOptions("집", labels).single()
        val appointment = RouteTime.parseServer("2026-11-20T17:00:00")!!
        val moving = option.toMoving(0L, 0L, labels)

        val late = moving.toArrived(appointment + RouteTime.minutesToMillis(7), appointment, labels)
        assertTrue(late.mySubtitle.endsWith("약속 7분 후 도착"))
        assertEquals("7분 늦게 도착", late.timeSummary.endNote)
        assertEquals("약속 7분 후 도착", arrivedEstimateNote(appointment + RouteTime.minutesToMillis(7), appointment, labels))

        val onTime = moving.toArrived(appointment, appointment, labels)
        assertTrue(onTime.mySubtitle.endsWith("정시 도착"))
        assertEquals("정시 도착", onTime.timeSummary.endNote)
        assertEquals("약속 시각에 도착", arrivedEstimateNote(appointment, appointment, labels))
    }

    @Test
    fun `약속 시각을 모르면 도착 완료 문구만 보여 준다`() {
        val option = options(bus).toRouteOptions("집", labels).single()
        val moving = option.toMoving(0L, 0L, labels)

        val arrived = moving.toArrived(0L, null, labels)

        assertTrue(arrived.mySubtitle.endsWith("도착 완료"))
        assertEquals("", arrived.timeSummary.endNote)
        assertEquals("", arrivedEstimateNote(0L, null, labels))
    }

    @Test
    fun `출발 위치를 고르지 않으면 서버에서 받은 이동수단으로는 출발할 수 없다`() {
        val live = options(bus).toRouteOptions("집", labels)
        val selecting = RouteSampleData.overview.copy(options = live, moving = null).openRouteSelection()
            .selectTransport(RouteTransport.BUS)

        assertFalse(selecting.canStart())
        val start = RouteStartLocation("basic-1", RouteStartKind.BASIC, "집", null, 37.5, 127.0)
        assertTrue(selecting.copy(startLocation = start).canStart())
        assertFalse(selecting.copy(startLocation = start, actionStatus = RouteRequestStatus.LOADING).canStart())
    }
}
