package com.jigmnnaga.presentation.appointment.placevote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R

/**
 * 장소 검색·투표 화면과 [PlaceVoteViewModel]을 연결한다.
 * 저장·제출 결과와 최대 선택 안내를 스낵바로 보여 준다.
 * 방장이 후보 장소를 저장하면 [onSaved]를 불러 약속방으로 돌아간다.
 */
@Composable
fun PlaceVoteRoute(
    viewModel: PlaceVoteViewModel,
    onBack: () -> Unit,
    onLoginRequired: (resumeAfterLogin: () -> Unit) -> Unit = {},
    onSaved: () -> Unit = {},
    modifier: Modifier = Modifier,
    maxSelection: Int = PlaceVoteViewModel.DEFAULT_MAX_SELECTION
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        val message = when (notice) {
            PlaceVoteNotice.SUBMIT_API_NOT_CONNECTED -> context.getString(R.string.place_vote_api_unavailable)
            PlaceVoteNotice.MAX_SELECTION_REACHED ->
                context.getString(R.string.place_vote_max_selection, maxSelection)
            PlaceVoteNotice.SEARCH_LOGIN_REQUIRED -> {
                viewModel.clearNotice()
                onLoginRequired(viewModel::retrySearch)
                null
            }
            PlaceVoteNotice.SEARCH_FAILED_RETRYABLE -> context.getString(R.string.place_vote_search_failed_retry)
            PlaceVoteNotice.SEARCH_FAILED -> context.getString(R.string.place_vote_search_failed)
            PlaceVoteNotice.SAVED -> {
                viewModel.clearNotice()
                onSaved()
                null
            }
            PlaceVoteNotice.SUBMITTED -> context.getString(R.string.place_vote_submitted)
            PlaceVoteNotice.ACTION_LOGIN_REQUIRED -> {
                viewModel.clearNotice()
                onLoginRequired(viewModel::retryAfterLogin)
                null
            }
            PlaceVoteNotice.ACTION_CLOSED -> context.getString(R.string.place_vote_action_closed)
            PlaceVoteNotice.ACTION_INVALID_PLACE -> context.getString(R.string.place_vote_action_invalid_place)
            PlaceVoteNotice.ACTION_NOT_ALLOWED -> context.getString(R.string.place_vote_action_not_allowed)
            PlaceVoteNotice.ACTION_FAILED_RETRYABLE -> context.getString(R.string.place_vote_action_failed_retry)
            PlaceVoteNotice.ACTION_FAILED -> context.getString(R.string.place_vote_action_failed)
            PlaceVoteNotice.VOTES_LOAD_FAILED -> context.getString(R.string.place_vote_votes_load_failed)
            null -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearNotice()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlaceVoteScreen(
            state = state,
            onBackClick = onBack,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onCandidateSelectionChange = viewModel::onSelectionChange,
            onSubmitSelection = viewModel::onSubmit
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 80.dp)
        )
    }
}
