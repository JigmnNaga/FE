package com.jigmnnaga.presentation.appointment.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

private val TimeColumnWidth = 40.dp
private val CellWidth = 53.75.dp
private val CellHeight = 29.dp
private val CellGap = 3.dp
private val CellCorner = 3.dp
private val HeaderHeight = 15.dp
private val LegendKeySize = 12.dp

/**
 * 겹치는 일정 히트맵 시간표(APT-005-03). 표시 전용이며 집계와 선택 규칙은 호출하는 쪽이 정한다.
 * 이 시간표에는 사용자 동작이 없으므로 콜백 없이 상태만 받는다.
 * 시스템 바와 IME inset은 부모 화면이 처리한다.
 */
@Composable
fun AvailabilityHeatmap(
    state: AvailabilityHeatmapUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing20),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            Text(
                text = stringResource(R.string.appointment_schedule_heatmap_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.monthLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = AppColors.MainGreen.Green900
                )
                Text(
                    text = stringResource(R.string.appointment_schedule_heatmap_unit),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = AppColors.Monotone.Gray600
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TimeColumn(state = state)
                TimetableGrid(state = state, modifier = Modifier.weight(1f))
            }
            AvailabilityLegend()
        }
    }
}

@Composable
private fun TimeColumn(state: AvailabilityHeatmapUiState) {
    Column(modifier = Modifier.width(TimeColumnWidth)) {
        Spacer(Modifier.height(HeaderHeight))
        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            state.rows.forEach { row ->
                Box(
                    modifier = Modifier.height(CellHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = AppColors.Monotone.Gray600,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableGrid(
    state: AvailabilityHeatmapUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.height(HeaderHeight),
            horizontalArrangement = Arrangement.spacedBy(CellGap)
        ) {
            state.dates.forEach { date ->
                Text(
                    text = date.label,
                    modifier = Modifier.width(CellWidth),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.Monotone.Gray600,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            state.rows.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    state.dates.forEachIndexed { columnIndex, date ->
                        AvailabilityCell(
                            level = state.levelAt(rowIndex, columnIndex),
                            description = stringResource(
                                R.string.appointment_schedule_heatmap_cell_description,
                                date.label,
                                row.label,
                                row.availableCounts[columnIndex]
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityCell(level: AvailabilityLevel, description: String) {
    Box(
        modifier = Modifier
            .width(CellWidth)
            .height(CellHeight)
            .clip(RoundedCornerShape(CellCorner))
            .background(level.color())
            .semantics { contentDescription = description }
    )
}

@Composable
private fun AvailabilityLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.appointment_schedule_heatmap_legend_title),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = AppColors.Monotone.Gray600
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvailabilityLevel.entries.forEach { level ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(LegendKeySize)
                            .clip(RoundedCornerShape(CellCorner))
                            .background(level.color())
                    )
                    Text(
                        text = stringResource(
                            R.string.appointment_schedule_heatmap_legend_count,
                            level.legendCount
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = AppColors.Monotone.Gray600
                    )
                }
            }
        }
    }
}

private fun AvailabilityLevel.color(): Color = when (this) {
    AvailabilityLevel.NONE -> AppColors.Monotone.Gray100
    AvailabilityLevel.LOW -> AppColors.MainGreen.Green300
    AvailabilityLevel.MEDIUM -> AppColors.MainGreen.Green500
    AvailabilityLevel.HIGH -> AppColors.MainGreen.Green800
}

@Preview(showBackground = true, widthDp = 412, heightDp = 560)
@Composable
private fun AvailabilityHeatmapPreview() {
    JigmnnagaTheme {
        AvailabilityHeatmap(
            state = previewHeatmapState(),
            modifier = Modifier.padding(AppDimens.Spacing16)
        )
    }
}

private fun previewHeatmapState() = AvailabilityHeatmapUiState(
    monthLabel = "2026년 9월",
    dates = listOf(
        "4일 (금)", "5일 (토)", "6일 (일)", "7일 (월)",
        "8일 (화)", "9일 (수)", "10일 (목)", "11일 (금)"
    ).mapIndexed { index, label -> AvailabilityDateColumnUiState("date-${index + 4}", label) },
    rows = listOf(
        "10:00" to listOf(1, 1, 1, 0, 0, 0, 0, 0),
        "11:00" to listOf(2, 2, 2, 1, 1, 1, 1, 1),
        "12:00" to listOf(2, 3, 3, 1, 1, 1, 1, 1),
        "13:00" to listOf(2, 3, 3, 1, 1, 1, 1, 1),
        "14:00" to listOf(0, 2, 2, 1, 1, 1, 1, 1),
        "15:00" to listOf(0, 1, 1, 0, 0, 0, 0, 0),
        "16:00" to listOf(1, 1, 1, 0, 0, 0, 0, 0),
        "17:00" to listOf(1, 1, 1, 0, 0, 0, 0, 0),
        "18:00" to listOf(0, 0, 0, 0, 0, 0, 0, 0),
        "19:00" to listOf(0, 0, 0, 0, 0, 0, 0, 0)
    ).map { (label, counts) -> AvailabilityTimeRowUiState(label, counts) }
)