package com.jigmnnaga.presentation.mypage.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jigmnnaga.R
import com.jigmnnaga.presentation.appointment.manage.ManageConfirmDialog
import kotlinx.serialization.Serializable

/** 기본 위치 설정 화면 네비게이션 목적지 타입. */
@Serializable
data object DefaultLocationRoute

/**
 * 기본 위치 설정 화면과 [DefaultLocationViewModel]을 연결한다.
 * 추가·수정 화면은 출발 위치 선택 화면처럼 이 화면을 덮는 전체 화면으로 띄우고,
 * 삭제는 확인 팝업을 거친다. 로그인이 필요하다는 응답을 받으면 [onLoginRequired]를 한 번 부른다.
 */
@Composable
fun DefaultLocationRoute(
    viewModel: DefaultLocationViewModel,
    onBackClick: () -> Unit,
    onLoginRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loginRequired) {
        if (state.loginRequired) {
            viewModel.consumeLoginRequired()
            onLoginRequired()
        }
    }

    DefaultLocationScreen(
        state = state,
        onBackClick = onBackClick,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick,
        onRetryLoad = viewModel::load,
        modifier = modifier
    )

    state.editor?.let { editor ->
        // 시스템 뒤로 가기는 Dialog가 받아 추가·수정 화면만 닫는다.
        Dialog(
            onDismissRequest = viewModel::onDismissEditor,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DefaultLocationEditorScreen(
                editor = editor,
                onBackClick = viewModel::onDismissEditor,
                onNameChange = viewModel::onNameChange,
                onQueryChange = viewModel::onQueryChange,
                onSelectPlace = viewModel::onSelectPlace,
                onSave = viewModel::onSave
            )
        }
    }

    state.deleteTarget?.let { target ->
        ManageConfirmDialog(
            title = stringResource(R.string.default_location_delete_title),
            message = stringResource(R.string.default_location_delete_message, target.name),
            confirmText = stringResource(R.string.default_location_delete),
            onDismiss = viewModel::onDismissDelete,
            onConfirm = viewModel::onConfirmDelete,
            dialogTag = "default-location-delete-dialog",
            cancelTag = "default-location-delete-cancel",
            confirmTag = "default-location-delete-confirm"
        )
    }
}
