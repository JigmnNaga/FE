package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.AttendanceRespondRequestDto
import com.jigmnnaga.domain.model.AttendanceAnswer

internal fun AttendanceAnswer.toDto(): AttendanceRespondRequestDto =
    AttendanceRespondRequestDto(attendanceStatus = name)
