
package com.jigmnnaga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JigmnnagaTheme {
                Text(text = "지금 나가")
            }
        }
    }
}