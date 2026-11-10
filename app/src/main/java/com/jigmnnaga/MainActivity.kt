
package com.jigmnnaga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.composable
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.core.navigation.AppNavHost
import com.jigmnnaga.presentation.common.splash.SplashRoute
import com.jigmnnaga.presentation.common.splash.SplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JigmnnagaTheme {
                AppNavHost(
                    startDestination = SplashRoute
                ) { _ ->
                    composable<SplashRoute> {
                        SplashScreen(
                            onStartClick = {
                                // TODO: COM-002 구현 후 화면 이동 연결
                            }
                        )
                    }
                }
            }
        }
    }
}
