package com.jigmnnaga.presentation.appointment.placevote

import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.LocationVote
import com.jigmnnaga.domain.model.MemberSchedule
import com.jigmnnaga.domain.model.MySchedule
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.ReminderSendSummary
import com.jigmnnaga.domain.model.ScheduleRejectReason
import com.jigmnnaga.domain.model.ScheduleResult
import com.jigmnnaga.domain.model.ScheduleStatus
import com.jigmnnaga.domain.model.ScheduleSubmission
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.AppointmentManageRepository
import com.jigmnnaga.domain.repository.MyMemberIdProvider
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import com.jigmnnaga.domain.repository.ScheduleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 장소 후보 저장(방장)과 장소 제출·투표 현황(참여자)을 서버 저장소와 이어 주는 동작을 확인한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceVoteServerViewModelTest {

    private class FakeManageRepository : AppointmentManageRepository {
        var result: AppointmentManageResult<Unit> = AppointmentManageResult.Success(Unit)
        val changes = mutableListOf<Pair<Long, CandidateLocationChange>>()
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun updateCandidateLocations(
            appointmentId: Long,
            change: CandidateLocationChange
        ): AppointmentManageResult<Unit> {
            changes += appointmentId to change
            gate?.await()
            return result
        }

        override suspend fun updateTitle(appointmentId: Long, title: String): AppointmentManageResult<Unit> = TODO()
        override suspend fun updateCandidateDates(
            appointmentId: Long,
            dateType: NewAppointmentDateType,
            dates: List<String>
        ): AppointmentManageResult<Unit> = TODO()

        override suspend fun updateResponseDeadline(appointmentId: Long, deadline: String): AppointmentManageResult<Unit> =
            TODO()

        override suspend fun cancel(appointmentId: Long): AppointmentManageResult<Unit> = TODO()
        override suspend fun delete(appointmentId: Long): AppointmentManageResult<Unit> = TODO()
        override suspend fun delegateHost(appointmentId: Long, newHostMemberId: Long): AppointmentManageResult<Unit> =
            TODO()

        override suspend fun kickParticipant(appointmentId: Long, targetMemberId: Long): AppointmentManageResult<Unit> =
            TODO()

        override suspend fun leave(appointmentId: Long, newHostMemberId: Long?): AppointmentManageResult<Unit> = TODO()
        override suspend fun updateNotificationSetting(
            appointmentId: Long,
            enabled: Boolean
        ): AppointmentManageResult<Unit> = TODO()

        override suspend fun sendReminders(appointmentId: Long): AppointmentManageResult<ReminderSendSummary> = TODO()
    }

    private class FakeScheduleRepository : ScheduleRepository {
        var mine: ScheduleResult<MySchedule> = ScheduleResult.Success(MySchedule(false, emptyList(), null))
        var status: ScheduleResult<ScheduleStatus> = ScheduleResult.Failure(retryable = true)
        var submitResult: ScheduleResult<Unit> = ScheduleResult.Success(Unit)
        val submissions = mutableListOf<Pair<Long, ScheduleSubmission>>()
        var statusCalls = 0

        override suspend fun getMySchedule(appointmentId: Long): ScheduleResult<MySchedule> = mine

        override suspend fun getScheduleStatus(appointmentId: Long): ScheduleResult<ScheduleStatus> {
            statusCalls++
            return status
        }

        override suspend fun submitSchedule(
            appointmentId: Long,
            submission: ScheduleSubmission
        ): ScheduleResult<Unit> {
            submissions += appointmentId to submission
            return submitResult
        }
    }

    private class FakeSearchRepository(var places: List<SearchedPlace> = emptyList()) : PlaceSearchRepository {
        override suspend fun search(
            query: String,
            type: PlaceSearchType,
            page: Int,
            size: Int
        ): PlaceSearchResult<PlaceSearchPage> = PlaceSearchResult.Success(PlaceSearchPage(places, true))
    }

    private val manage = FakeManageRepository()
    private val schedule = FakeScheduleRepository()
    private val search = FakeSearchRepository()
    private var changedCount = 0

    private val texts = PlaceVoteTexts(
        voteLabel = { count, total -> "$count/${total}명" },
        progressTitle = { "$it 선택" },
        progressSubtitle = { "$it 남음" },
        progressSelecting = "선택중",
        progressDone = "완료"
    )

    private val memberIds = object : MyMemberIdProvider {
        override suspend fun myMemberId(): Long? = 11L
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun candidates() = listOf(
        PlaceCandidateUiModel("location-1", "강남역"),
        PlaceCandidateUiModel("location-2", "잠실역")
    )

    private fun template(role: PlaceVoteRole, selected: Set<String> = emptySet()) = PlaceVoteUiState(
        role = role,
        searchQuery = "",
        candidates = candidates(),
        selectedCandidateIds = selected,
        title = "일정 조율",
        question = "어디서 만날까요?",
        submitLabel = "저장",
        showSearchInput = true
    )

    private fun hostViewModel() = PlaceVoteViewModel(
        template = template(PlaceVoteRole.HOST, selected = setOf("location-1", "location-2")),
        searchRepository = search,
        searchDebounceMillis = 0L,
        appointmentId = APPOINTMENT_ID,
        manageRepository = manage,
        scheduleRepository = schedule,
        texts = texts,
        onChanged = { changedCount++ }
    )

    private fun participantViewModel() = PlaceVoteViewModel(
        template = template(PlaceVoteRole.PARTICIPANT),
        appointmentId = APPOINTMENT_ID,
        manageRepository = manage,
        scheduleRepository = schedule,
        myMemberIdProvider = memberIds,
        texts = texts,
        onChanged = { changedCount++ }
    )

    private fun member(id: Long, nickname: String, preferred: Long?) = MemberSchedule(
        memberId = id,
        nickname = nickname,
        profileImageUrl = null,
        submitted = preferred != null,
        availabilities = emptyList(),
        preferredLocationId = preferred
    )

    private fun vote(locationId: Long, name: String, count: Int) = LocationVote(
        locationId = locationId,
        placeName = name,
        address = null,
        lineNames = null,
        voteCount = count,
        topVoted = false,
        voters = emptyList()
    )

    private fun status(members: List<MemberSchedule>, votes: List<LocationVote>) = ScheduleStatus(
        totalCount = 3,
        submittedCount = members.count { it.preferredLocationId != null },
        members = members,
        locationVotes = votes
    )

    private fun searched(id: String, latitude: Double?, longitude: Double?) = SearchedPlace(
        kakaoPlaceId = id,
        name = "신촌역",
        lineNames = "2호선",
        roadAddress = "서울 서대문구",
        latitude = latitude,
        longitude = longitude
    )

    // ---- 방장 ----

    @Test
    fun `방장은 서버의 후보 장소를 모두 고른 채로 시작하고 저장하면 ID로 유지한다`() {
        val vm = hostViewModel()
        assertEquals(setOf("location-1", "location-2"), vm.uiState.value.selectedCandidateIds)

        vm.onSubmit()

        assertEquals(
            listOf(APPOINTMENT_ID to CandidateLocationChange(keepLocationIds = listOf(1L, 2L), newLocations = emptyList())),
            manage.changes
        )
        assertEquals(PlaceVoteNotice.SAVED, vm.notice.value)
        assertEquals(1, changedCount)
    }

    @Test
    fun `검색해서 고른 새 장소는 위치와 함께 새 장소로 보낸다`() {
        search.places = listOf(searched("kakao-1", 37.55, 126.94))
        val vm = hostViewModel()
        vm.onSearchQueryChange("신촌")
        vm.onSelectionChange(setOf("location-1", "location-2", "kakao-1"))
        vm.onSearchQueryChange("")

        vm.onSubmit()

        val change = manage.changes.single().second
        assertEquals(listOf(1L, 2L), change.keepLocationIds)
        assertEquals(
            listOf(NewCandidateLocation("신촌역", "서울 서대문구", "2호선", 37.55, 126.94)),
            change.newLocations
        )
    }

    @Test
    fun `위치가 없는 새 장소는 보내지 않고 안내한다`() {
        search.places = listOf(searched("kakao-1", null, null))
        val vm = hostViewModel()
        vm.onSearchQueryChange("신촌")
        vm.onSelectionChange(setOf("location-1", "kakao-1"))

        vm.onSubmit()

        assertTrue(manage.changes.isEmpty())
        assertEquals(PlaceVoteNotice.ACTION_INVALID_PLACE, vm.notice.value)
        assertEquals(0, changedCount)
    }

    @Test
    fun `방장 저장이 받아지지 않으면 이유에 맞는 안내를 보낸다`() {
        val vm = hostViewModel()
        val cases = listOf(
            AppointmentManageResult.Rejected(AppointmentManageRejectReason.INVALID_LOCATIONS) to
                PlaceVoteNotice.ACTION_INVALID_PLACE,
            AppointmentManageResult.Rejected(AppointmentManageRejectReason.APPOINTMENT_CANCELED) to
                PlaceVoteNotice.ACTION_CLOSED,
            AppointmentManageResult.NotHost to PlaceVoteNotice.ACTION_NOT_ALLOWED,
            AppointmentManageResult.Failure(retryable = true) to PlaceVoteNotice.ACTION_FAILED_RETRYABLE,
            AppointmentManageResult.Failure(retryable = false) to PlaceVoteNotice.ACTION_FAILED
        )
        for ((result, expected) in cases) {
            manage.result = result
            vm.onSubmit()
            assertEquals(expected, vm.notice.value)
            vm.clearNotice()
        }
        assertEquals(0, changedCount)
        assertEquals(setOf("location-1", "location-2"), vm.uiState.value.selectedCandidateIds)
    }

    @Test
    fun `로그인이 필요하면 안내하고 로그인한 뒤 다시 저장한다`() {
        manage.result = AppointmentManageResult.LoginRequired
        val vm = hostViewModel()
        vm.onSubmit()
        assertEquals(PlaceVoteNotice.ACTION_LOGIN_REQUIRED, vm.notice.value)
        assertEquals(1, manage.changes.size)

        manage.result = AppointmentManageResult.Success(Unit)
        vm.clearNotice()
        vm.retryAfterLogin()

        assertEquals(2, manage.changes.size)
        assertEquals(PlaceVoteNotice.SAVED, vm.notice.value)
    }

    @Test
    fun `저장하는 동안에는 저장 버튼을 막고 같은 요청을 다시 보내지 않는다`() {
        val gate = CompletableDeferred<Unit>()
        manage.gate = gate
        val vm = hostViewModel()

        vm.onSubmit()
        assertFalse(vm.uiState.value.submitEnabled)
        vm.onSubmit()
        assertEquals(1, manage.changes.size)

        gate.complete(Unit)
        assertTrue(vm.uiState.value.submitEnabled)
        assertEquals(PlaceVoteNotice.SAVED, vm.notice.value)
    }

    @Test
    fun `방장 화면은 투표 현황을 불러오지 않는다`() {
        hostViewModel()
        assertEquals(0, schedule.statusCalls)
    }

    // ---- 참여자 ----

    @Test
    fun `참여자는 화면을 열면 내가 고른 장소와 투표 현황을 채운다`() {
        schedule.mine = ScheduleResult.Success(MySchedule(true, emptyList(), preferredLocationId = 2L))
        schedule.status = ScheduleResult.Success(
            status(
                members = listOf(member(10, "태희", 2L), member(11, "나", 2L), member(12, "하늘", null)),
                votes = listOf(vote(1, "강남역", 0), vote(2, "잠실역", 2))
            )
        )

        val state = participantViewModel().uiState.value

        assertEquals(setOf("location-2"), state.selectedCandidateIds)
        assertEquals(listOf("0/3명", "2/3명"), state.candidates.map { it.voteLabel })
        assertEquals(listOf(false, false), state.candidates.map { it.isVoteComplete })
        val progress = state.progress!!
        assertEquals("2 선택", progress.title)
        assertEquals("1 남음", progress.subtitle)
        assertEquals("선택중", progress.statusLabel)
        assertEquals(listOf("태", "나", "하"), progress.members.map { it.initial })
        assertEquals(listOf(false, true, false), progress.members.map { it.isMe })
        assertEquals(listOf("태희", "나", "하늘"), progress.members.map { it.name })
        assertNull(progress.members.last().placeName)
    }

    @Test
    fun `모든 멤버가 같은 장소를 골랐으면 완료로 표시한다`() {
        schedule.status = ScheduleResult.Success(
            status(
                members = listOf(member(10, "태희", 1L), member(11, "나", 1L), member(12, "하늘", 1L)),
                votes = listOf(vote(1, "강남역", 3), vote(2, "잠실역", 0))
            )
        )

        val state = participantViewModel().uiState.value

        assertEquals(listOf(true, false), state.candidates.map { it.isVoteComplete })
        assertNull(state.progress!!.subtitle)
        assertEquals("완료", state.progress!!.statusLabel)
    }

    @Test
    fun `참여자는 하나만 고르고 다른 장소를 누르면 선택이 바뀐다`() {
        schedule.status = ScheduleResult.Success(status(emptyList(), emptyList()))
        val vm = participantViewModel()

        vm.onSelectionChange(setOf("location-1"))
        assertEquals(setOf("location-1"), vm.uiState.value.selectedCandidateIds)

        vm.onSelectionChange(setOf("location-1", "location-2"))
        assertEquals(setOf("location-2"), vm.uiState.value.selectedCandidateIds)
        assertNull(vm.notice.value)

        vm.onSelectionChange(emptySet())
        assertTrue(vm.uiState.value.selectedCandidateIds.isEmpty())
        assertFalse(vm.uiState.value.submitEnabled)
    }

    @Test
    fun `고른 장소를 제출하면 희망 장소만 보내고 투표 현황을 다시 불러온다`() {
        schedule.status = ScheduleResult.Success(status(emptyList(), emptyList()))
        val vm = participantViewModel()
        vm.onSelectionChange(setOf("location-2"))

        vm.onSubmit()

        assertEquals(
            listOf(APPOINTMENT_ID to ScheduleSubmission(availabilities = null, preferredLocationId = 2L)),
            schedule.submissions
        )
        assertEquals(PlaceVoteNotice.SUBMITTED, vm.notice.value)
        assertEquals(1, changedCount)
        assertEquals(2, schedule.statusCalls)
        assertEquals(setOf("location-2"), vm.uiState.value.selectedCandidateIds)
    }

    @Test
    fun `제출이 받아지지 않으면 이유에 맞는 안내를 보낸다`() {
        val vm = participantViewModel()
        vm.onSelectionChange(setOf("location-1"))
        val cases = listOf(
            ScheduleResult.Rejected(ScheduleRejectReason.DEADLINE_PASSED) to PlaceVoteNotice.ACTION_CLOSED,
            ScheduleResult.Rejected(ScheduleRejectReason.APPOINTMENT_CANCELED) to PlaceVoteNotice.ACTION_CLOSED,
            ScheduleResult.Rejected(ScheduleRejectReason.INVALID_LOCATION) to PlaceVoteNotice.ACTION_INVALID_PLACE,
            ScheduleResult.NotParticipant to PlaceVoteNotice.ACTION_NOT_ALLOWED,
            ScheduleResult.Failure(retryable = true) to PlaceVoteNotice.ACTION_FAILED_RETRYABLE,
            ScheduleResult.Failure(retryable = false) to PlaceVoteNotice.ACTION_FAILED
        )
        for ((result, expected) in cases) {
            schedule.submitResult = result
            vm.onSubmit()
            assertEquals(expected, vm.notice.value)
            vm.clearNotice()
        }
        assertEquals(0, changedCount)
        assertEquals(setOf("location-1"), vm.uiState.value.selectedCandidateIds)
    }

    @Test
    fun `투표 현황을 불러오지 못해도 후보는 그대로 보여 주고 안내한다`() {
        schedule.status = ScheduleResult.Failure(retryable = true)

        val vm = participantViewModel()

        assertEquals(PlaceVoteNotice.VOTES_LOAD_FAILED, vm.notice.value)
        assertEquals(2, vm.uiState.value.candidates.size)
        assertNull(vm.uiState.value.candidates.first().voteLabel)
        assertNull(vm.uiState.value.progress)
    }

    @Test
    fun `로그인이 필요해 현황을 못 불러오면 로그인한 뒤 다시 불러온다`() {
        schedule.mine = ScheduleResult.LoginRequired
        val vm = participantViewModel()
        assertEquals(PlaceVoteNotice.ACTION_LOGIN_REQUIRED, vm.notice.value)

        schedule.mine = ScheduleResult.Success(MySchedule(true, emptyList(), preferredLocationId = 1L))
        schedule.status = ScheduleResult.Success(
            status(listOf(member(11, "나", 1L)), listOf(vote(1, "강남역", 1)))
        )
        vm.clearNotice()
        vm.retryAfterLogin()

        assertEquals(setOf("location-1"), vm.uiState.value.selectedCandidateIds)
        assertEquals("1/3명", vm.uiState.value.candidates.first().voteLabel)
    }

    private companion object {
        const val APPOINTMENT_ID = 77L
    }
}
