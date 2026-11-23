package com.jigmnnaga.presentation.chat

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** 채팅 ViewModel이 없을 때(화면 미리보기·테스트) 쓰는 빈 흐름. */
object NoChat {
    val state = MutableStateFlow(AppointmentChatUiState(isLoading = false))
    val notice = MutableStateFlow<ChatNotice?>(null)
}

/** 채팅 탭이 보이고 앱이 앞에 있는 동안만 메시지를 읽음 처리하도록 [viewModel]에 알린다. */
@Composable
fun ChatReadingEffect(viewModel: AppointmentChatViewModel?, chatTabSelected: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel, chatTabSelected, resumed) {
        viewModel?.setReading(chatTabSelected && resumed)
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel?.setReading(false) }
    }
}

/**
 * 메시지 목록의 스크롤을 맡는다.
 * - 처음 불러오면 가장 아래(최근 메시지)로 간다.
 * - 새 메시지가 오면 이미 아래쪽을 보고 있거나 내가 보낸 메시지일 때만 아래로 간다.
 * - 위쪽 끝에 가까워지면 [onLoadMore]를 부른다.
 * 목록은 메시지 외의 항목(안내 문구 등)이 앞에 올 수 있어 [leadingItemCount]로 보정한다.
 */
@Composable
fun ChatListEffects(
    listState: LazyListState,
    messages: List<ChatMessageUiState>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    leadingItemCount: Int = 0
) {
    val lastId = messages.lastOrNull()?.id
    val lastIsMine = messages.lastOrNull()?.isMine == true
    var previousLastId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastId) {
        if (lastId == null) return@LaunchedEffect
        val lastIndex = leadingItemCount + messages.lastIndex
        val firstLoad = previousLastId == null
        val layout = listState.layoutInfo
        val nearBottom = (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) >= layout.totalItemsCount - 2
        if (firstLoad) {
            listState.scrollToItem(lastIndex)
        } else if (nearBottom || lastIsMine) {
            listState.animateScrollToItem(lastIndex)
        }
        previousLastId = lastId
    }

    // 키보드가 올라와 목록 높이가 줄어들 때, 줄어들기 전에 맨 아래를 보고 있었다면 가장 아래 메시지가 계속 보이게 한다.
    val latestMessageCount by rememberUpdatedState(messages.size)
    LaunchedEffect(listState) {
        var previousHeight = 0
        var wasAtBottom = true
        snapshotFlow { listState.layoutInfo.viewportSize.height to !listState.canScrollForward }
            .collect { (height, atBottom) ->
                val shrank = previousHeight > 0 && height in 1 until previousHeight
                if (shrank && wasAtBottom && latestMessageCount > 0) {
                    listState.scrollToItem(leadingItemCount + latestMessageCount - 1)
                    listState.scrollBy(Float.MAX_VALUE)
                }
                previousHeight = height
                wasAtBottom = atBottom
            }
    }

    LaunchedEffect(listState, hasMore, messages.size) {
        if (!hasMore) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex <= 2 && listState.layoutInfo.totalItemsCount > 0 }
            .distinctUntilChanged()
            .collect { nearTop -> if (nearTop) onLoadMore() }
    }
}
