package com.jigmnnaga.presentation.mypage

import java.util.Locale

/**
 * 마이페이지 메인 화면 UI 상태.
 *
 * API 연동 전에는 Mock 데이터로, 연동 후에는 MypageViewModel이 채운다.
 */
data class MypageUiState(
    val nickname: String = "",
    val email: String = "",
    /** 프로필 아바타에 표시할 첫 글자. null이면 기본 아이콘 표시. */
    val profileInitial: String? = null,
    /** 프로필 이미지 URL. null이면 이니셜 또는 기본 아이콘 표시. */
    val profileImageUrl: String? = null,
    val totalAppointments: Int = 0,
    val onTimeCount: Int = 0,
    val lateCount: Int = 0,
    val isProfileLoading: Boolean = false,
    val isStatsLoading: Boolean = false,
    val profileLoadFailed: Boolean = false,
    val statsLoadFailed: Boolean = false,
    val loginRequired: Boolean = false
) {
    /** 제때 도착률 (0~100). 약속이 0개면 0f. */
    val onTimeRatePercent: Float
        get() = if (totalAppointments == 0) 0f
                else (onTimeCount.toFloat() / totalAppointments.toFloat()) * 100f

    /** 제때 도착률 텍스트 (예: "95.65%"). 약속이 0개면 "0%". */
    val onTimeRateText: String
        get() = if (totalAppointments == 0) "0%"
                else String.format(Locale.US, "%.2f%%", onTimeRatePercent)

    companion object {
        /** Preview: 기본 마이페이지 화면 */
        val PreviewDefault = MypageUiState(
            nickname = "김지금",
            email = "jigeum@kakao.com",
            profileInitial = "나",
            totalAppointments = 23,
            onTimeCount = 22,
            lateCount = 1
        )

        /** Preview: 프로필 이미지(이니셜)가 없는 화면 */
        val PreviewNoProfileImage = MypageUiState(
            nickname = "김지금",
            email = "jigeum@kakao.com",
            profileInitial = null,
            totalAppointments = 23,
            onTimeCount = 22,
            lateCount = 1
        )

        /** Preview: 긴 닉네임 및 이메일 */
        val PreviewLongName = MypageUiState(
            nickname = "매우길고긴닉네임이엄청나게길어요",
            email = "very.long.email.address.here@example.com",
            profileInitial = "매",
            totalAppointments = 23,
            onTimeCount = 22,
            lateCount = 1
        )

        /** Preview: 통계 데이터가 0인 화면 */
        val PreviewZeroStats = MypageUiState(
            nickname = "김지금",
            email = "jigeum@kakao.com",
            profileInitial = "나",
            totalAppointments = 0,
            onTimeCount = 0,
            lateCount = 0
        )
    }
}
