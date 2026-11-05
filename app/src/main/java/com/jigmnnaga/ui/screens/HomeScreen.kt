
package com.jigmnnaga.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jigmnnaga.ui.components.BottomNavigationBar
import com.jigmnnaga.ui.components.MainHeader

private val MainGreen200 = Color(0xFFF5F5E9)

@Composable
fun HomeScreen(
    onAddClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var selectedTab by remember {
        mutableStateOf("home")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainGreen200),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MainHeader(
            onNotificationClick = onNotificationClick,
            onProfileClick = onProfileClick,
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp
            )
        )

        BottomNavigationBar(
            selectedTab = selectedTab,
            onHomeClick = {
                selectedTab = "home"
            },
            onAddClick = onAddClick,
            onScheduleClick = {
                selectedTab = "schedule"
                onScheduleClick()
            }
        )
    }
}
