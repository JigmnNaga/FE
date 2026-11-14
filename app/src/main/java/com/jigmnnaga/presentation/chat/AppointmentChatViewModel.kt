package com.jigmnnaga.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.ChatMessage
import com.jigmnnaga.domain.model.ChatMessageType
import com.jigmnnaga.domain.model.ChatPage
import com.jigmnnaga.domain.model.ChatReadEvent
import com.jigmnnaga.domain.model.ChatResult
import com.jigmnnaga.domain.repository.ChatRealtime
import com.jigmnnaga.domain.repository.ChatRepository
import com.jigmnnaga.domain.repository.MyMemberIdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** 채팅 중 사용자에게 한 번 알려야 하는 안내. */
enum class ChatNotice {
    LOGIN_REQUIRED,
    NOT_PARTICIPANT,
    LOAD_FAILED,
    SEND_FAILED,
    MESSAGE_TOO_LONG
}

data class AppointmentChatUiState(
    val messages: List<ChatMessageUiState> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false
)

/** 서버가 받아 주는 메시지 최대 길이. */
const val CHAT_MESSAGE_MAX_LENGTH = 1000

/**
 * 약속방 하나의 채팅을 맡는다. 이전 메시지를 페이지로 불러오고, 실시간으로 받은 메시지와 읽음 알림을 합치고,
 * 화면에서 채팅이 보이는 동안 마지막 메시지를 읽음 처리한다.
 * 방장·참여자 약속방이 함께 쓰며, 약속 정보가 다시 불러와져도 이 ViewModel은 그대로 유지된다.
 */
