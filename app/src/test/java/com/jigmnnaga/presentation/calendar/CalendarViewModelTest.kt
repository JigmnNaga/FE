package com.jigmnnaga.presentation.calendar

import com.jigmnnaga.domain.model.CalendarDay
import com.jigmnnaga.domain.model.CalendarResult
import com.jigmnnaga.domain.model.HomeAppointment
import com.jigmnnaga.domain.repository.CalendarRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    /** 테스트용 고정 오늘 날짜 — 2026년 9월 6일(일) */
    private val fixedToday = CalendarDate(2026, 9, 6)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeCalendarRepository : CalendarRepository {
        var result: CalendarResult = CalendarResult.Success(emptyList())
        var callCount = 0
        var lastMonth: String? = null

        override suspend fun getCalendar(month: String): CalendarResult {
            callCount++
            lastMonth = month
            return result
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun viewModel(
        repo: FakeCalendarRepository = FakeCalendarRepository(),
        today: CalendarDate = fixedToday
    ) = CalendarViewModel(repo, testDispatcher, today)

    private fun appointment(
        id: Long = 1L,
        title: String = "테스트 약속",
        confirmedDate: String? = "2026-09-06",
        confirmedTime: String? = "15:00",
        locationName: String? = "강남역",
        detailPlace: String? = null,
        confirmed: Boolean = true,
        memberCount: Int = 3
    ) = HomeAppointment(
        appointmentId = id,
        title = title,
        canceled = false,
        confirmed = confirmed,
        confirmedDate = confirmedDate,
        confirmedTime = confirmedTime,
        confirmedLocationName = locationName,
        detailPlaceName = detailPlace,
        candidateStartDate = null,
        candidateEndDate = null,
        memberCount = memberCount,
        unreadMessageCount = 0
    )

    private fun day(date: String, vararg appointments: HomeAppointment) =
        CalendarDay(date, appointments.toList())

    private fun successOf(vararg days: CalendarDay) =
        CalendarResult.Success(days.toList())

    // ── 초기 로드 ─────────────────────────────────────────────────────────────

    @Test
    fun `ViewModel 생성 직후 isLoading이 false이다`() = runTest {
        // UnconfinedTestDispatcher: init { doLoad() }가 즉시 완료된다
        val vm = viewModel()

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `초기 displayMonth는 today와 동일한 연월이다`() = runTest {
        val vm = viewModel()

        assertEquals(fixedToday.year, vm.uiState.value.displayMonth.year)
        assertEquals(fixedToday.month, vm.uiState.value.displayMonth.month)
    }

    // ── 성공 응답 ─────────────────────────────────────────────────────────────

    @Test
    fun `성공 응답 시 약속이 uiState에 반영된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(day("2026-09-06", appointment(1L, "스터디")))
        }
        val vm = viewModel(repo)

        assertEquals(1, vm.uiState.value.appointments.size)
        assertEquals("스터디", vm.uiState.value.appointments[0].title)
    }

    @Test
    fun `appointmentId가 String으로 변환되어 MockCalendarAppointment id에 담긴다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(day("2026-09-06", appointment(99L)))
        }
        val vm = viewModel(repo)

        assertEquals("99", vm.uiState.value.appointments.first().id)
    }

    @Test
    fun `memberCount가 그대로 보존되지는 않고 participants는 emptyList이다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(day("2026-09-06", appointment(memberCount = 5)))
        }
        val vm = viewModel(repo)

        assertTrue(vm.uiState.value.appointments.first().participants.isEmpty())
    }

    // ── 빈 월 ─────────────────────────────────────────────────────────────────

    @Test
    fun `빈 월 응답 시 appointments가 emptyList이고 error는 null이다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = CalendarResult.Success(emptyList())
        }
        val vm = viewModel(repo)

        assertTrue(vm.uiState.value.appointments.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    // ── 이전·다음 달 이동 ─────────────────────────────────────────────────────

    @Test
    fun `goToPrevMonth 호출 시 displayMonth가 이전 달로 변경된다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)

        vm.goToPrevMonth()

        assertEquals(2026, vm.uiState.value.displayMonth.year)
        assertEquals(8, vm.uiState.value.displayMonth.month)
    }

    @Test
    fun `goToNextMonth 호출 시 displayMonth가 다음 달로 변경된다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)

        vm.goToNextMonth()

        assertEquals(2026, vm.uiState.value.displayMonth.year)
        assertEquals(10, vm.uiState.value.displayMonth.month)
    }

    @Test
    fun `1월에서 goToPrevMonth 호출 시 연도가 하나 줄고 12월이 된다`() = runTest {
        val janToday = CalendarDate(2026, 1, 1)
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo, today = janToday)

        vm.goToPrevMonth()

        assertEquals(2025, vm.uiState.value.displayMonth.year)
        assertEquals(12, vm.uiState.value.displayMonth.month)
    }

    @Test
    fun `12월에서 goToNextMonth 호출 시 연도가 하나 늘고 1월이 된다`() = runTest {
        val decToday = CalendarDate(2026, 12, 1)
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo, today = decToday)

        vm.goToNextMonth()

        assertEquals(2027, vm.uiState.value.displayMonth.year)
        assertEquals(1, vm.uiState.value.displayMonth.month)
    }

    @Test
    fun `월 이동 시 API를 재호출한다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)
        val before = repo.callCount

        vm.goToNextMonth()

        assertTrue(repo.callCount > before)
        assertEquals("2026-10", repo.lastMonth)
    }

    @Test
    fun `goToPrevMonth API 호출 파라미터가 이전 달 형식이다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)

        vm.goToPrevMonth()

        assertEquals("2026-08", repo.lastMonth)
    }

    // ── 날짜 선택 ─────────────────────────────────────────────────────────────

    @Test
    fun `selectDate 호출 시 selectedDate가 설정된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(day("2026-09-12", appointment()))
        }
        val vm = viewModel(repo)

        vm.selectDate(12)

        assertEquals(CalendarDate(2026, 9, 12), vm.uiState.value.selectedDate)
    }

    @Test
    fun `같은 날짜를 다시 selectDate하면 선택이 해제된다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)

        vm.selectDate(12)
        vm.selectDate(12)

        assertNull(vm.uiState.value.selectedDate)
    }

    @Test
    fun `selectDate 호출 시 API를 재호출하지 않는다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)
        val before = repo.callCount

        vm.selectDate(6)
        vm.selectDate(12)
        vm.selectDate(6)

        assertEquals(before, repo.callCount)
    }

    @Test
    fun `selectedDate가 설정되면 monthAppointments가 해당 날짜만 필터링된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(
                day("2026-09-06", appointment(1L)),
                day("2026-09-12", appointment(2L))
            )
        }
        val vm = viewModel(repo)

        vm.selectDate(6)

        val filtered = vm.uiState.value.monthAppointments
        assertEquals(1, filtered.size)
        assertEquals(6, filtered.first().day)
    }

    @Test
    fun `selectedDate 해제 시 monthAppointments가 전체 월 약속을 반환한다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(
                day("2026-09-06", appointment(1L)),
                day("2026-09-12", appointment(2L))
            )
        }
        val vm = viewModel(repo)
        vm.selectDate(6)
        vm.selectDate(6) // 해제

        assertEquals(2, vm.uiState.value.monthAppointments.size)
    }

    @Test
    fun `월 이동 시 selectedDate가 초기화된다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)
        vm.selectDate(6)
        assertNotNull(vm.uiState.value.selectedDate)

        vm.goToNextMonth()

        assertNull(vm.uiState.value.selectedDate)
    }

    // ── 경쟁 조건 (빠른 이동) ────────────────────────────────────────────────

    @Test
    fun `빠른 월 이동 시 최종 displayMonth에 맞는 상태가 반영된다`() = runTest {
        val repo = FakeCalendarRepository()
        val vm = viewModel(repo)

        // 빠르게 여러 번 이동 (UnconfinedTestDispatcher에서 각 doLoad가 즉시 완료)
        vm.goToNextMonth()  // 10월
        vm.goToNextMonth()  // 11월
        vm.goToPrevMonth()  // 10월

        assertEquals(10, vm.uiState.value.displayMonth.month)
    }

    // ── 오류 처리 ─────────────────────────────────────────────────────────────

    @Test
    fun `API Failure 시 error가 설정되고 retryable 값이 반영된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = CalendarResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error!!.retryable)
    }

    @Test
    fun `non-retryable Failure 시 error retryable이 false이다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = CalendarResult.Failure(retryable = false)
        }
        val vm = viewModel(repo)

        assertFalse(vm.uiState.value.error!!.retryable)
    }

    // ── 재시도 ────────────────────────────────────────────────────────────────

    @Test
    fun `retry 호출 시 API를 다시 조회한다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = CalendarResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)
        val before = repo.callCount

        repo.result = CalendarResult.Success(emptyList())
        vm.retry()

        assertTrue(repo.callCount > before)
    }

    @Test
    fun `retry 성공 시 error가 null이 된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = CalendarResult.Failure(retryable = true)
        }
        val vm = viewModel(repo)
        assertNotNull(vm.uiState.value.error)

        repo.result = CalendarResult.Success(emptyList())
        vm.retry()

        assertNull(vm.uiState.value.error)
    }

    // ── 로그인 필요 ───────────────────────────────────────────────────────────

    @Test
    fun `LoginRequired 응답 시 loginRequired가 true가 된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = CalendarResult.LoginRequired
        }
        val vm = viewModel(repo)

        assertTrue(vm.uiState.value.loginRequired)
    }

    @Test
    fun `consumeLoginRequired 호출 시 loginRequired가 false가 된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = CalendarResult.LoginRequired
        }
        val vm = viewModel(repo)
        assertTrue(vm.uiState.value.loginRequired)

        vm.consumeLoginRequired()

        assertFalse(vm.uiState.value.loginRequired)
    }

    // ── 장소·확정 필드 변환 ───────────────────────────────────────────────────

    @Test
    fun `confirmedLocationName이 null이고 detailPlaceName도 null이면 location이 장소 미정이다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(
                day("2026-09-06", appointment(locationName = null, detailPlace = null))
            )
        }
        val vm = viewModel(repo)

        assertEquals("장소 미정", vm.uiState.value.appointments.first().location)
    }

    @Test
    fun `confirmedLocationName이 null이면 detailPlaceName을 location으로 사용한다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(
                day("2026-09-06", appointment(locationName = null, detailPlace = "지하 1층"))
            )
        }
        val vm = viewModel(repo)

        assertEquals("지하 1층", vm.uiState.value.appointments.first().location)
    }

    @Test
    fun `confirmed=false이지만 confirmedDate가 있는 약속도 정상 변환된다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(
                day("2026-09-10", appointment(id = 42L, confirmed = false, confirmedDate = "2026-09-10"))
            )
        }
        val vm = viewModel(repo)

        val appt = vm.uiState.value.appointments.first()
        assertEquals("42", appt.id)
        assertEquals(10, appt.day)
    }

    // ── TODAY 레이블 ─────────────────────────────────────────────────────────

    @Test
    fun `오늘 날짜 약속의 dayLabel이 TODAY이고 isToday가 true이다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(day("2026-09-06", appointment(confirmedDate = "2026-09-06")))
        }
        val vm = viewModel(repo)  // fixedToday = 2026-09-06

        val appt = vm.uiState.value.appointments.first()
        assertEquals("TODAY", appt.dayLabel)
        assertTrue(appt.isToday)
    }

    @Test
    fun `오늘이 아닌 날짜 약속의 isToday가 false이고 dayLabel이 요일이다`() = runTest {
        val repo = FakeCalendarRepository().apply {
            result = successOf(day("2026-09-12", appointment(confirmedDate = "2026-09-12")))
        }
        val vm = viewModel(repo)  // fixedToday = 2026-09-06, 2026-09-12 는 토요일

        val appt = vm.uiState.value.appointments.first()
        assertFalse(appt.isToday)
        assertEquals("토", appt.dayLabel)
    }
}
