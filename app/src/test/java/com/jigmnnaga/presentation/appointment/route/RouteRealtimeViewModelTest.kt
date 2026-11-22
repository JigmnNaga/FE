package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.LocationShareEnd
import com.jigmnnaga.domain.model.LocationShareEndReason
import com.jigmnnaga.domain.model.LocationShareError
import com.jigmnnaga.domain.model.MemberArrival
import com.jigmnnaga.domain.model.MemberDepartureStatus
import com.jigmnnaga.domain.model.MemberEtaUpdate
import com.jigmnnaga.domain.model.MemberLocationUpdate
import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelOptions
import com.jigmnnaga.domain.model.TravelResult
import com.jigmnnaga.domain.repository.LocationRealtime
import com.jigmnnaga.domain.repository.TravelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRealtimeViewModelTest {

    private class FakeRealtime : LocationRealtime {
        private val connected = MutableStateFlow(false)
        override val isConnected: Flow<Boolean> = connected
        var connects = 0
        var disconnects = 0
        val locations = MutableSharedFlow<MemberLocationUpdate>(extraBufferCapacity = 16)
        val etas = MutableSharedFlow<MemberEtaUpdate>(extraBufferCapacity = 16)
        val arrivals = MutableSharedFlow<MemberArrival>(extraBufferCapacity = 16)
        val ends = MutableSharedFlow<LocationShareEnd>(extraBufferCapacity = 16)
        val errors = MutableSharedFlow<LocationShareError>(extraBufferCapacity = 16)
        val sent = mutableListOf<Triple<Long, Double, Double>>()

        override fun connect() { connects++ }
        override fun disconnect() { disconnects++ }
        override fun locations(appointmentId: Long) = locations
        override fun etas(appointmentId: Long) = etas
        override fun arrivals(appointmentId: Long) = arrivals
        override fun sharingEnds(appointmentId: Long) = ends
        override fun errors() = errors
        override fun sendLocation(appointmentId: Long, latitude: Double, longitude: Double): Boolean {
            sent += Triple(appointmentId, latitude, longitude)
            return true
        }
    }

    private class FakeTravel : TravelRepository {
        override suspend fun registerDepartureLocation(
            appointmentId: Long, name: String, latitude: Double, longitude: Double
        ): TravelResult<Unit> = TravelResult.Success(Unit)

        override suspend fun getTravelOptions(
            appointmentId: Long, latitude: Double, longitude: Double
        ): TravelResult<TravelOptions> = TravelResult.Success(TravelOptions())

        override suspend fun selectTravelMode(
            appointmentId: Long, mode: TravelMode, durationMinutes: Int
        ): TravelResult<Unit> = TravelResult.Success(Unit)

        override suspend fun depart(appointmentId: Long): TravelResult<Unit> = TravelResult.Success(Unit)

        override suspend fun arrive(appointmentId: Long): TravelResult<Unit> = TravelResult.Success(Unit)
    }

    private val dispatcher = StandardTestDispatcher()
    private val realtime = FakeRealtime()
    private val myPositions = MutableSharedFlow<RouteCoordinates>(extraBufferCapacity = 16)
    private var now = RouteTime.parseServer("2026-11-20T16:15:00")!!

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(state: RouteUiState = RouteSampleData.selecting.selectTransport(RouteTransport.BUS)) =
        RouteViewModel(
            initialState = state,
            appointmentId = 7L,
            travelRepository = FakeTravel(),
            nowMillis = { now },
            locationRealtime = realtime,
            locationUpdates = { myPositions },
            locationSendIntervalMillis = 5_000L
        ).also { dispatcher.scheduler.runCurrent() }

    private fun RouteViewModel.depart() {
        onStartMoving()
        dispatcher.scheduler.runCurrent()
    }

    private fun RouteViewModel.clear() {
        RouteViewModel::class.java.getDeclaredMethod("onCleared").apply { isAccessible = true }.invoke(this)
    }

    private fun RouteViewModel.friend(id: String) = uiState.value.members.first { it.id == id }

    private fun location(memberId: Long, status: MemberDepartureStatus) =
        MemberLocationUpdate(memberId, 37.5, 127.0, status, "2026-11-20T16:15:00")

    @Test
    fun `화면이 만들어지면 연결하고 정리하면 연결을 끊는다`() {
        val vm = vm()
        assertEquals(1, realtime.connects)
        assertEquals(0, realtime.disconnects)

        vm.clear()

        assertEquals(1, realtime.disconnects)
    }

    @Test
    fun `친구가 출발했다는 위치를 받으면 이동수단과 함께 이동 중으로 바꾼다`() {
        val state = RouteSampleData.overview.let { overview ->
            overview.copy(
                members = overview.members.map {
                    if (it.id == "2") it.copy(plannedTransport = RouteTransport.SUBWAY) else it
                }
            )
        }
        val vm = vm(state)

        realtime.locations.tryEmit(location(2L, MemberDepartureStatus.DEPARTED))
        dispatcher.scheduler.runCurrent()

        val friend = vm.friend("2")
        assertEquals(RouteMemberStatus.MOVING, friend.status)
        assertNull(friend.statusLabel)
        assertEquals("지하철로 이동 중", friend.description)
        assertEquals(RouteTransport.SUBWAY, friend.transport)
        assertEquals(37.5, friend.latitude!!, 0.0)
        assertEquals(127.0, friend.longitude!!, 0.0)
        // 다른 친구는 그대로다.
        assertEquals(RouteMemberStatus.NOT_STARTED, vm.friend("3").status)
    }

    @Test
    fun `이동수단을 모르는 친구는 이동 중이라고만 보여 준다`() {
        val vm = vm(RouteSampleData.overview)

        realtime.locations.tryEmit(location(3L, MemberDepartureStatus.DEPARTED))
        dispatcher.scheduler.runCurrent()

        assertEquals("이동 중", vm.friend("3").description)
        assertNull(vm.friend("3").transport)
    }

    @Test
    fun `출발 전 상태와 모르는 상태의 위치는 상태를 바꾸지 않고 위치만 담는다`() {
        val vm = vm(RouteSampleData.overview)

        realtime.locations.tryEmit(location(2L, MemberDepartureStatus.BEFORE_DEPARTURE))
        realtime.locations.tryEmit(location(3L, MemberDepartureStatus.UNKNOWN))
        dispatcher.scheduler.runCurrent()

        assertEquals(RouteMemberStatus.NOT_STARTED, vm.friend("2").status)
        assertEquals(37.5, vm.friend("2").latitude!!, 0.0)
        assertEquals(RouteMemberStatus.NOT_STARTED, vm.friend("3").status)
    }

    @Test
    fun `친구의 도착 소식을 받으면 도착 완료로 바꾸고 이후 위치가 와도 되돌리지 않는다`() {
        val vm = vm(RouteSampleData.overview)

        realtime.arrivals.tryEmit(MemberArrival(2L, "2026-11-20T16:50:00"))
        realtime.locations.tryEmit(location(2L, MemberDepartureStatus.DEPARTED))
        dispatcher.scheduler.runCurrent()

        val friend = vm.friend("2")
        assertEquals(RouteMemberStatus.ARRIVED, friend.status)
        assertEquals("도착 완료", friend.description)
        assertNull(friend.statusLabel)
    }

    @Test
    fun `내 위치와 친구의 도착 예정 소식은 친구 카드를 바꾸지 않는다`() {
        val vm = vm(RouteSampleData.overview)
        val before = vm.uiState.value.members

        realtime.locations.tryEmit(location(1L, MemberDepartureStatus.DEPARTED))
        realtime.etas.tryEmit(MemberEtaUpdate(2L, "2026-11-20T16:50:00", 30))
        dispatcher.scheduler.runCurrent()

        assertEquals(before, vm.uiState.value.members)
    }

    @Test
    fun `출발하면 내 위치를 최소 간격으로 서버에 보낸다`() {
        val vm = vm()
        vm.depart()
        assertEquals(RouteStage.MOVING, vm.uiState.value.stage)

        myPositions.tryEmit(RouteCoordinates(37.1, 127.1))
        dispatcher.scheduler.runCurrent()
        now += 1_000L
        myPositions.tryEmit(RouteCoordinates(37.2, 127.2))
        dispatcher.scheduler.runCurrent()
        now += 4_000L
        myPositions.tryEmit(RouteCoordinates(37.3, 127.3))
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(Triple(7L, 37.1, 127.1), Triple(7L, 37.3, 127.3)), realtime.sent)
    }

    @Test
    fun `출발하기 전에는 내 위치를 보내지 않는다`() {
        vm()

        myPositions.tryEmit(RouteCoordinates(37.1, 127.1))
        dispatcher.scheduler.runCurrent()

        assertTrue(realtime.sent.isEmpty())
    }

    @Test
    fun `서버가 내 위치 공유를 거절하면 더 보내지 않고 너무 자주 보냈다는 오류는 무시한다`() {
        val vm = vm()
        vm.depart()

        realtime.errors.tryEmit(LocationShareError.TOO_FREQUENT)
        realtime.errors.tryEmit(LocationShareError.NOT_DEPARTED)
        dispatcher.scheduler.runCurrent()
        myPositions.tryEmit(RouteCoordinates(37.1, 127.1))
        dispatcher.scheduler.runCurrent()
        assertEquals(1, realtime.sent.size)

        realtime.errors.tryEmit(LocationShareError.SHARING_STOPPED)
        dispatcher.scheduler.runCurrent()
        now += 10_000L
        myPositions.tryEmit(RouteCoordinates(37.2, 127.2))
        dispatcher.scheduler.runCurrent()
        assertEquals(1, realtime.sent.size)
    }

    @Test
    fun `내 위치 공유 종료 소식을 받으면 더 보내지 않지만 친구의 종료는 상관없다`() {
        val vm = vm()
        vm.depart()

        realtime.ends.tryEmit(LocationShareEnd(2L, LocationShareEndReason.USER_REQUEST))
        dispatcher.scheduler.runCurrent()
        myPositions.tryEmit(RouteCoordinates(37.1, 127.1))
        dispatcher.scheduler.runCurrent()
        assertEquals(1, realtime.sent.size)

        realtime.ends.tryEmit(LocationShareEnd(1L, LocationShareEndReason.USER_REQUEST))
        dispatcher.scheduler.runCurrent()
        now += 10_000L
        myPositions.tryEmit(RouteCoordinates(37.2, 127.2))
        dispatcher.scheduler.runCurrent()
        assertEquals(1, realtime.sent.size)
    }

    @Test
    fun `서버가 내 도착을 알려 오면 도착 완료로 바꾸고 위치 전송을 멈춘다`() {
        val vm = vm()
        vm.depart()
        assertEquals(false, vm.uiState.value.moving?.hasArrived)

        now = RouteTime.parseServer("2026-11-20T16:50:00")!!
        realtime.arrivals.tryEmit(MemberArrival(1L, "2026-11-20T16:50:00"))
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(true, state.moving?.hasArrived)
        assertEquals(RouteMemberStatus.ARRIVED, state.members.first { it.isMe }.status)
        assertEquals("도착 완료 오후 4:50", state.destinationEstimateTitle)

        myPositions.tryEmit(RouteCoordinates(37.1, 127.1))
        dispatcher.scheduler.runCurrent()
        assertTrue(realtime.sent.isEmpty())
    }

    @Test
    fun `내 도착 예정 시각이 다시 계산되어 오면 남은 시간과 도착지 카드를 그 시각으로 맞춘다`() {
        val vm = vm()
        vm.depart()

        realtime.etas.tryEmit(MemberEtaUpdate(1L, "2026-11-20T16:40:00", 25, "2026-11-20T16:15:00"))
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals("25분 남음", state.moving?.timeSummary?.endNote)
        assertEquals("오후 4:40", state.moving?.timeSummary?.endTime)
        assertEquals("도착 예정 오후 4:40", state.destinationEstimateTitle)
        assertEquals("오후 4:40 도착 · 25분 남음", state.members.first { it.isMe }.description)
    }

    @Test
    fun `이동 중이 아닐 때 받은 내 도착 예정 소식은 무시한다`() {
        val vm = vm(RouteSampleData.overview)
        val before = vm.uiState.value

        realtime.etas.tryEmit(MemberEtaUpdate(1L, "2026-11-20T16:40:00", 25))
        dispatcher.scheduler.runCurrent()

        assertEquals(before, vm.uiState.value)
    }

    /** 처음에는 권한이 없어 바로 끝나는 위치 흐름을, 이후에는 [myPositions]를 돌려 주는 뷰모델. */
    private fun vmWithoutPermissionAtFirst(): Pair<RouteViewModel, () -> Int> {
        var calls = 0
        val vm = RouteViewModel(
            initialState = RouteSampleData.selecting.selectTransport(RouteTransport.BUS),
            appointmentId = 7L,
            travelRepository = FakeTravel(),
            nowMillis = { now },
            locationRealtime = realtime,
            locationUpdates = { calls++; if (calls == 1) emptyFlow() else myPositions },
            locationSendIntervalMillis = 5_000L
        ).also { dispatcher.scheduler.runCurrent() }
        return vm to { calls }
    }

    @Test
    fun `권한이 없다고 확인되면 이동 중에 권한 없음 화면을 보여 준다`() {
        val (vm, _) = vmWithoutPermissionAtFirst()
        vm.depart()

        assertFalse(vm.uiState.value.needsLocationPermission)
        vm.onLocationPermissionChecked(false)

        assertTrue(vm.uiState.value.needsLocationPermission)
    }

    @Test
    fun `권한이 없어 시작되지 못한 위치 전송은 권한을 허용하고 돌아오면 다시 시작한다`() {
        val (vm, calls) = vmWithoutPermissionAtFirst()
        vm.depart()
        vm.onLocationPermissionChecked(false)
        assertEquals(1, calls())

        vm.onLocationPermissionChecked(true)
        dispatcher.scheduler.runCurrent()
        assertFalse(vm.uiState.value.needsLocationPermission)
        assertEquals(2, calls())

        myPositions.tryEmit(RouteCoordinates(37.1, 127.1))
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(Triple(7L, 37.1, 127.1)), realtime.sent)
    }

    @Test
    fun `위치 전송이 이어지는 중이면 권한을 다시 확인해도 전송을 새로 시작하지 않는다`() {
        var calls = 0
        val vm = RouteViewModel(
            initialState = RouteSampleData.selecting.selectTransport(RouteTransport.BUS),
            appointmentId = 7L,
            travelRepository = FakeTravel(),
            nowMillis = { now },
            locationRealtime = realtime,
            locationUpdates = { calls++; myPositions },
            locationSendIntervalMillis = 5_000L
        ).also { dispatcher.scheduler.runCurrent() }
        vm.depart()

        vm.onLocationPermissionChecked(true)
        vm.onLocationPermissionChecked(true)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, calls)
    }

    @Test
    fun `도착한 뒤에는 권한이 없어도 권한 없음 화면을 보여 주지 않고 전송도 다시 시작하지 않는다`() {
        val (vm, calls) = vmWithoutPermissionAtFirst()
        vm.depart()
        now = RouteTime.parseServer("2026-11-20T16:50:00")!!
        realtime.arrivals.tryEmit(MemberArrival(1L, "2026-11-20T16:50:00"))
        dispatcher.scheduler.runCurrent()
        assertEquals(true, vm.uiState.value.moving?.hasArrived)

        vm.onLocationPermissionChecked(false)
        assertFalse(vm.uiState.value.needsLocationPermission)

        vm.onLocationPermissionChecked(true)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, calls())
    }

    @Test
    fun `출발하기 전에는 권한이 없어도 권한 없음 화면을 보여 주지 않는다`() {
        val vm = vm()

        vm.onLocationPermissionChecked(false)

        assertFalse(vm.uiState.value.needsLocationPermission)
        assertFalse(vm.uiState.value.locationPermissionGranted)
    }
}
