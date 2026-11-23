package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelOption
import com.jigmnnaga.domain.model.TravelOptions
import com.jigmnnaga.domain.model.TravelRejectReason
import com.jigmnnaga.domain.model.TravelResult
import com.jigmnnaga.domain.model.TravelStep
import com.jigmnnaga.domain.model.TravelStepType
import com.jigmnnaga.domain.repository.TravelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteTravelViewModelTest {
    private val sampleOptions = TravelOptions(
        destinationName = "강남역",
        appointmentDateTime = "2026-11-20T17:00:00",
        options = listOf(
            TravelOption(
                mode = TravelMode.BUS,
                durationMinutes = 35,
                distanceMeters = 8000,
                estimatedArrivalTime = "2026-11-20T16:50:00",
                steps = listOf(
                    TravelStep(TravelStepType.WALK, 5),
                    TravelStep(TravelStepType.BUS, 24, "146"),
                    TravelStep(TravelStepType.WALK, 6)
                )
            ),
            TravelOption(
                mode = TravelMode.TAXI,
                durationMinutes = 22,
                distanceMeters = 9000,
                estimatedArrivalTime = "2026-11-20T16:37:00",
                steps = listOf(TravelStep(TravelStepType.TAXI, 22))
            )
        )
    )

    private class FakeTravel(var options: TravelResult<TravelOptions>) : TravelRepository {
        val calls = mutableListOf<String>()
        var register: TravelResult<Unit> = TravelResult.Success(Unit)
        var select: TravelResult<Unit> = TravelResult.Success(Unit)
        var depart: TravelResult<Unit> = TravelResult.Success(Unit)
        var arrive: TravelResult<Unit> = TravelResult.Success(Unit)

        override suspend fun registerDepartureLocation(
            appointmentId: Long,
            name: String,
            latitude: Double,
            longitude: Double
        ): TravelResult<Unit> {
            calls += "register:$appointmentId:$name:$latitude,$longitude"
            return register
        }

        override suspend fun getTravelOptions(
            appointmentId: Long,
            latitude: Double,
            longitude: Double
        ): TravelResult<TravelOptions> {
            calls += "options:$appointmentId:$latitude,$longitude"
            return options
        }

        override suspend fun selectTravelMode(
            appointmentId: Long,
            mode: TravelMode,
            durationMinutes: Int
        ): TravelResult<Unit> {
            calls += "mode:$appointmentId:$mode:$durationMinutes"
            return select
        }

        override suspend fun depart(appointmentId: Long): TravelResult<Unit> {
            calls += "depart:$appointmentId"
            return depart
        }

        override suspend fun arrive(appointmentId: Long): TravelResult<Unit> {
            calls += "arrive:$appointmentId"
            return arrive
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val travel = FakeTravel(TravelResult.Success(sampleOptions))
    private var now = RouteTime.parseServer("2026-11-20T16:15:00")!!
    private val home = RouteStartLocation("basic-1", RouteStartKind.BASIC, "집", null, 37.5, 127.0)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 실제 앱처럼 경로 선택지와 이동 정보가 비어 있는 경로 선택 단계. */
    private fun vm() = RouteViewModel(
        initialState = RouteSampleData.overview.copy(options = emptyList(), moving = null).openRouteSelection(),
        appointmentId = 7L,
        travelRepository = travel,
        nowMillis = { now },
        movingTickMillis = 30_000L
    )

    private fun RouteViewModel.pickStart(location: RouteStartLocation = home) {
        onOpenStartPicker()
        onSelectStartCandidate(location)
        onConfirmStartLocation()
        dispatcher.scheduler.runCurrent()
    }

    private fun RouteViewModel.depart(transport: RouteTransport = RouteTransport.BUS) {
        pickStart()
        onSelectTransport(transport)
        onStartMoving()
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `출발 위치를 확정하면 서버에 등록하고 그 위치로 이동수단 후보를 불러온다`() {
        val vm = vm()
        vm.pickStart()

        assertEquals(listOf("register:7:집:37.5,127.0", "options:7:37.5,127.0"), travel.calls)
        val state = vm.uiState.value
        assertEquals(listOf(RouteTransport.BUS, RouteTransport.TAXI), state.options.map { it.transport })
        assertEquals(RouteRequestStatus.IDLE, state.optionsStatus)
        assertNull(state.selectedTransport)
        assertFalse(state.canStart())
    }

    @Test
    fun `출발 위치 등록에 실패하면 후보를 부르지 않고 실패로 표시한다`() {
        travel.register = TravelResult.Failure(retryable = true)
        val vm = vm()
        vm.pickStart()

        assertEquals(listOf("register:7:집:37.5,127.0"), travel.calls)
        assertEquals(RouteRequestStatus.FAILED, vm.uiState.value.optionsStatus)
        assertTrue(vm.uiState.value.options.isEmpty())
    }

    @Test
    fun `후보 조회에 실패하거나 후보가 없으면 실패로 표시한다`() {
        travel.options = TravelResult.Failure(retryable = false)
        val failed = vm()
        failed.pickStart()
        assertEquals(RouteRequestStatus.FAILED, failed.uiState.value.optionsStatus)

        travel.options = TravelResult.Success(TravelOptions(options = emptyList()))
        val empty = vm()
        empty.pickStart()
        assertEquals(RouteRequestStatus.FAILED, empty.uiState.value.optionsStatus)
    }

    @Test
    fun `후보를 부르는 동안에는 불러오는 중으로 표시한다`() {
        val vm = vm()
        vm.onOpenStartPicker()
        vm.onSelectStartCandidate(home)
        vm.onConfirmStartLocation()

        assertEquals(RouteRequestStatus.LOADING, vm.uiState.value.optionsStatus)
        dispatcher.scheduler.runCurrent()
        assertEquals(RouteRequestStatus.IDLE, vm.uiState.value.optionsStatus)
    }

    @Test
    fun `실패한 뒤 다시 시도하면 같은 위치로 다시 불러온다`() {
        travel.options = TravelResult.Failure(retryable = true)
        val vm = vm()
        vm.pickStart()
        travel.calls.clear()
        travel.options = TravelResult.Success(sampleOptions)

        vm.onRetryTravelOptions()
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("register:7:집:37.5,127.0", "options:7:37.5,127.0"), travel.calls)
        assertEquals(2, vm.uiState.value.options.size)
        assertEquals(RouteRequestStatus.IDLE, vm.uiState.value.optionsStatus)
    }

    @Test
    fun `이미 불러왔거나 불러오는 중이면 다시 시도해도 아무 일도 하지 않는다`() {
        val vm = vm()
        vm.pickStart()
        travel.calls.clear()

        vm.onRetryTravelOptions()
        dispatcher.scheduler.runCurrent()

        assertTrue(travel.calls.isEmpty())
    }

    @Test
    fun `위치를 고르지 않고 닫으면 서버를 부르지 않는다`() {
        val vm = vm()
        vm.onOpenStartPicker()
        vm.onConfirmStartLocation()
        vm.onCloseStartPicker()
        dispatcher.scheduler.runCurrent()

        assertTrue(travel.calls.isEmpty())
    }

    @Test
    fun `이동수단을 고르면 도착지 카드에 도착 예정과 약속 여유가 보인다`() {
        val vm = vm()
        vm.pickStart()
        vm.onSelectTransport(RouteTransport.BUS)

        val state = vm.uiState.value
        assertTrue(state.destinationEstimateTitle.startsWith("도착 예정"))
        assertTrue(state.destinationEstimateTitle.endsWith("4:50"))
        assertTrue(state.destinationEstimateNote.endsWith("10분 여유"))
        assertTrue(state.canStart())
    }

    @Test
    fun `지금 출발하기는 이동수단을 저장하고 출발을 알린 뒤 약속 이동으로 넘어간다`() {
        val vm = vm()
        vm.depart()

        assertEquals(
            listOf("register:7:집:37.5,127.0", "options:7:37.5,127.0", "mode:7:BUS:35", "depart:7"),
            travel.calls
        )
        val state = vm.uiState.value
        assertEquals(RouteStage.MOVING, state.stage)
        assertEquals(RouteRequestStatus.IDLE, state.actionStatus)
        assertEquals("나 · 버스 이동 중", state.moving?.myTitle)
        assertFalse(state.canArrive())
        val me = state.members.first { it.isMe }
        assertEquals(RouteMemberStatus.MOVING, me.status)
        assertEquals(RouteTransport.BUS, me.transport)
    }

    @Test
    fun `이미 출발한 상태라는 응답이면 출발한 것으로 보고 넘어간다`() {
        travel.depart = TravelResult.Rejected(TravelRejectReason.ALREADY_DEPARTED)
        val vm = vm()
        vm.depart()

        assertEquals(RouteStage.MOVING, vm.uiState.value.stage)
    }

    @Test
    fun `이동수단 저장에 실패하면 출발을 알리지 않고 경로 선택에 머문다`() {
        travel.select = TravelResult.Rejected(TravelRejectReason.TIME_PASSED)
        val vm = vm()
        vm.depart()

        assertFalse(travel.calls.any { it.startsWith("depart") })
        val state = vm.uiState.value
        assertEquals(RouteStage.SELECT, state.stage)
        assertEquals(RouteRequestStatus.FAILED, state.actionStatus)
        assertTrue(state.canStart())
    }

    @Test
    fun `출발 요청이 실패하면 경로 선택에 머물고 다시 누를 수 있다`() {
        travel.depart = TravelResult.Failure(retryable = true)
        val vm = vm()
        vm.depart()
        assertEquals(RouteStage.SELECT, vm.uiState.value.stage)
        assertEquals(RouteRequestStatus.FAILED, vm.uiState.value.actionStatus)

        travel.depart = TravelResult.Success(Unit)
        vm.onStartMoving()
        dispatcher.scheduler.runCurrent()

        assertEquals(RouteStage.MOVING, vm.uiState.value.stage)
    }

    @Test
    fun `출발 요청 중에는 한 번 더 눌러도 다시 보내지 않는다`() {
        val vm = vm()
        vm.pickStart()
        vm.onSelectTransport(RouteTransport.BUS)
        travel.calls.clear()

        vm.onStartMoving()
        vm.onStartMoving()
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("mode:7:BUS:35", "depart:7"), travel.calls)
    }

    @Test
    fun `이동수단을 고르지 않으면 출발하지 못한다`() {
        val vm = vm()
        vm.pickStart()
        travel.calls.clear()

        vm.onStartMoving()
        dispatcher.scheduler.runCurrent()

        assertTrue(travel.calls.isEmpty())
        assertEquals(RouteStage.SELECT, vm.uiState.value.stage)
    }

    @Test
    fun `이동 중에는 남은 시간을 다시 계산하고 도착 직전에 도착 버튼이 켜진다`() {
        val vm = vm()
        vm.depart()
        assertFalse(vm.uiState.value.canArrive())
        assertTrue(vm.uiState.value.moving!!.mySubtitle.endsWith("35분 남음"))

        now += RouteTime.minutesToMillis(10)
        dispatcher.scheduler.advanceTimeBy(30_000L)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.moving!!.mySubtitle.endsWith("25분 남음"))
        assertTrue(vm.uiState.value.members.first { it.isMe }.description.endsWith("25분 남음"))
        assertFalse(vm.uiState.value.canArrive())

        now += RouteTime.minutesToMillis(24)
        dispatcher.scheduler.advanceTimeBy(30_000L)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.canArrive())
    }

    @Test
    fun `도착을 누르면 서버에 알리고 도착 완료로 바뀐다`() {
        val vm = vm()
        vm.depart()
        now += RouteTime.minutesToMillis(35)
        dispatcher.scheduler.advanceTimeBy(30_000L)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.canArrive())

        vm.onArrive()
        dispatcher.scheduler.runCurrent()

        assertEquals("arrive:7", travel.calls.last())
        val state = vm.uiState.value
        assertTrue(state.moving!!.hasArrived)
        assertEquals("나 · 도착 완료", state.moving?.myTitle)
        assertEquals(RouteRequestStatus.IDLE, state.actionStatus)
        assertTrue(state.destinationEstimateTitle.startsWith("도착 완료"))
        assertTrue(state.destinationEstimateTitle.endsWith("4:50"))
        assertEquals("약속 10분 전 도착", state.destinationEstimateNote)
        assertTrue(state.moving!!.mySubtitle.endsWith("약속 10분 전 도착"))
        assertEquals(RouteMemberStatus.ARRIVED, state.members.first { it.isMe }.status)
        assertFalse(state.canArrive())
    }

    @Test
    fun `도착한 뒤에는 남은 시간을 더 이상 다시 계산하지 않는다`() {
        val vm = vm()
        vm.depart()
        now += RouteTime.minutesToMillis(35)
        dispatcher.scheduler.advanceTimeBy(30_000L)
        dispatcher.scheduler.runCurrent()
        vm.onArrive()
        dispatcher.scheduler.runCurrent()
        val arrived = vm.uiState.value

        now += RouteTime.minutesToMillis(5)
        dispatcher.scheduler.advanceTimeBy(120_000L)
        dispatcher.scheduler.runCurrent()

        assertEquals(arrived, vm.uiState.value)
    }

    @Test
    fun `이미 도착한 상태라는 응답이면 도착한 것으로 본다`() {
        travel.arrive = TravelResult.Rejected(TravelRejectReason.ALREADY_ARRIVED)
        val vm = vm()
        vm.depart()
        now += RouteTime.minutesToMillis(35)
        dispatcher.scheduler.advanceTimeBy(30_000L)
        dispatcher.scheduler.runCurrent()

        vm.onArrive()
        dispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.moving!!.hasArrived)
    }

    @Test
    fun `도착 요청이 실패하면 도착 완료로 바꾸지 않고 다시 누를 수 있다`() {
        travel.arrive = TravelResult.Failure(retryable = true)
        val vm = vm()
        vm.depart()
        now += RouteTime.minutesToMillis(35)
        dispatcher.scheduler.advanceTimeBy(30_000L)
        dispatcher.scheduler.runCurrent()

        vm.onArrive()
        dispatcher.scheduler.runCurrent()

        val failed = vm.uiState.value
        assertFalse(failed.moving!!.hasArrived)
        assertEquals(RouteRequestStatus.FAILED, failed.actionStatus)
        assertTrue(failed.canArrive())

        travel.arrive = TravelResult.Success(Unit)
        vm.onArrive()
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.moving!!.hasArrived)
    }

    @Test
    fun `도착 직전이 아니면 도착을 눌러도 서버를 부르지 않는다`() {
        val vm = vm()
        vm.depart()
        travel.calls.clear()

        vm.onArrive()
        dispatcher.scheduler.runCurrent()

        assertTrue(travel.calls.isEmpty())
        assertNotNull(vm.uiState.value.moving)
        assertFalse(vm.uiState.value.moving!!.hasArrived)
    }

    @Test
    fun `출발 위치를 바꾸면 이전에 고른 이동수단은 지우고 새 위치로 다시 불러온다`() {
        val vm = vm()
        vm.pickStart()
        vm.onSelectTransport(RouteTransport.TAXI)
        travel.calls.clear()

        vm.onOpenStartPicker()
        vm.onSelectStartCandidate(
            RouteStartLocation("search-9", RouteStartKind.SEARCH, "학교", null, 37.6, 127.1)
        )
        vm.onConfirmStartLocation()

        assertNull(vm.uiState.value.selectedTransport)
        assertTrue(vm.uiState.value.options.isEmpty())
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("register:7:학교:37.6,127.1", "options:7:37.6,127.1"), travel.calls)
        assertEquals("학교", vm.uiState.value.startLocation?.name)
    }

    /** 약속 날짜가 [meetingDate]인 경로 선택 단계. */
    private fun vmOn(meetingDate: String?) = RouteViewModel(
        initialState = RouteSampleData.overview
            .copy(options = emptyList(), moving = null, meetingDate = meetingDate)
            .openRouteSelection(),
        appointmentId = 7L,
        travelRepository = travel,
        nowMillis = { now },
        movingTickMillis = 30_000L
    )

    private fun RouteViewModel.chooseRoute() {
        pickStart()
        onSelectTransport(RouteTransport.BUS)
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `약속 당일이 아니면 경로를 골라도 출발할 수 없고 서버에 출발을 알리지 않는다`() {
        val vm = vmOn(RouteTime.dayOf(now + 6 * 24 * 60 * 60_000L))

        vm.chooseRoute()
        assertFalse(vm.uiState.value.departureOpen)
        assertFalse(vm.uiState.value.canStart())

        vm.onStartMoving()
        dispatcher.scheduler.runCurrent()

        assertEquals(RouteStage.SELECT, vm.uiState.value.stage)
        assertTrue(travel.calls.none { it.startsWith("depart") })
    }

    @Test
    fun `약속 당일이면 경로를 고른 뒤 출발할 수 있다`() {
        val vm = vmOn(RouteTime.dayOf(now))

        vm.chooseRoute()
        assertTrue(vm.uiState.value.departureOpen)
        assertTrue(vm.uiState.value.canStart())

        vm.onStartMoving()
        dispatcher.scheduler.runCurrent()

        assertEquals(RouteStage.MOVING, vm.uiState.value.stage)
        assertTrue(travel.calls.contains("depart:7"))
    }

    @Test
    fun `약속 날짜를 모르면 출발을 막지 않는다`() {
        val vm = vmOn(null)

        vm.chooseRoute()

        assertTrue(vm.uiState.value.departureOpen)
        assertTrue(vm.uiState.value.canStart())
    }

    @Test
    fun `자정이 지나 약속 당일이 되면 경로 선택 화면에 들어올 때 다시 확인해 출발할 수 있다`() {
        now = RouteTime.parseServer("2026-11-19T23:50:00")!!
        val vm = vmOn("2026-11-20")
        assertFalse(vm.uiState.value.departureOpen)

        now = RouteTime.parseServer("2026-11-20T00:10:00")!!
        vm.onMyCardClick()

        assertTrue(vm.uiState.value.departureOpen)
    }

    @Test
    fun `약속이 이미 지난 날이면 출발할 수 없다`() {
        val vm = vmOn("2026-11-19")

        assertFalse(vm.uiState.value.departureOpen)
    }
}
