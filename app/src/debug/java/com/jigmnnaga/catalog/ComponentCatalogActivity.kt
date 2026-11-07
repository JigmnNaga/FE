package com.jigmnnaga.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.catalog.screens.CircleButtonTestScreen
import com.jigmnnaga.catalog.screens.ComponentTestScreen
import com.jigmnnaga.catalog.screens.DesignSystemTestScreen
import com.jigmnnaga.catalog.screens.HeaderTestScreen

class ComponentCatalogActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(247, 249, 237)
        window.navigationBarColor = android.graphics.Color.rgb(247, 249, 237)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            JigmnnagaTheme {
                ComponentCatalogHost()
            }
        }
    }
}

private enum class CatalogPage(val label: String) {
    DESIGN_SYSTEM("Tokens"),
    COMPONENTS("Buttons & bars"),
    CIRCLE_BUTTONS("Circle buttons"),
    HEADERS("Headers")
}

@Composable
private fun ComponentCatalogHost() {
    var selectedPage by remember { mutableStateOf(CatalogPage.COMPONENTS) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CatalogPage.entries.forEach { page ->
                TextButton(onClick = { selectedPage = page }) {
                    Text(
                        text = page.label,
                        color = if (selectedPage == page) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        Surface(modifier = Modifier.weight(1f)) {
            Box(Modifier.fillMaxSize()) {
                when (selectedPage) {
                    CatalogPage.DESIGN_SYSTEM -> DesignSystemTestScreen()
                    CatalogPage.COMPONENTS -> ComponentTestScreen()
                    CatalogPage.CIRCLE_BUTTONS -> CircleButtonTestScreen()
                    CatalogPage.HEADERS -> HeaderTestScreen()
                }
            }
        }
    }
}
