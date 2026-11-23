package com.jigmnnaga.presentation.mypage.help

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 도움말 및 문의 화면 네비게이션 목적지 타입. */
@Serializable
data object HelpSupportRoute

/**
 * 도움말 및 문의 화면 Route Composable.
 *
 * ViewModel 연동 전까지는 로컬 임시 상태로 FAQ 접기/펼치기 UI를 확인한다.
 * 추후 문의하기 기능 연동 시 ViewModel을 추가하면 된다.
 */
@Composable
fun HelpSupportRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val faqCount = 4
    var expandedStates by remember { mutableStateOf(List(faqCount) { false }) }

    HelpSupportScreen(
        expandedStates = expandedStates,
        onBackClick = onBackClick,
        onItemToggle = { index ->
            expandedStates = expandedStates.toMutableList().also { it[index] = !it[index] }
        },
        onInquiryClick = { /* TODO: 문의하기 기능 연동 */ },
        modifier = modifier
    )
}
