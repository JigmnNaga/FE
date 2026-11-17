package com.jigmnnaga.presentation.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

@Composable
fun ProfileEditScreen(
    state: ProfileEditUiState,
    onBackClick: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPhotoSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // ── 메인 화면 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.MainGreen.Green200)
        ) {
            Spacer(
                modifier = Modifier
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .fillMaxWidth()
                    .background(AppColors.MainGreen.Green200)
            )
            BackHeader(
                title = "프로필 편집",
                onBackClick = onBackClick
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.Spacing20, vertical = 16.dp)
            ) {
                ProfileImageSection(
                    profileInitial = state.profileInitial,
                    onPhotoChangeClick = { showPhotoSheet = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(24.dp))
                NicknameInputSection(
                    nickname = state.nickname,
                    onNicknameChange = onNicknameChange
                )
                Spacer(modifier = Modifier.height(18.dp))
                ProfileInfoSection(
                    joinDate = state.joinDate,
                    totalAppointments = state.totalAppointments
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.MainGreen.Green200)
                    .navigationBarsPadding()
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(bottom = 20.dp)
            ) {
                AppButton(
                    text = "저장하기",
                    onClick = onSaveClick,
                    enabled = state.isSaveEnabled
                )
            }
        }

        // ── 사진 변경 바텀시트 오버레이 ──
        if (showPhotoSheet) {
            // 딤 배경
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x541C211D))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showPhotoSheet = false }
            )
            // 시트 패널
            PhotoChangeBottomSheet(
                onDismiss = { showPhotoSheet = false },
                onSelectFromLibrary = { showPhotoSheet = false },
                onTakePhoto = { showPhotoSheet = false },
                onDeletePhoto = { showPhotoSheet = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// 프로필 이미지 섹션 (아바타 + 힌트 텍스트 + 카메라 배지)
// ─────────────────────────────────────────────────────────
@Composable
private fun ProfileImageSection(
    profileInitial: String?,
    onPhotoChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Figma: 프로필 사진 영역 (flex-col, gap=10, items-center, relative)
    // 카메라 배지는 이 영역의 bottom:26, right:26에 absolute 배치됨
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 아바타 원형 (별도 clip — 배지는 여기 밖에 있음)
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(AppColors.MainGreen.Green700)
                    .border(width = 2.dp, color = AppColors.MainGreen.Green400, shape = CircleShape)
                    .clickable(onClick = onPhotoChangeClick),
                contentAlignment = Alignment.Center
            ) {
                if (profileInitial != null) {
                    Text(
                        text = profileInitial,
                        color = AppColors.MainGreen.Green200,
                        fontSize = 28.16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = null,
                        tint = AppColors.MainGreen.Green200,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            // 힌트 텍스트
            Text(
                text = "사진을 눌러 변경할 수 있어요",
                color = AppColors.Monotone.Gray500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 카메라 배지: align(BottomEnd) → offset(-26,-26) → bottom:26dp, right:26dp
        // 아바타의 clip Box 외부 레이어에 위치 → 원형 clip에 잘리지 않음
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-26).dp, y = (-26).dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(AppColors.MainGreen.Green900)
                .border(width = 2.dp, color = AppColors.MainGreen.Green200, shape = CircleShape)
                .clickable(onClick = onPhotoChangeClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.profile_edit),
                contentDescription = "사진 변경",
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// 이름 입력 섹션
// ─────────────────────────────────────────────────────────
@Composable
private fun NicknameInputSection(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "이름",
            color = AppColors.MainGreen.Green700,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            textStyle = TextStyle(
                color = AppColors.Monotone.SoftBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(AppColors.MainGreen.Green700),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.Monotone.White,
                    shape = RoundedCornerShape(13.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.Monotone.Gray100,
                    shape = RoundedCornerShape(13.dp)
                )
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "친구들에게 보여지는 이름이에요",
            color = AppColors.Monotone.Gray500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────
// 기본 프로필 정보 카드
// ─────────────────────────────────────────────────────────
@Composable
private fun ProfileInfoSection(
    joinDate: String,
    totalAppointments: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "기본 프로필 정보",
            color = AppColors.MainGreen.Green700,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.Monotone.White,
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.Monotone.Gray100,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            ProfileInfoRow(
                label = "가입일",
                value = joinDate,
                valueColor = AppColors.Monotone.SoftBlack
            )
            ProfileInfoRow(
                label = "함께한 약속",
                value = "${totalAppointments}회",
                valueColor = AppColors.MainGreen.Green700
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AppColors.Monotone.Gray500,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────
// 사진 변경 바텀시트 (MYP-002-02)
// ─────────────────────────────────────────────────────────
@Composable
private fun PhotoChangeBottomSheet(
    onDismiss: () -> Unit,
    onSelectFromLibrary: () -> Unit,
    onTakePhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = sheetShape)
            .background(color = AppColors.Monotone.White, shape = sheetShape)
            .navigationBarsPadding()
    ) {
        // 드래그 핸들 (상단 Gray200 바)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Monotone.Gray200)
            )
        }
        // 타이틀 행
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "프로필 사진 변경",
                color = AppColors.Monotone.SoftBlack,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
        // 항목 목록
        PhotoSheetItem(text = "라이브러리에서 선택", onClick = { onSelectFromLibrary(); onDismiss() })
        PhotoSheetItem(text = "사진 찍기", onClick = { onTakePhoto(); onDismiss() })
        PhotoSheetItem(text = "프로필 사진 삭제", onClick = { onDeletePhoto(); onDismiss() })
        // 하단 홈 인디케이터 여백
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 108.dp, height = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Monotone.SoftBlack)
            )
        }
    }
}

@Composable
private fun PhotoSheetItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = AppColors.MainGreen.Green700,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
        Icon(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = null,
            tint = AppColors.MainGreen.Green700,
            modifier = Modifier.size(14.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPreview() {
    JigmnnagaTheme {
        ProfileEditScreen(
            state = ProfileEditUiState.PreviewDefault,
            onBackClick = {},
            onNicknameChange = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenDisabledPreview() {
    JigmnnagaTheme {
        ProfileEditScreen(
            state = ProfileEditUiState.PreviewDisabled,
            onBackClick = {},
            onNicknameChange = {},
            onSaveClick = {}
        )
    }
}
