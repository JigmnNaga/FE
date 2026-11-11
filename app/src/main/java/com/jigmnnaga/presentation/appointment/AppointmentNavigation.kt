package com.jigmnnaga.presentation.appointment

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateRoute
import kotlinx.serialization.Serializable

/** 약속 생성 화면(APT-001). */
@Serializable
data object AppointmentCreateDestination

/**
 * 초대 수락 화면(APT-002). 초대 링크의 코드를 인자로 받는다.
 * 화면 연결은 초대 API 연동(#49)에서 이 그래프에 추가한다.
 */
@Serializable
data class AppointmentInviteDestination(val inviteCode: String)

/** 약속 기능이 소유하는 이동 경로 그래프. 앱 호스트에는 이 객체만 등록한다. */
object AppointmentNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<AppointmentCreateDestination> {
            AppointmentCreateRoute(onExit = { navigator.navigateBack() })
        }
    }
}