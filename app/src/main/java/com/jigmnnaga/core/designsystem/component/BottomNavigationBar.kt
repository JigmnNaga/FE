package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

enum class BottomNavigationTab {
    NONE,
    HOME,
    SCHEDULE
}

// 네비게이션 바 inset은 부모 Screen 담당. 이 컴포넌트는 80dp 바 영역만 담당.
@Composable
fun BottomNavigationBar(
    selectedTab: BottomNavigationTab,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.BottomNavigationHeight)
            .background(AppColors.MainGreen.Green200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.BottomNavigationHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavigationItem(
                label = stringResource(R.string.bottom_navigation_home),
                icon = R.drawable.home,
                selected = selectedTab == BottomNavigationTab.HOME,
                onClick = onHomeClick
            )
            BottomNavigationItem(
                label = stringResource(R.string.bottom_navigation_schedule),
                icon = R.drawable.calendar_gray,
                preserveIconColors = true,
                selected = selectedTab == BottomNavigationTab.SCHEDULE,
                onClick = onScheduleClick
            )
        }

        CircleButton(
            contentDescription = stringResource(R.string.add_appointment_description),
            onClick = onAddClick,
            width = AppDimens.CircleButtonLarge,
            icon = painterResource(R.drawable.plus),
            iconSize = AppDimens.IconLarge,
            backgroundColor = AppColors.MainGreen.Green900,
            contentColor = AppColors.MainGreen.Green200,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// 기존 HomeScreen 호환용 오버로드. 신규 코드는 enum API 사용.
@Deprecated("Use the BottomNavigationTab overload instead.")
@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tab = when (selectedTab.lowercase()) {
        "home" -> BottomNavigationTab.HOME
        "schedule" -> BottomNavigationTab.SCHEDULE
        else -> BottomNavigationTab.NONE
    }
    BottomNavigationBar(tab, onHomeClick, onAddClick, onScheduleClick, modifier)
}

@Composable
private fun BottomNavigationItem(
    label: String,
    icon: Int,
    preserveIconColors: Boolean = false,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) AppColors.Brown.Brown950 else AppColors.Monotone.Gray400
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(AppDimens.BottomNavigationIconSize),
            colorFilter = if (preserveIconColors && !selected) null else
                androidx.compose.ui.graphics.ColorFilter.tint(tint)
        )
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(AppDimens.BottomNavigationLabelGap)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun BottomNavigationBarPreview() {
    JigmnnagaTheme {
        Column {
            BottomNavigationBar(
                selectedTab = BottomNavigationTab.NONE,
                onHomeClick = {}, onAddClick = {}, onScheduleClick = {}
            )
            BottomNavigationBar(
                selectedTab = BottomNavigationTab.HOME,
                onHomeClick = {}, onAddClick = {}, onScheduleClick = {}
            )
            BottomNavigationBar(
                selectedTab = BottomNavigationTab.SCHEDULE,
                onHomeClick = {}, onAddClick = {}, onScheduleClick = {}
            )
        }
    }
}
