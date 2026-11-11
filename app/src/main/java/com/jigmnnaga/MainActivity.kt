package com.jigmnnaga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.composable
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.core.navigation.AppNavHost
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.appointment.AppointmentNavigationGraph
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.common.login.LoginScreen
import com.jigmnnaga.presentation.common.login.terms.PrivacyPolicyRoute
import com.jigmnnaga.presentation.common.login.terms.PrivacyPolicyScreen
import com.jigmnnaga.presentation.common.login.terms.TermsOfServiceRoute
import com.jigmnnaga.presentation.common.login.terms.TermsOfServiceScreen
import com.jigmnnaga.presentation.common.splash.SplashRoute
import com.jigmnnaga.presentation.common.splash.SplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            JigmnnagaTheme {
                AppNavHost(
                    startDestination = SplashRoute
                ) { navigator: AppNavigator ->
                    composable<SplashRoute> {
                        SplashScreen(
                            onStartClick = { navigator.navigate(LoginRoute) }
                        )
                    }
                    composable<LoginRoute> {
                        LoginScreen(
                            onKakaoLoginClick = {
                                // TODO: 카카오 로그인 구현 후 연결
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
                }
            }
        }
    }
}