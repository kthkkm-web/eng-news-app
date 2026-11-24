package app.myroutine.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendarSheet(
    selected: LocalDate,
    today: LocalDate,
    weekStart: DayOfWeek,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val c = AppTheme.colors
    var month by remember { mutableStateOf(YearMonth.from(selected)) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "이전 달", tint = c.textPrimary) }
                Text("${month.year}년 ${month.monthValue}월", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "다음 달", tint = c.textPrimary) }
            }
            Spacer(Modifier.height(8.dp))
            val days = (0L until 7L).map { weekStart.plus(it) }
            Row(Modifier.fillMaxWidth()) {
                days.forEach { Text(RuleFormatter.dayName(it), color = c.textTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
            val first = month.atDay(1)
            val lead = ((first.dayOfWeek.value - weekStart.value) + 7) % 7
            val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { d ->
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                            if (d != null) {
                                val sel = d == selected
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                                        .background(if (sel) c.accent else c.surface)
                                        .then(if (d == today && !sel) Modifier.border(1.5.dp, c.accent, CircleShape) else Modifier)
                                        .clickable { onPick(d) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${d.dayOfMonth}", color = if (sel) c.onAccent else c.textPrimary, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            TextButton(onClick = { onPick(today) }, modifier = Modifier.align(Alignment.End)) { Text("오늘로", color = c.accent) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
