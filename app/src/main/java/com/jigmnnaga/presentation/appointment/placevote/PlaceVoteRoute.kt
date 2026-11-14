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
 * 서버 미연동 안내와 최대 선택 안내를 스낵바로 보여 준다.
 */
@Composable
fun PlaceVoteRoute(
    viewModel: PlaceVoteViewModel,
    onBack: () -> Unit,
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
            null -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearNotice()
        }
    }

    val summaryTitle = if (viewModel.showsSelectedSummary && state.selectedPlaceNames.isNotEmpty()) {
        context.getString(R.string.place_vote_selected_summary, state.selectedPlaceNames.size)
    } else {
        null
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlaceVoteScreen(
            state = state.copy(selectedSummaryTitle = summaryTitle),
            onBackClick = onBack,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onCandidateSelectionChange = viewModel::onSelectionChange,
            onSubmitSelection = viewModel::onSubmit,
            onRemoveCandidate = if (state.role == PlaceVoteRole.HOST) viewModel::onRemoveCandidate else null
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
