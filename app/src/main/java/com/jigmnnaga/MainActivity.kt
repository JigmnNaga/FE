
package com.jigmnnaga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.ui.screens.HomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JigmnnagaTheme {
                HomeScreen(
                    onAddClick = {},
                    onScheduleClick = {},
                    onNotificationClick = {},
                    onProfileClick = {}
                )
            }
        }
    }
}
