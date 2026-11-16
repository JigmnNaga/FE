package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.CalendarDayResponseDto
import com.jigmnnaga.domain.model.CalendarDay

/**
 * [CalendarDayResponseDto]를 도메인 모델 [CalendarDay]로 변환한다.
 *
 * 개별 약속은 기존 [HomeAppointmentMapper.toDomain]을 그대로 재사용한다.
 * - `confirmed = false`이지만 `confirmedDate != null`인 약속도 정상 처리된다.
 * - 장소 정보([confirmedLocationName], [detailPlaceName])가 null이어도 오류가 발생하지 않는다.
 * - `memberCount`만 활용하며 참여자 이름·이미지를 임의 생성하지 않는다.
 */
internal fun CalendarDayResponseDto.toDomain(): CalendarDay = CalendarDay(
    date = date,
    appointments = appointments.map { it.toDomain() }
)
