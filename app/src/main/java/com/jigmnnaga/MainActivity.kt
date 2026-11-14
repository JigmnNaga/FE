package com.jigmnnaga

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.composable
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.core.navigation.AppNavHost
import com.jigmnnaga.core.navigation.AppNavigationOptions
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.core.navigation.LoginReturnRegistry
import com.jigmnnaga.presentation.appointment.AppointmentInviteDestination
import com.jigmnnaga.presentation.appointment.AppointmentNavigationGraph
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteLinkParser
import com.jigmnnaga.presentation.calendar.CalendarNavigationGraph
import com.jigmnnaga.presentation.home.HomeNavigationGraph
import com.jigmnnaga.presentation.home.HomeRoute
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.common.login.terms.PrivacyPolicyRoute
import com.jigmnnaga.presentation.common.login.terms.PrivacyPolicyScreen
import com.jigmnnaga.presentation.common.login.terms.TermsOfServiceRoute
import com.jigmnnaga.presentation.common.login.terms.TermsOfServiceScreen
import com.jigmnnaga.presentation.common.splash.SplashRoute
import com.jigmnnaga.presentation.common.splash.SplashScreen

class MainActivity : ComponentActivity() {

    /** 초대 링크로 앱이 열렸을 때 꺼낸 초대 코드. 화면 이동을 마치면 비운다. */
    private var pendingInviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInviteCode = extractInviteCode(intent)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            JigmnnagaTheme {
                AppNavHost(
                    startDestination = SplashRoute,
                    navigatorEffects = { navigator ->
                        val inviteCode = pendingInviteCode
                        LaunchedEffect(inviteCode) {
                            if (inviteCode != null) {
                                navigator.navigate(AppointmentInviteDestination(inviteCode))
                                pendingInviteCode = null
                            }
                        }
                    }
                ) { navigator: AppNavigator ->
                    composable<SplashRoute> {
                        SplashScreen(
                            onStartClick = { navigator.navigate(LoginRoute) }
                        )
                    }
                    composable<LoginRoute> {
                        LoginRoute(
                            onLoginSuccess = {
                                // 초대 화면처럼 로그인 후 돌아갈 곳이 있으면 그곳으로 복귀한다.
                                val returnAfterLogin = LoginReturnRegistry.consume()
                                if (returnAfterLogin != null) {
                                    navigator.navigateBack()
                                    returnAfterLogin()
                                } else {
                                    // TODO: 백엔드 로그인 API 연동 완료 후 서버 JWT 저장 로직 추가 필요.
                                    // 현재는 #25 홈 화면 UI 테스트를 위한 임시 Navigation 연결입니다.
                                    navigator.navigate(
                                        destination = HomeRoute,
                                        options = AppNavigationOptions(
                                            popUpTo = SplashRoute,
                                            inclusive = true,
                                            launchSingleTop = true
                                        )
                                    )
                                }
                            },
                            onTermsClick = { navigator.navigate(TermsOfServiceRoute) },
                            onPrivacyClick = { navigator.navigate(PrivacyPolicyRoute) }
                        )
                    }
                    composable<TermsOfServiceRoute> {
                        TermsOfServiceScreen(onBackClick = { navigator.navigateBack() })
                    }
                    composable<PrivacyPolicyRoute> {
                        PrivacyPolicyScreen(onBackClick = { navigator.navigateBack() })
                    }
                    AppointmentNavigationGraph.register(this, navigator)
                    HomeNavigationGraph.register(this, navigator)
                    CalendarNavigationGraph.register(this, navigator)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractInviteCode(intent)?.let { pendingInviteCode = it }
    }

    private fun extractInviteCode(intent: Intent?): String? =
        intent?.data?.toString()?.let(AppointmentInviteLinkParser::extractInviteCode)
}
