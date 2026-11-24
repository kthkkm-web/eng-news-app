package app.myroutine.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.ui.components.PillChip
import app.myroutine.ui.theme.AppTheme
import app.myroutine.ui.theme.RoutinePalettes
import java.time.YearMonth

@Composable
fun TimelineScreen(onOpenRoutine: (String) -> Unit, onManageTags: () -> Unit, vm: TimelineViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = AppTheme.colors

    LazyColumn(Modifier.fillMaxSize().background(c.background), contentPadding = PaddingValues(bottom = 40.dp)) {
        item(key = "top") {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("타임라인", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).border(2.dp, c.textPrimary, RoundedCornerShape(10.dp))
                        .clickable(onClick = vm::cyclePeriod).padding(horizontal = 10.dp, vertical = 4.dp),
                ) { Text(s.period.label, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
        item(key = "chips") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                PillChip("진행 중 ${s.activeCount}", !s.showEnded, filledStyle = true) { vm.setShowEnded(false) }
                Spacer(Modifier.width(8.dp))
                if (s.endedCount > 0) PillChip("종료·보관 ${s.endedCount}", s.showEnded, filledStyle = true) { vm.setShowEnded(true) }
                Spacer(Modifier.weight(1f))
                Text("태그 관리", color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onManageTags).padding(6.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        item(key = "overall") {
            Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(c.surface).padding(16.dp)) {
                Text("전체 달성", color = c.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                GrassHeatmap(s.today, s.today, s.weekStart, s.overall, c.accent, cellDp = 14f, showMonthLabels = true)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!s.loading && s.groups.isEmpty()) {
            item(key = "empty") {
                Text(
                    if (s.showEnded) "종료·보관된 루틴이 없어요" else "루틴을 추가하면 여기에 기록이 쌓여요",
                    color = c.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(48.dp),
                )
            }
        }
        s.groups.forEach { g ->
            item(key = "g-${g.key}") {
                val open = g.key !in s.collapsed
                Row(
                    Modifier.fillMaxWidth().clickable { vm.toggleGroup(g.key) }.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (g.emoji.isNotBlank()) Text(g.emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                    Text(g.title, color = c.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("  ${g.cards.size}", color = c.textTertiary, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = c.textPrimary)
                }
            }
            if (g.key !in s.collapsed) {
                val perRow = if (s.period == TimelinePeriod.YEAR) 1 else 2
                g.cards.chunked(perRow).forEachIndexed { i, row ->
                    item(key = "c-${g.key}-$i-${row.joinToString { it.routine.id }}") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { card -> TimelineCardView(card, s, Modifier.weight(1f)) { onOpenRoutine(card.routine.id) } }
                            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCardView(card: TimelineCard, s: TimelineUiState, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    val color = RoutinePalettes.color(card.routine.color)
    Row(modifier.clip(RoundedCornerShape(20.dp)).background(c.surface).clickable(onClick = onClick).padding(top = 14.dp, bottom = 14.dp, end = 14.dp)) {
        Box(Modifier.padding(top = 2.dp).width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(card.label, color = c.textSecondary, fontSize = 12.sp, maxLines = 1)
            Text(
                "${card.routine.emoji}${card.routine.name}", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            when (s.period) {
                TimelinePeriod.WEEK -> WeekRowHeat(ScheduleResolver.weekStartOf(s.today, s.weekStart), s.today, card.values, color)
                TimelinePeriod.MONTH -> MonthGridHeat(YearMonth.from(s.today), s.today, s.weekStart, card.values, color)
                TimelinePeriod.YEAR -> GrassHeatmap(s.today, s.today, s.weekStart, card.values, color, cellDp = 11f, showMonthLabels = true)
            }
        }
    }
}

