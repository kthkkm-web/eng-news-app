package app.myroutine.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.ui.components.ProgressRing
import app.myroutine.ui.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val PAGES = 2000
private const val MID = PAGES / 2

/** 일~토 주간 스트립. 좌우 스와이프로 주 이동, 날짜마다 달성률 링. */
@Composable
fun WeekStrip(
    today: LocalDate,
    selected: LocalDate,
    weekStart: DayOfWeek,
    ratios: Map<LocalDate, Float?>,
    onSelect: (LocalDate) -> Unit,
) {
    val anchor = remember(today, weekStart) { ScheduleResolver.weekStartOf(today, weekStart) }
    fun pageOf(d: LocalDate) = MID + ChronoUnit.WEEKS.between(anchor, ScheduleResolver.weekStartOf(d, weekStart)).toInt()
    val pager = rememberPagerState(initialPage = pageOf(selected)) { PAGES }
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // 선택 날짜가 바뀌면(달력 등) 페이지 이동
    LaunchedEffect(selected, anchor) {
        val p = pageOf(selected)
        if (pager.currentPage != p) pager.animateScrollToPage(p)
    }
    // 사용자가 넘기면 같은 요일로 선택 이동
    LaunchedEffect(pager, anchor) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { page ->
            val cur = pageOf(currentSelected)
            if (page != cur) currentOnSelect(currentSelected.plusWeeks((page - cur).toLong()))
        }
    }

    HorizontalPager(pager, modifier = Modifier.fillMaxWidth()) { page ->
        val ws = anchor.plusWeeks((page - MID).toLong())
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (0L until 7L).forEach { i ->
                val d = ws.plusDays(i)
                DayCell(
                    date = d,
                    isToday = d == today,
                    isSelected = d == selected,
                    ratio = ratios[d],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(d) },
                )
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, ratio: Float?, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    val fg = if (isSelected) c.onAccent else c.textPrimary
    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isSelected) c.accent else Color.Transparent)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                RuleFormatter.dayName(date.dayOfWeek), fontSize = 14.sp,
                color = when { isSelected -> fg; date.dayOfWeek == DayOfWeek.SUNDAY -> c.danger.copy(alpha = 0.85f); else -> c.textSecondary },
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            ProgressRing(
                progress = ratio,
                color = if (isSelected) c.onAccent else c.accent,
                track = if (isSelected) c.onAccent.copy(alpha = 0.25f) else c.surfaceHigh,
                size = 38.dp,
                stroke = 3.dp,
            ) {
                Text(
                    "${date.dayOfMonth}", color = fg, fontSize = 15.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
