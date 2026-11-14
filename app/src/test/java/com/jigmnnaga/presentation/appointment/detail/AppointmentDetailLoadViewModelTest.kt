package com.jigmnnaga.presentation.appointment.detail

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailResult
import com.jigmnnaga.domain.repository.AppointmentDetailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentDetailLoadViewModelTest {

    private class FakeRepository : AppointmentDetailRepository {
        val results = ArrayDeque<AppointmentDetailResult<AppointmentDetail>>()
        val requestedIds = mutableListOf<Long>()

        override suspend fun getAppointmentDetail(appointmentId: Long): AppointmentDetailResult<AppointmentDetail> {
            requestedIds += appointmentId
            return results.removeFirst()
        }
    }

    private val repo = FakeRepository()

    private fun detail(title: String = "중간고사 카공 모임") = AppointmentDetail(
        appointmentId = 7L,
        title = title,
        canceled = false,
        dateSelectionType = AppointmentDateSelectionType.MULTIPLE,
        candidateDates = emptyList(),
        candidateLocations = emptyList(),
        responseDeadline = null,
        inviteUrl = null,
        confirmed = false,
        confirmedDate = null,
        confirmedTime = null,
        confirmedLocation = null,
        detailPlace = null,
        myRole = null,
        myAttendanceStatus = AppointmentAttendanceStatus.PENDING,
        myNotificationEnabled = false,
        myTravelMode = null,
        joinedCount = 1,
        scheduleSubmittedCount = 0,
        members = emptyList()
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `만들면 바로 해당 약속 ID로 불러오고 성공하면 약속 상세를 담는다`() {
        repo.results.add(AppointmentDetailResult.Success(detail()))
        val vm = AppointmentDetailLoadViewModel(7L, repo)

        assertEquals(listOf(7L), repo.requestedIds)
        assertEquals(AppointmentDetailLoadState.Loaded(detail()), vm.state.value)
    }

    @Test
    fun `로그인이 필요하면 상태만 바뀌고 다시 부르지 않는다`() {
        repo.results.add(AppointmentDetailResult.LoginRequired)
        val vm = AppointmentDetailLoadViewModel(7L, repo)

        assertEquals(AppointmentDetailLoadState.LoginRequired, vm.state.value)
        assertEquals(1, repo.requestedIds.size)
    }

    @Test
    fun `참여자가 아니면 참여자 아님 상태가 된다`() {
        repo.results.add(AppointmentDetailResult.NotParticipant)
        val vm = AppointmentDetailLoadViewModel(7L, repo)

        assertEquals(AppointmentDetailLoadState.NotParticipant, vm.state.value)
    }

    @Test
    fun `실패하면 재시도 가능 여부를 담고 다시 시도하면 성공할 수 있다`() {
        repo.results.add(AppointmentDetailResult.Failure(retryable = true))
        repo.results.add(AppointmentDetailResult.Success(detail("새 제목")))
        val vm = AppointmentDetailLoadViewModel(7L, repo)
        assertEquals(AppointmentDetailLoadState.Failed(retryable = true), vm.state.value)

        vm.load()

        assertEquals(2, repo.requestedIds.size)
        assertEquals(AppointmentDetailLoadState.Loaded(detail("새 제목")), vm.state.value)
    }

    @Test
    fun `재시도할 수 없는 실패도 상태에 그대로 담는다`() {
        repo.results.add(AppointmentDetailResult.Failure(retryable = false))
        val vm = AppointmentDetailLoadViewModel(7L, repo)

        assertEquals(AppointmentDetailLoadState.Failed(retryable = false), vm.state.value)
    }

    @Test
    fun `로그인 뒤 다시 불러오면 로그인 필요 상태에서 벗어난다`() {
        repo.results.add(AppointmentDetailResult.LoginRequired)
        repo.results.add(AppointmentDetailResult.Success(detail()))
        val vm = AppointmentDetailLoadViewModel(7L, repo)

        vm.load()

        assertEquals(AppointmentDetailLoadState.Loaded(detail()), vm.state.value)
    }

    @Test
    fun `약속 정보가 바뀌었다는 신호를 받으면 약속 상세를 다시 불러온다`() {
        repo.results.add(AppointmentDetailResult.Success(detail("옛 이름")))
        repo.results.add(AppointmentDetailResult.Success(detail("새 이름")))
        val signal = kotlinx.coroutines.flow.MutableStateFlow(0)
        val vm = AppointmentDetailLoadViewModel(7L, repo, signal)
        assertEquals(AppointmentDetailLoadState.Loaded(detail("옛 이름")), vm.state.value)

        signal.value = 1

        assertEquals(listOf(7L, 7L), repo.requestedIds)
        assertEquals(AppointmentDetailLoadState.Loaded(detail("새 이름")), vm.state.value)
    }
}
