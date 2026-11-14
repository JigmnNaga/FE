package com.jigmnnaga.presentation.notification

import com.jigmnnaga.domain.model.AppNotification
import com.jigmnnaga.domain.model.NotificationPage
import com.jigmnnaga.domain.model.NotificationResult
import com.jigmnnaga.domain.model.NotificationType
import com.jigmnnaga.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    private class FakeRepository : NotificationRepository {
        var pages: ArrayDeque<NotificationResult<NotificationPage>> = ArrayDeque()
        var readResult: NotificationResult<Unit> = NotificationResult.Success(Unit)
        val requestedCursors = mutableListOf<Long?>()
        val readIds = mutableListOf<Long>()

        override suspend fun getNotifications(cursor: Long?, size: Int): NotificationResult<NotificationPage> {
            requestedCursors += cursor
            return pages.removeFirst()
        }

        override suspend fun readNotification(notificationId: Long): NotificationResult<Unit> {
            readIds += notificationId
            return readResult
        }

        override suspend fun readAllNotifications(): NotificationResult<Long> = NotificationResult.Success(0)
    }

    private val now = 1_000_000_000_000L

    private fun item(id: Long, type: NotificationType = NotificationType.INVITED, appointmentId: Long? = 100 + id) =
        AppNotification(id, type, "태양님이 '모임$id' 약속에 초대했어요", "", appointmentId, false, now)

    private fun page(vararg items: AppNotification, next: Long? = null, hasNext: Boolean = false) =
        NotificationResult.Success(NotificationPage(items.toList(), next, hasNext))

    private val repo = FakeRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = NotificationViewModel(repo) { now }

    @Test
    fun `처음에는 로딩 상태이고 불러오면 목록이 채워진다`() {
        repo.pages.add(page(item(1), item(2, NotificationType.KICKED)))
        val vm = viewModel()
        assertEquals(NotificationLoadStatus.LOADING, vm.state.value.loadStatus)

        vm.load()

        val state = vm.state.value
        assertEquals(NotificationLoadStatus.LOADED, state.loadStatus)
        assertEquals(1, state.invites.size)
        assertEquals(1, state.activities.size)
        assertEquals("태양", state.invites[0].inviterName)
    }

    @Test
    fun `실패하면 에러 상태가 되고 다시 불러오면 회복한다`() {
        repo.pages.add(NotificationResult.Failure(true))
        repo.pages.add(page(item(1)))
        val vm = viewModel()

        vm.load()
        assertEquals(NotificationLoadStatus.ERROR, vm.state.value.loadStatus)

        vm.load()
        assertEquals(NotificationLoadStatus.LOADED, vm.state.value.loadStatus)
    }

    @Test
    fun `로그인이 필요하면 이벤트를 보내고 소비하면 비운다`() {
        repo.pages.add(NotificationResult.LoginRequired)
        val vm = viewModel()
        vm.load()
        assertEquals(NotificationEvent.LoginRequired, vm.event.value)
        vm.consumeEvent()
        assertNull(vm.event.value)
    }

    @Test
    fun `다음 페이지는 커서로 이어 붙이고 중복은 버린다`() {
        repo.pages.add(page(item(2), item(1), next = 1L, hasNext = true))
        repo.pages.add(page(item(1), item(0), next = null, hasNext = false))
        val vm = viewModel()
        vm.load()

        vm.loadMore()

        assertEquals(listOf<Long?>(null, 1L), repo.requestedCursors)
        assertEquals(listOf(2L, 1L, 0L), vm.state.value.invites.map { it.id })
        assertEquals(false, vm.state.value.hasNext)
    }

    @Test
    fun `다음 페이지가 없으면 더 부르지 않는다`() {
        repo.pages.add(page(item(1)))
        val vm = viewModel()
        vm.load()
        vm.loadMore()
        assertEquals(1, repo.requestedCursors.size)
    }

    @Test
    fun `알림을 누르면 읽음 처리하고 약속으로 이동하게 한다`() {
        repo.pages.add(page(item(1)))
        val vm = viewModel()
        vm.load()

        vm.onNotificationClick(1)

        assertEquals(listOf(1L), repo.readIds)
        assertEquals(InviteNotificationStatus.HANDLED, vm.state.value.invites[0].status)
        assertEquals(NotificationEvent.OpenAppointment(101L), vm.event.value)
    }

    @Test
    fun `읽음 처리에 실패하면 원래대로 되돌린다`() {
        repo.pages.add(page(item(1)))
        repo.readResult = NotificationResult.Failure(true)
        val vm = viewModel()
        vm.load()

        vm.onNotificationClick(1)

        assertEquals(InviteNotificationStatus.PENDING, vm.state.value.invites[0].status)
    }

    @Test
    fun `약속이 없는 알림은 이동 이벤트를 보내지 않는다`() {
        repo.pages.add(page(item(1, appointmentId = null)))
        val vm = viewModel()
        vm.load()
        vm.onNotificationClick(1)
        assertNull(vm.event.value)
    }

    @Test
    fun `탭을 바꾸면 선택 탭이 바뀐다`() {
        repo.pages.add(page(item(1)))
        val vm = viewModel()
        vm.load()
        vm.onTabSelect(NotificationTab.ACTIVITY)
        assertEquals(NotificationTab.ACTIVITY, vm.state.value.selectedTab)
    }
}
