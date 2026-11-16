package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.CalendarResult

interface CalendarRepository {
    /**
     * 월별 캘린더 조회.
     * @param month `yyyy-MM` 형식 (예: `"2026-09"`)
     */
    suspend fun getCalendar(month: String): CalendarResult
}
