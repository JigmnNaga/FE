package com.jigmnnaga.presentation.mypage

data class ProfileEditUiState(
    val nickname: String = "",
    val profileInitial: String? = null,
    val joinDate: String = "",
    val totalAppointments: Int = 0,
    val isSaveEnabled: Boolean = false
) {
    companion object {
        val PreviewDefault = ProfileEditUiState(
            nickname = "김지금",
            profileInitial = "나",
            joinDate = "2026. 04. 12",
            totalAppointments = 23,
            isSaveEnabled = true
        )

        val PreviewDisabled = ProfileEditUiState(
            nickname = "",
            profileInitial = null,
            joinDate = "2026. 04. 12",
            totalAppointments = 0,
            isSaveEnabled = false
        )
    }
}
