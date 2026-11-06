package com.jigmnnaga.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Figma spacing tokens and measured component dimensions. */
object AppDimens {
    // Figma spacing variables
    val Spacing4 = 4.dp
    val Spacing8 = 8.dp
    val Spacing12 = 12.dp
    val Spacing16 = 16.dp
    val Spacing20 = 20.dp
    val Spacing24 = 24.dp
    val Spacing32 = 32.dp
    val Spacing40 = 40.dp

    // Measured component dimensions from PR review/Figma comparisons
    val StartButtonWidth = 167.dp
    val StartButtonHeight = 56.dp
    val ButtonWidth = 175.dp
    val ButtonHeight = 64.dp
    val ButtonCornerRadius = 16.dp
    val StartButtonArrowSize = 18.dp
    val StartButtonArrowGap = 10.dp

    val MainHeaderHeight = 68.dp
    val MeetingHeaderHeight = 68.dp
    val BackHeaderHeight = 68.dp
    val HeaderHorizontalPadding = 20.dp
    val HeaderActionSize = 38.dp
    val HeaderIconSize = 18.dp
    val HeaderActionGap = 10.dp

    val BottomNavigationHeight = 80.dp
    val BottomNavigationHorizontalPadding = 70.dp
    val BottomNavigationIconSize = 22.dp
    val BottomNavigationLabelGap = 3.dp
    val InputHeight = 50.dp
    val InputHorizontalPadding = 16.dp
    val InputSendGap = 5.dp
    val SendButtonSize = 50.dp
    val SendIconSize = 16.dp
    val LeaveButtonTopPadding = 1.dp
    val LeaveButtonBottomPadding = 15.dp

    // Component variant dimensions not yet verified across all Figma variants.
    // Retained for existing visual catalog examples only.
    val CircleButtonSmall = 28.dp
    val CircleButtonMedium = 38.dp
    val CircleButtonLarge = 56.dp
    val IconSmall = 14.dp
    val IconMedium = 20.dp
    val IconLarge = 24.dp
}
