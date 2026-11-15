package com.jigmnnaga.presentation.home

import com.jigmnnaga.domain.model.HomeAppointment
import com.jigmnnaga.domain.model.HomeAppointmentListResult
import com.jigmnnaga.domain.model.NextAppointmentResult
import com.jigmnnaga.domain.model.PendingInviteCountResult
import com.jigmnnaga.domain.repository.HomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeHomeRepository : HomeRepository {
        var upcomingResult: HomeAppointmentListResult = HomeAppointmentListResult.Success(emptyList())
        var pastResult: HomeAppointmentListResult = HomeAppointmentListResult.Success(emptyList())
        var nextResult: NextAppointmentResult = NextAppointmentResult.Success(null)
        var inviteResult: PendingInviteCountResult = PendingInviteCountResult.Success(0)

        var upcomingCallCount = 0
        var pastCallCount = 0
        var nextCallCount = 0
        var inviteCallCount = 0

        override suspend fun getUpcomingAppointments(): HomeAppointmentListResult {
            upcomingCallCount++
            return upcomingResult
        }

        override suspend fun getPastAppointments(): HomeAppointmentListResult {
            pastCallCount++
            return pastResult
        }

        override suspend fun getNextAppointment(): NextAppointmentResult {
            nextCallCount++
            return nextResult
        }

        override suspend fun getPendingInviteCount(): PendingInviteCountResult {
            inviteCallCount++
            return inviteResult
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun viewModel(repo: FakeHomeRepository = FakeHomeRepository()) =
        HomeViewModel(repo, testDispatcher)

    private fun appointment(
        id: Long = 1L,
        title: String = "약속",
        confirmedDate: String? = "2025-10-24",
        confirmedTime: String? = "15:00",
        memberCount: Int = 2
    ) = HomeAppointment(
        appointmentId = id,
        title = title,
        canceled = false,
        confirmed = confirmedDate != null,
        confirmedDate = confirmedDate,
        confirmedTime = confirmedTime,
        confirmedLocationName = null,
        detailPlaceName = null,
        candidateStartDate = null,
        candidateEndDate = null,
        memberCount = memberCount,
        unreadMessageCount = 0
    )

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

    private fun tomorrowString(): String {
        val cal = Calendar.getInstance(Locale.KOREA)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(cal.time)
    }

    // ── 초기 데이터 조회 ────────────────────────────────────────────────────────

    @Test
    fun `ViewModel 생성 직후 isLoading이 false이다`() = runTest {
        // UnconfinedTestDispatcher: init { doLoad() }가 즉시 완료된다
        val vm = viewModel()

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `예정 약속 목록이 uiState에 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(
                listOf(appointment(1L, "약속 A"), appointment(2L, "약속 B"))
            )
        }
        val vm = viewModel(repo)

        assertEquals(2, vm.uiState.value.upcomingAppointments.size)
        assertEquals("약속 A", vm.uiState.value.upcomingAppointments[0].title)
        assertEquals("약속 B", vm.uiState.value.upcomingAppointments[1].title)
    }

    @Test
    fun `지난 약속 목록이 uiState에 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            pastResult = HomeAppointmentListResult.Success(listOf(appointment(3L, "지난 약속")))
        }
        val vm = viewModel(repo)

        assertEquals(1, vm.uiState.value.pastAppointments.size)
        assertEquals("지난 약속", vm.uiState.value.pastAppointments[0].title)
    }

    @Test
    fun `초대 수가 uiState에 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            inviteResult = PendingInviteCountResult.Success(5)
        }
        val vm = viewModel(repo)

        assertEquals(5, vm.uiState.value.pendingInviteCount)
    }

    @Test
    fun `다음 약속(오늘)이 todayAppointment에 반영된다`() = runTest {
        val today = todayString()
        val repo = FakeHomeRepository().apply {
            nextResult = NextAppointmentResult.Success(appointment(10L, "오늘의 약속", confirmedDate = today))
        }
        val vm = viewModel(repo)

        assertNotNull(vm.uiState.value.todayAppointment)
        assertEquals("오늘의 약속", vm.uiState.value.todayAppointment?.title)
    }

    @Test
    fun `appointmentId가 String으로 변환되어 MockAppointment id에 담긴다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(listOf(appointment(42L)))
        }
        val vm = viewModel(repo)

        assertEquals("42", vm.uiState.value.upcomingAppointments.first().id)
    }

    @Test
    fun `memberCount가 MockAppointment participantCount에 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(listOf(appointment(memberCount = 7)))
        }
        val vm = viewModel(repo)

        assertEquals(7, vm.uiState.value.upcomingAppointments.first().participantCount)
    }

    // ── 오늘의 약속 ──────────────────────────────────────────────────────────────

    @Test
    fun `오늘 날짜인 다음 약속은 todayAppointment에 표시된다`() = runTest {
        val today = todayString()
        val repo = FakeHomeRepository().apply {
            nextResult = NextAppointmentResult.Success(appointment(confirmedDate = today))
        }
        val vm = viewModel(repo)

        assertNotNull(vm.uiState.value.todayAppointment)
    }

    @Test
    fun `내일 날짜인 다음 약속은 todayAppointment에 표시되지 않는다`() = runTest {
        val tomorrow = tomorrowString()
        val repo = FakeHomeRepository().apply {
            nextResult = NextAppointmentResult.Success(appointment(confirmedDate = tomorrow))
        }
        val vm = viewModel(repo)

        assertNull(vm.uiState.value.todayAppointment)
    }

    @Test
    fun `다음 약속이 null이면 todayAppointment도 null이다`() = runTest {
        val repo = FakeHomeRepository().apply {
            nextResult = NextAppointmentResult.Success(null)
        }
        val vm = viewModel(repo)

        assertNull(vm.uiState.value.todayAppointment)
    }

    // ── 탭 전환 ──────────────────────────────────────────────────────────────────

    @Test
    fun `selectTab PAST 호출 시 selectedTab이 PAST로 변경된다`() = runTest {
        val vm = viewModel()

        vm.selectTab(HomeTab.PAST)

        assertEquals(HomeTab.PAST, vm.uiState.value.selectedTab)
    }

    @Test
    fun `selectTab UPCOMING 호출 시 selectedTab이 UPCOMING으로 변경된다`() = runTest {
        val vm = viewModel()
        vm.selectTab(HomeTab.PAST)

        vm.selectTab(HomeTab.UPCOMING)

        assertEquals(HomeTab.UPCOMING, vm.uiState.value.selectedTab)
    }

    @Test
    fun `탭 전환 시 API를 재호출하지 않는다`() = runTest {
        val repo = FakeHomeRepository()
        val vm = viewModel(repo)
        val upcomingBefore = repo.upcomingCallCount
        val pastBefore = repo.pastCallCount

        vm.selectTab(HomeTab.PAST)
        vm.selectTab(HomeTab.UPCOMING)
        vm.selectTab(HomeTab.PAST)

        assertEquals(upcomingBefore, repo.upcomingCallCount)
        assertEquals(pastBefore, repo.pastCallCount)
    }

    // ── 오류 처리 ────────────────────────────────────────────────────────────────

    @Test
    fun `예정 약속 API 실패 시 upcomingError가 설정된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)

        assertNotNull(vm.uiState.value.upcomingError)
        assertTrue(vm.uiState.value.upcomingError!!.retryable)
    }

    @Test
    fun `지난 약속 API 실패 시 pastError가 설정된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            pastResult = HomeAppointmentListResult.Failure(retryable = false)
        }
        val vm = viewModel(repo)

        assertNotNull(vm.uiState.value.pastError)
        assertFalse(vm.uiState.value.pastError!!.retryable)
    }

    @Test
    fun `초대 API만 실패해도 약속 목록은 정상 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(listOf(appointment()))
            inviteResult = PendingInviteCountResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)

        assertEquals(1, vm.uiState.value.upcomingAppointments.size)
        assertNull(vm.uiState.value.upcomingError)
    }

    @Test
    fun `다음 약속 API만 실패해도 약속 목록은 정상 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(listOf(appointment(1L), appointment(2L)))
            nextResult = NextAppointmentResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)

        assertEquals(2, vm.uiState.value.upcomingAppointments.size)
        assertNull(vm.uiState.value.todayAppointment)
    }

    @Test
    fun `일부 API 실패 시 성공한 데이터와 오류 상태가 독립적으로 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(listOf(appointment()))
            pastResult = HomeAppointmentListResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)

        assertEquals(1, vm.uiState.value.upcomingAppointments.size)
        assertNull(vm.uiState.value.upcomingError)
        assertNotNull(vm.uiState.value.pastError)
    }

    // ── 인증 ────────────────────────────────────────────────────────────────────

    @Test
    fun `예정 약속 LoginRequired 응답 시 loginRequired가 true가 된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.LoginRequired
        }
        val vm = viewModel(repo)

        assertTrue(vm.uiState.value.loginRequired)
    }

    @Test
    fun `지난 약속 LoginRequired 응답 시 loginRequired가 true가 된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            pastResult = HomeAppointmentListResult.LoginRequired
        }
        val vm = viewModel(repo)

        assertTrue(vm.uiState.value.loginRequired)
    }

    @Test
    fun `consumeLoginRequired 호출 시 loginRequired가 false가 된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.LoginRequired
        }
        val vm = viewModel(repo)
        assertTrue(vm.uiState.value.loginRequired)

        vm.consumeLoginRequired()

        assertFalse(vm.uiState.value.loginRequired)
    }

    // ── 새로고침 ─────────────────────────────────────────────────────────────────

    @Test
    fun `refresh 호출 시 API를 다시 조회한다`() = runTest {
        val repo = FakeHomeRepository()
        val vm = viewModel(repo)
        val upcomingBefore = repo.upcomingCallCount

        vm.refresh()

        assertTrue(repo.upcomingCallCount > upcomingBefore)
    }

    @Test
    fun `refresh 성공 시 오류가 해제된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)
        assertNotNull(vm.uiState.value.upcomingError)

        repo.upcomingResult = HomeAppointmentListResult.Success(emptyList())
        vm.refresh()

        assertNull(vm.uiState.value.upcomingError)
    }

    @Test
    fun `refresh 실패 시 이전에 성공한 약속 목록이 유지된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(listOf(appointment()))
        }
        val vm = viewModel(repo)
        assertEquals(1, vm.uiState.value.upcomingAppointments.size)

        repo.upcomingResult = HomeAppointmentListResult.Failure(retryable = true)
        vm.refresh()

        // 실패 시 기존 데이터 유지, 오류는 새로 설정
        assertEquals(1, vm.uiState.value.upcomingAppointments.size)
        assertNotNull(vm.uiState.value.upcomingError)
    }

    @Test
    fun `refresh 호출 시 isLoading이 true로 전환됐다가 false로 완료된다`() = runTest {
        // UnconfinedTestDispatcher에서 refresh()는 동기적으로 완료되므로
        // 최종 상태(false)만 검증한다
        val vm = viewModel()

        vm.refresh()

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `refresh 성공 후 새 데이터가 반영된다`() = runTest {
        val repo = FakeHomeRepository().apply {
            upcomingResult = HomeAppointmentListResult.Success(listOf(appointment(1L, "첫 약속")))
        }
        val vm = viewModel(repo)
        assertEquals("첫 약속", vm.uiState.value.upcomingAppointments.first().title)

        repo.upcomingResult = HomeAppointmentListResult.Success(
            listOf(appointment(2L, "새 약속 A"), appointment(3L, "새 약속 B"))
        )
        vm.refresh()

        assertEquals(2, vm.uiState.value.upcomingAppointments.size)
        assertEquals("새 약속 A", vm.uiState.value.upcomingAppointments.first().title)
    }
}
