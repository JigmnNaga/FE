package com.jigmnnaga.presentation.appointment.schedule

/** 가능 인원 수에 따른 시간표 칸 색상 단계. */
enum class AvailabilityLevel(val legendCount: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        fun fromCount(count: Int): AvailabilityLevel = when {
            count <= 0 -> NONE
            count == 1 -> LOW
            count == 2 -> MEDIUM
            else -> HIGH
        }
    }
}

data class AvailabilityDateColumnUiState(
    val id: String,
    val label: String
)

data class AvailabilityTimeRowUiState(
    val label: String,
    val availableCounts: List<Int>
)

/** UI-only state. 가능 인원 집계는 화면을 소유한 쪽에서 계산해 전달한다. */
data class AvailabilityHeatmapUiState(
    val monthLabel: String,
    val dates: List<AvailabilityDateColumnUiState>,
    val rows: List<AvailabilityTimeRowUiState>
) {
    init {
        require(rows.all { it.availableCounts.size == dates.size })
    }

    fun levelAt(rowIndex: Int, columnIndex: Int): AvailabilityLevel =
        AvailabilityLevel.fromCount(rows[rowIndex].availableCounts[columnIndex])
}