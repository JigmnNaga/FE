package com.jigmnnaga.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppNotification
import com.jigmnnaga.domain.model.NotificationResult
import com.jigmnnaga.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 화면이 한 번만 처리해야 하는 일. */
sealed interface NotificationEvent {
    data object LoginRequired : NotificationEvent
    data class OpenAppointment(val appointmentId: Long) : NotificationEvent
}

class NotificationViewModel(
    private val repository: NotificationRepository,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private var notifications: List<AppNotification> = emptyList()
    private var nextCursor: Long? = null
    private var hasNext = false
    private var loadStatus = NotificationLoadStatus.LOADING
    private var selectedTab = NotificationTab.INVITE
    private var isLoadingMore = false

    private val _state = MutableStateFlow(NotificationUiState(loadStatus = NotificationLoadStatus.LOADING))
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<NotificationEvent?>(null)
    val event: StateFlow<NotificationEvent?> = _event.asStateFlow()

    /** 첫 페이지부터 다시 불러온다. 화면에 들어올 때와 "다시 시도"에서 쓴다. */
    fun load() {
        loadStatus = NotificationLoadStatus.LOADING
        render()
        viewModelScope.launch {
            when (val result = repository.getNotifications(cursor = null, size = PAGE_SIZE)) {
                is NotificationResult.Success -> {
                    notifications = result.value.items
                    nextCursor = result.value.nextCursor
                    hasNext = result.value.hasNext
                    loadStatus = NotificationLoadStatus.LOADED
                }
                NotificationResult.LoginRequired -> {
                    loadStatus = NotificationLoadStatus.ERROR
                    _event.value = NotificationEvent.LoginRequired
                }
                is NotificationResult.Failure -> loadStatus = NotificationLoadStatus.ERROR
            }
            render()
        }
    }

    /** 목록 끝에 닿으면 다음 페이지를 이어 붙인다. */
    fun loadMore() {
        val cursor = nextCursor
        if (!hasNext || cursor == null || isLoadingMore || loadStatus != NotificationLoadStatus.LOADED) return
        isLoadingMore = true
        render()
        viewModelScope.launch {
            when (val result = repository.getNotifications(cursor = cursor, size = PAGE_SIZE)) {
                is NotificationResult.Success -> {
                    val knownIds = notifications.map { it.id }.toSet()
                    notifications = notifications + result.value.items.filter { it.id !in knownIds }
                    nextCursor = result.value.nextCursor
                    hasNext = result.value.hasNext
                }
                NotificationResult.LoginRequired -> _event.value = NotificationEvent.LoginRequired
                is NotificationResult.Failure -> Unit
            }
            isLoadingMore = false
            render()
        }
    }

    fun onTabSelect(tab: NotificationTab) {
        selectedTab = tab
        render()
    }

    /** 알림을 누르면 읽음 처리를 하고, 약속이 연결된 알림이면 그 약속으로 이동하게 한다. */
    fun onNotificationClick(notificationId: Long) {
        val target = notifications.firstOrNull { it.id == notificationId } ?: return
        if (!target.read) markRead(target)
        target.appointmentId?.let { _event.value = NotificationEvent.OpenAppointment(it) }
    }

    fun consumeEvent() {
        _event.value = null
    }

    private fun markRead(target: AppNotification) {
        updateRead(target.id, read = true)
        viewModelScope.launch {
            when (repository.readNotification(target.id)) {
                is NotificationResult.Success -> Unit
                NotificationResult.LoginRequired -> {
                    updateRead(target.id, read = false)
                    _event.value = NotificationEvent.LoginRequired
                }
                is NotificationResult.Failure -> updateRead(target.id, read = false)
            }
        }
    }

    private fun updateRead(id: Long, read: Boolean) {
        notifications = notifications.map { if (it.id == id) it.copy(read = read) else it }
        render()
    }

    private fun render() {
        val (invites, activities) = notifications.toTabItems(now())
        _state.value = NotificationUiState(
            selectedTab = selectedTab,
            invites = invites,
            activities = activities,
            loadStatus = loadStatus,
            hasNext = hasNext,
            isLoadingMore = isLoadingMore
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