class AppointmentChatViewModel(
    private val appointmentId: Long,
    private val repository: ChatRepository,
    private val realtime: ChatRealtime,
    private val memberIdProvider: MyMemberIdProvider,
    private val pageSize: Int = ChatRepository.DEFAULT_PAGE_SIZE
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentChatUiState())
    val uiState: StateFlow<AppointmentChatUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<ChatNotice?>(null)
    val notice: StateFlow<ChatNotice?> = _notice.asStateFlow()

    private var messages: List<ChatMessage> = emptyList()
    private var myMemberId: Long? = null
    private var nextCursor: Long? = null
    private var hasMore = false
    private var isLoading = true
    private var isLoadingMore = false
    private var lastReadSent = 0L
    private var reading = false

    init {
        realtime.connect()
        viewModelScope.launch {
            realtime.messages(appointmentId).collect { onMessageReceived(it) }
        }
        viewModelScope.launch {
            realtime.readEvents(appointmentId).collect { onReadEvent(it) }
        }
        viewModelScope.launch {
            // 연결될 때마다(끊겼다 다시 연결될 때 포함) 그 사이 놓친 메시지를 다시 받아 온다.
            realtime.isConnected.distinctUntilChanged().collect { connected ->
                if (connected) refreshLatest()
            }
        }
        viewModelScope.launch { loadInitial() }
    }

    /** 채팅 탭이 화면에 보이고 앱이 앞에 있을 때만 true로 둔다. true인 동안 새 메시지를 읽음 처리한다. */
    fun setReading(isReading: Boolean) {
        if (reading == isReading) return
        reading = isReading
        if (reading) markLatestAsRead()
    }

    /** [content]를 보낸다. 서버가 받아 주면 true이고, 보낸 메시지는 실시간으로 되돌아와 목록에 붙는다. */
    fun sendMessage(content: String): Boolean {
        val text = content.trim()
        if (text.isEmpty()) return false
        if (text.length > CHAT_MESSAGE_MAX_LENGTH) {
            _notice.value = ChatNotice.MESSAGE_TOO_LONG
            return false
        }
        val sent = realtime.sendMessage(appointmentId, text)
        if (!sent) _notice.value = ChatNotice.SEND_FAILED
        return sent
    }

    /** 위로 스크롤해 더 이전 메시지가 필요할 때 부른다. */
    fun loadMore() {
        val cursor = nextCursor ?: return
        if (isLoading || isLoadingMore) return
        isLoadingMore = true
        publish()
        viewModelScope.launch {
            when (val result = repository.getMessages(appointmentId, cursor, pageSize)) {
                is ChatResult.Success -> {
                    messages = mergeMessages(messages, result.value.messages)
                    nextCursor = result.value.nextCursor
                    hasMore = result.value.hasNext
                }
                else -> notifyFailure(result)
            }
            isLoadingMore = false
            publish()
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    override fun onCleared() {
        realtime.disconnect()
    }

    private suspend fun loadInitial() {
        myMemberId = memberIdProvider.myMemberId()
        when (val result = repository.getMessages(appointmentId, null, pageSize)) {
            is ChatResult.Success -> {
                val page = result.value
                messages = mergeMessages(messages, page.messages)
                nextCursor = page.nextCursor
                hasMore = page.hasNext
                lastReadSent = maxOf(lastReadSent, page.myLastReadMessageId ?: 0L)
            }
            else -> notifyFailure(result)
        }
        isLoading = false
        publish()
        markLatestAsRead()
    }

    private suspend fun refreshLatest() {
        val result = repository.getMessages(appointmentId, null, pageSize)
        if (result !is ChatResult.Success) {
            if (result is ChatResult.LoginRequired || result is ChatResult.NotParticipant) notifyFailure(result)
            return
        }
        val page: ChatPage = result.value
        val newestKnown = messages.maxOfOrNull { it.id }
        val oldestFetched = page.messages.firstOrNull()?.id
        messages = if (newestKnown != null && oldestFetched != null && page.hasNext && oldestFetched > newestKnown) {
            // 연결이 오래 끊겨 최근 페이지와 기존 목록 사이에 빈 구간이 생겼다. 최근 페이지로 새로 시작한다.
            nextCursor = page.nextCursor
            hasMore = true
            page.messages
        } else {
            mergeMessages(messages, page.messages)
        }
        isLoading = false
        publish()
        markLatestAsRead()
    }

    private fun onMessageReceived(message: ChatMessage) {
        messages = mergeMessages(messages, listOf(message))
        publish()
        markLatestAsRead()
    }

    private fun onReadEvent(event: ChatReadEvent) {
        val from = event.previousLastReadMessageId ?: 0L
        messages = messages.map { message ->
            val unread = message.unreadCount
            val readNow = message.type == ChatMessageType.TEXT &&
                message.id > from && message.id <= event.lastReadMessageId &&
                message.senderId != event.memberId && unread != null && unread > 0
            if (readNow) message.copy(unreadCount = unread!! - 1) else message
        }
        publish()
    }

    private fun markLatestAsRead() {
        if (!reading) return
        val latest = messages.maxOfOrNull { it.id } ?: return
        if (latest <= lastReadSent) return
        val previous = lastReadSent
        lastReadSent = latest
        if (realtime.sendRead(appointmentId, latest)) return
        // 실시간 연결이 없으면 REST로 알린다. 실패하면 다음 기회에 다시 보내도록 되돌린다.
        viewModelScope.launch {
            val result = repository.markAsRead(appointmentId, latest)
            if (result !is ChatResult.Success && lastReadSent == latest) lastReadSent = previous
        }
    }

    private fun notifyFailure(result: ChatResult<*>) {
        _notice.value = when (result) {
            is ChatResult.LoginRequired -> ChatNotice.LOGIN_REQUIRED
            is ChatResult.NotParticipant -> ChatNotice.NOT_PARTICIPANT
            else -> ChatNotice.LOAD_FAILED
        }
    }

    private fun publish() {
        val me = myMemberId
        _uiState.value = AppointmentChatUiState(
            messages = messages.map { it.toUiState(me) },
            isLoading = isLoading,
            isLoadingMore = isLoadingMore,
            hasMore = hasMore
        )
    }
}

/** 같은 ID는 [incoming] 값으로 바꾸고(안 읽은 사람 수가 더 새롭다), ID 순으로 정렬한다. */
internal fun mergeMessages(existing: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> {
    if (incoming.isEmpty()) return existing
    val byId = LinkedHashMap<Long, ChatMessage>()
    existing.forEach { byId[it.id] = it }
    incoming.forEach { byId[it.id] = it }
    return byId.values.sortedBy { it.id }
}

internal fun ChatMessage.toUiState(myMemberId: Long?): ChatMessageUiState {
    val system = type == ChatMessageType.SYSTEM
    val mine = !system && myMemberId != null && senderId == myMemberId
    return ChatMessageUiState(
        id = id.toString(),
        body = content,
        isMine = mine,
        senderName = if (mine || system) null else senderNickname,
        timeLabel = formatChatTime(sentAt),
        unreadCount = unreadCount ?: 0,
        isSystem = system
    )
}
