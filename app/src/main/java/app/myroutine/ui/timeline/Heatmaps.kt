package app.myroutine.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** 진행률 → 칸 색. null = 해당 없음. */
fun heatColor(p: Float?, color: Color, empty: Color, notDue: Color): Color = when {
    p == null -> notDue
    p <= 0f -> empty
    else -> lerp(empty, color, 0.35f + 0.65f * p.coerceIn(0f, 1f))
}

/**
 * GitHub 잔디 스타일. 열 = 주, 행 = 요일. 마지막 열이 [endDate] 가 속한 주.
 * columns = null 이면 폭에 맞춰 칸 수 자동.
 */
@Composable
fun GrassHeatmap(
    endDate: LocalDate,
    today: LocalDate,
    weekStart: DayOfWeek,
    values: Map<LocalDate, Float?>,
    color: Color,
    modifier: Modifier = Modifier,
    columns: Int? = null,
    cellDp: Float = 14f,
    showMonthLabels: Boolean = false,
) {
    val c = AppTheme.colors
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 3f
        val cols = columns ?: ((maxWidth.value + gap) / (cellDp + gap)).toInt().coerceAtLeast(1)
        val cell = (maxWidth.value - gap * (cols - 1)) / cols
        val lastWeek = ScheduleResolver.weekStartOf(endDate, weekStart)
        val firstWeek = lastWeek.minusWeeks((cols - 1).toLong())
        val labelH = if (showMonthLabels) 16f else 0f
        Column {
            if (showMonthLabels) {
                Box(Modifier.fillMaxWidth().height(labelH.dp)) {
                    var prevMonth = -1
                    for (i in 0 until cols) {
                        val wk = firstWeek.plusWeeks(i.toLong())
                        val m = wk.plusDays(6).monthValue
                        if (m != prevMonth && wk.plusDays(6).dayOfMonth <= 7) {
                            Text(
                                java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH),
                                color = c.textSecondary, fontSize = 10.sp,
                                modifier = Modifier.padding(start = ((cell + gap) * i).dp),
                            )
                        }
                        prevMonth = m
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(((cell + gap) * 7 - gap).dp)) {
                val px = with(density) { cell.dp.toPx() }
                val g = with(density) { gap.dp.toPx() }
                val r = CornerRadius(px * 0.22f)
                for (col in 0 until cols) for (row in 0 until 7) {
                    val d = firstWeek.plusWeeks(col.toLong()).plusDays(row.toLong())
                    if (d.isAfter(endDate)) continue
                    val tl = Offset(col * (px + g), row * (px + g))
                    drawRoundRect(heatColor(values[d], color, c.heatEmpty, c.heatEmpty.copy(alpha = 0.55f)), tl, Size(px, px), r)
                    if (d == today) drawRoundRect(color, tl, Size(px, px), r, style = Stroke(with(density) { 1.5.dp.toPx() }))
                }
            }
        }
    }
}

/** 1주: 요일 7칸 (일~토 라벨). */
@Composable
fun WeekRowHeat(weekStartDate: LocalDate, today: LocalDate, values: Map<LocalDate, Float?>, color: Color) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (0L until 7L).forEach { i ->
            val d = weekStartDate.plusDays(i)
            HeatCell(
                text = RuleFormatter.dayName(d.dayOfWeek), bg = heatColor(values[d], color, c.heatEmpty, c.heatEmpty.copy(alpha = 0.5f)),
                isToday = d == today, color = color, dim = values[d] == null, modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 1달: 달력형. */
@Composable
fun MonthGridHeat(month: YearMonth, today: LocalDate, weekStart: DayOfWeek, values: Map<LocalDate, Float?>, color: Color, showWeekdays: Boolean = false) {
    val c = AppTheme.colors
    val lead = ((month.atDay(1).dayOfWeek.value - weekStart.value) + 7) % 7
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showWeekdays) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0L until 7L).forEach { Text(RuleFormatter.dayName(weekStart.plus(it)), color = c.textTertiary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { d ->
                    if (d == null) Spacer(Modifier.weight(1f))
                    else HeatCell(
                        "${d.dayOfMonth}", heatColor(values[d], color, c.heatEmpty, c.heatEmpty.copy(alpha = 0.5f)),
                        d == today, color, dim = values[d] == null, modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HeatCell(text: String, bg: Color, isToday: Boolean, color: Color, dim: Boolean, modifier: Modifier) {
    val c = AppTheme.colors
    Box(
        modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(bg)
            .then(if (isToday) Modifier.border(1.5.dp, color, RoundedCornerShape(6.dp)) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (dim) c.textTertiary.copy(alpha = 0.6f) else c.textPrimary.copy(alpha = 0.85f), fontSize = 11.sp) }
}

