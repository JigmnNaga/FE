package com.jigmnnaga.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.jigmnnaga.domain.model.ChatMessage
import com.jigmnnaga.domain.model.ChatMessageType
import com.jigmnnaga.domain.model.ChatPage
import com.jigmnnaga.domain.model.ChatReadEvent
import com.jigmnnaga.domain.model.ChatResult
import com.jigmnnaga.domain.repository.ChatRealtime
import com.jigmnnaga.domain.repository.ChatRepository
import com.jigmnnaga.domain.repository.MyMemberIdProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeRepository : ChatRepository {
        val queued = ArrayDeque<ChatResult<ChatPage>>()
        var fallback: ChatResult<ChatPage> = ChatResult.Success(ChatPage(emptyList(), null, null, false))
        val loadCalls = mutableListOf<Pair<Long?, Int>>()
        val readCalls = mutableListOf<Long>()
        var readResult: ChatResult<Unit> = ChatResult.Success(Unit)

        override suspend fun getMessages(appointmentId: Long, cursor: Long?, size: Int): ChatResult<ChatPage> {
            loadCalls += cursor to size
            return queued.removeFirstOrNull() ?: fallback
        }

        override suspend fun markAsRead(appointmentId: Long, lastReadMessageId: Long): ChatResult<Unit> {
            readCalls += lastReadMessageId
            return readResult
        }
    }

    private class FakeRealtime : ChatRealtime {
        val connected = MutableStateFlow(false)
        override val isConnected: Flow<Boolean> = connected
        val incoming = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 16)
        val reads = MutableSharedFlow<ChatReadEvent>(extraBufferCapacity = 16)
        var connectCalls = 0
        var disconnectCalls = 0
        var acceptSend = true
        val sentMessages = mutableListOf<String>()
        val sentReads = mutableListOf<Long>()

        override fun connect() { connectCalls++ }
        override fun disconnect() { disconnectCalls++ }
        override fun messages(appointmentId: Long): Flow<ChatMessage> = incoming
        override fun readEvents(appointmentId: Long): Flow<ChatReadEvent> = reads

        override fun sendMessage(appointmentId: Long, content: String): Boolean {
            if (!acceptSend) return false
            sentMessages += content
            return true
        }

        override fun sendRead(appointmentId: Long, lastReadMessageId: Long): Boolean {
            if (!acceptSend) return false
            sentReads += lastReadMessageId
            return true
        }
    }

    private val me = 1L
    private val repository = FakeRepository()
    private val realtime = FakeRealtime()

    private fun msg(id: Long, sender: Long? = 2L, unread: Int? = 2, type: ChatMessageType = ChatMessageType.TEXT) =
        ChatMessage(
            id = id,
            type = type,
            senderId = sender,
            senderNickname = "닉$sender",
            content = "내용$id",
            sentAt = "2026-11-14T13:0${id % 10}:00",
            unreadCount = unread
        )

    private fun page(vararg ids: Long, last: Long? = null, cursor: Long? = null, hasNext: Boolean = false) =
        ChatResult.Success(ChatPage(ids.map { msg(it) }, last, cursor, hasNext))

    private fun newViewModel() = AppointmentChatViewModel(
        appointmentId = 9L,
        repository = repository,
        realtime = realtime,
        memberIdProvider = MyMemberIdProvider { me },
        pageSize = 30
    )

    @Test
    fun initialLoad_showsMessagesOldestFirstWithMineAndSenderName() = runTest(dispatcher) {
        repository.queued += ChatResult.Success(
            ChatPage(listOf(msg(1, sender = 2), msg(2, sender = me)), null, 1L, true)
        )
        val viewModel = newViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasMore)
        assertEquals(listOf("1", "2"), state.messages.map { it.id })
        assertFalse(state.messages[0].isMine)
        assertEquals("닉2", state.messages[0].senderName)
        assertTrue(state.messages[1].isMine)
        assertNull(state.messages[1].senderName)
        assertEquals("내용1", state.messages[0].body)
        assertEquals("오후 1:01", state.messages[0].timeLabel)
        assertEquals(2, state.messages[0].unreadCount)
        assertEquals(listOf<Long?>(null), repository.loadCalls.map { it.first })
        assertEquals(30, repository.loadCalls.single().second)
    }

    @Test
    fun systemMessage_isMarkedSystemWithoutSenderName() = runTest(dispatcher) {
        repository.queued += ChatResult.Success(
            ChatPage(listOf(msg(1, sender = null, unread = null, type = ChatMessageType.SYSTEM)), null, null, false)
        )
        val viewModel = newViewModel()
        runCurrent()

        val row = viewModel.uiState.value.messages.single()
        assertTrue(row.isSystem)
        assertFalse(row.isMine)
        assertNull(row.senderName)
        assertEquals(0, row.unreadCount)
    }

    @Test
    fun initialLoadFailure_notifiesAndStopsLoading() = runTest(dispatcher) {
        repository.queued += ChatResult.Failure(retryable = true)
        val viewModel = newViewModel()

        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(ChatNotice.LOAD_FAILED, viewModel.notice.value)
        viewModel.clearNotice()
        assertNull(viewModel.notice.value)
    }

    @Test
    fun loadFailures_mapToLoginAndParticipantNotices() = runTest(dispatcher) {
        repository.queued += ChatResult.LoginRequired
        val first = newViewModel()
        runCurrent()
        assertEquals(ChatNotice.LOGIN_REQUIRED, first.notice.value)

        repository.queued += ChatResult.NotParticipant
        val second = newViewModel()
        runCurrent()
        assertEquals(ChatNotice.NOT_PARTICIPANT, second.notice.value)
    }

    @Test
    fun connectsOnCreateAndDisconnectsWhenCleared() {
        val store = ViewModelStore()
        @Suppress("UNCHECKED_CAST")
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = newViewModel() as T
        })[AppointmentChatViewModel::class.java]
        assertEquals(1, realtime.connectCalls)
        assertEquals(0, realtime.disconnectCalls)

        store.clear()

        assertEquals(1, realtime.disconnectCalls)
    }

    @Test
    fun loadMore_usesCursorAndPrependsOlderMessages() = runTest(dispatcher) {
        repository.queued += page(5, 6, cursor = 5L, hasNext = true)
        repository.queued += page(3, 4, cursor = null, hasNext = false)
        val viewModel = newViewModel()
        runCurrent()

        viewModel.loadMore()
        assertTrue(viewModel.uiState.value.isLoadingMore)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingMore)
        assertFalse(state.hasMore)
        assertEquals(listOf("3", "4", "5", "6"), state.messages.map { it.id })
        assertEquals(5L, repository.loadCalls[1].first)

        viewModel.loadMore()
        runCurrent()
        assertEquals(2, repository.loadCalls.size)
    }

    @Test
    fun loadMore_failureKeepsMessagesAndCursorForRetry() = runTest(dispatcher) {
        repository.queued += page(5, 6, cursor = 5L, hasNext = true)
        repository.queued += ChatResult.Failure(retryable = true)
        repository.queued += page(3, 4)
        val viewModel = newViewModel()
        runCurrent()

        viewModel.loadMore()
        runCurrent()
        assertEquals(ChatNotice.LOAD_FAILED, viewModel.notice.value)
        assertEquals(listOf("5", "6"), viewModel.uiState.value.messages.map { it.id })
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.loadMore()
        runCurrent()
        assertEquals(listOf("3", "4", "5", "6"), viewModel.uiState.value.messages.map { it.id })
    }

    @Test
    fun receivedMessage_isAppendedAndDuplicatesAreReplaced() = runTest(dispatcher) {
        repository.queued += page(1)
        val viewModel = newViewModel()
        runCurrent()

        realtime.incoming.tryEmit(msg(2))
        realtime.incoming.tryEmit(msg(2, unread = 0))
        runCurrent()

        val messages = viewModel.uiState.value.messages
        assertEquals(listOf("1", "2"), messages.map { it.id })
        assertEquals(0, messages[1].unreadCount)
    }

    @Test
    fun readEvent_decrementsUnreadForOthersMessagesInRange() = runTest(dispatcher) {
        repository.queued += ChatResult.Success(
            ChatPage(
                listOf(
                    msg(1, sender = 2, unread = 2),
                    msg(2, sender = 3, unread = 1),
                    msg(3, sender = 3, unread = 2),
                    msg(4, sender = 2, unread = 2)
                ),
                null, null, false
            )
        )
        val viewModel = newViewModel()
        runCurrent()

        // 멤버 3이 1~3번을 읽었다. 2·3번은 3이 직접 쓴 메시지라 줄지 않는다. 4번은 범위 밖이다.
        realtime.reads.tryEmit(ChatReadEvent(memberId = 3L, previousLastReadMessageId = null, lastReadMessageId = 3L))
        runCurrent()

        assertEquals(listOf(1, 1, 2, 2), viewModel.uiState.value.messages.map { it.unreadCount })

        // 이전 읽음 위치(3) 다음부터만 센다.
        realtime.reads.tryEmit(ChatReadEvent(memberId = 3L, previousLastReadMessageId = 3L, lastReadMessageId = 4L))
        runCurrent()
        assertEquals(listOf(1, 1, 2, 1), viewModel.uiState.value.messages.map { it.unreadCount })
    }

    @Test
    fun readEvent_neverGoesBelowZero() = runTest(dispatcher) {
        repository.queued += ChatResult.Success(ChatPage(listOf(msg(1, sender = 2, unread = 0)), null, null, false))
        val viewModel = newViewModel()
        runCurrent()

        realtime.reads.tryEmit(ChatReadEvent(3L, null, 1L))
        runCurrent()

        assertEquals(0, viewModel.uiState.value.messages.single().unreadCount)
    }

    @Test
    fun whileReading_latestMessageIsSentAsReadOverRealtime() = runTest(dispatcher) {
        repository.queued += page(1, 2, last = 1L)
        realtime.connected.value = true
        val viewModel = newViewModel()
        viewModel.setReading(true)
        runCurrent()

        assertEquals(listOf(2L), realtime.sentReads)
        assertTrue(repository.readCalls.isEmpty())

        realtime.incoming.tryEmit(msg(3))
        runCurrent()
        assertEquals(listOf(2L, 3L), realtime.sentReads)
    }

    @Test
    fun notReading_doesNotMarkReadUntilReadingStarts() = runTest(dispatcher) {
        repository.queued += page(1, 2)
        val viewModel = newViewModel()
        runCurrent()
        realtime.incoming.tryEmit(msg(3))
        runCurrent()
        assertTrue(realtime.sentReads.isEmpty())
        assertTrue(repository.readCalls.isEmpty())

        viewModel.setReading(true)

        assertEquals(listOf(3L), realtime.sentReads)
    }

    @Test
    fun alreadyReadMessages_areNotSentAgain() = runTest(dispatcher) {
        repository.queued += page(1, 2, last = 2L)
        val viewModel = newViewModel()
        viewModel.setReading(true)
        runCurrent()

        assertTrue(realtime.sentReads.isEmpty())
        assertTrue(repository.readCalls.isEmpty())
    }

    @Test
    fun withoutRealtimeConnection_readIsSentOverRestAndRetriedAfterFailure() = runTest(dispatcher) {
        realtime.acceptSend = false
        repository.readResult = ChatResult.Failure(retryable = true)
        repository.queued += page(1, 2)
        val viewModel = newViewModel()
        viewModel.setReading(true)
        runCurrent()
        assertEquals(listOf(2L), repository.readCalls)

        realtime.incoming.tryEmit(msg(2, unread = 1))
        runCurrent()
        assertEquals(listOf(2L, 2L), repository.readCalls)

        repository.readResult = ChatResult.Success(Unit)
        realtime.incoming.tryEmit(msg(2, unread = 0))
        runCurrent()
        assertEquals(listOf(2L, 2L, 2L), repository.readCalls)

        realtime.incoming.tryEmit(msg(2, unread = 0))
        runCurrent()
        assertEquals(3, repository.readCalls.size)
    }

    @Test
    fun sendMessage_blankIsIgnored_sentIsTrimmed() = runTest(dispatcher) {
        val viewModel = newViewModel()
        runCurrent()

        assertFalse(viewModel.sendMessage("   "))
        assertTrue(realtime.sentMessages.isEmpty())
        assertNull(viewModel.notice.value)

        assertTrue(viewModel.sendMessage("  안녕  "))
        assertEquals(listOf("안녕"), realtime.sentMessages)
    }

    @Test
    fun sendMessage_failureKeepsDraftAndNotifies() = runTest(dispatcher) {
        realtime.acceptSend = false
        val viewModel = newViewModel()
        runCurrent()

        assertFalse(viewModel.sendMessage("안녕"))

        assertEquals(ChatNotice.SEND_FAILED, viewModel.notice.value)
    }

    @Test
    fun sendMessage_tooLongIsRejected() = runTest(dispatcher) {
        val viewModel = newViewModel()
        runCurrent()

        assertFalse(viewModel.sendMessage("가".repeat(CHAT_MESSAGE_MAX_LENGTH + 1)))
        assertEquals(ChatNotice.MESSAGE_TOO_LONG, viewModel.notice.value)
        assertTrue(viewModel.sendMessage("가".repeat(CHAT_MESSAGE_MAX_LENGTH)))
    }

    @Test
    fun reconnect_refetchesLatestAndMergesMissedMessages() = runTest(dispatcher) {
        repository.queued += page(1, 2)
        val viewModel = newViewModel()
        runCurrent()

        repository.queued += page(2, 3, 4)
        realtime.connected.value = true
        runCurrent()

        assertEquals(listOf("1", "2", "3", "4"), viewModel.uiState.value.messages.map { it.id })
        assertEquals(2, repository.loadCalls.size)
        assertNull(repository.loadCalls[1].first)
    }

    @Test
    fun reconnect_withGap_restartsFromLatestPage() = runTest(dispatcher) {
        repository.queued += page(1, 2)
        val viewModel = newViewModel()
        runCurrent()

        repository.queued += page(50, 51, cursor = 50L, hasNext = true)
        realtime.connected.value = true
        runCurrent()

        assertEquals(listOf("50", "51"), viewModel.uiState.value.messages.map { it.id })
        assertTrue(viewModel.uiState.value.hasMore)

        repository.queued += page(48, 49)
        viewModel.loadMore()
        runCurrent()
        assertEquals(50L, repository.loadCalls.last().first)
    }
}
