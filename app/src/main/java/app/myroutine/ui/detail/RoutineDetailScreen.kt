package app.myroutine.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.ui.chart.ChartRenderer
import app.myroutine.ui.chart.ChartStyle
import app.myroutine.ui.theme.AppTheme
import app.myroutine.ui.theme.RoutinePalettes
import app.myroutine.ui.timeline.MonthGridHeat

@Composable
fun RoutineDetailScreen(onBack: () -> Unit, onEdit: (String) -> Unit, vm: RoutineDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = AppTheme.colors
    LaunchedEffect(s.deleted) { if (s.deleted) onBack() }
    val r = s.routine
    Column(Modifier.fillMaxSize().background(c.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = c.textPrimary) }
            Spacer(Modifier.weight(1f))
            if (r != null) IconButton(onClick = { onEdit(r.id) }) { Icon(Icons.Rounded.Edit, "수정", tint = c.textPrimary) }
        }
        if (r == null) return@Column
        val color = RoutinePalettes.color(r.color)
        val st = s.stats
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("${r.emoji}${r.name}", color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            Text(RuleFormatter.summary(r), color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(4.dp))
            if (r.archived) Text("보관된 루틴이에요", color = c.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
            Spacer(Modifier.height(12.dp))
            if (st != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("연속 달성", "${st.currentStreak}일", Modifier.weight(1f))
                    StatTile("최장 연속", "${st.bestStreak}일", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("이번 달 달성률", st.monthRate?.let { "${(it * 100).toInt()}%" } ?: "-", Modifier.weight(1f), sub = "${st.monthDone}/${st.monthDue}일")
                    StatTile(
                        if (r.recordType == RecordType.NUMERIC) "기록한 날" else "총 완료",
                        if (r.recordType == RecordType.NUMERIC) "${st.totalCount}일" else "${st.totalCount}회",
                        Modifier.weight(1f), sub = "완료한 날 ${st.totalDoneDays}일",
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (r.recordType == RecordType.NUMERIC) {
                Section("기록 추이${r.unit?.let { " ($it)" } ?: ""}") {
                    ChartImage(s.series, color.toArgb(), bars = false, decimals = r.decimals)
                }
                Spacer(Modifier.height(16.dp))
                Section("최근 기록") {
                    ChartImage(s.series.takeLast(8), color.toArgb(), bars = true, decimals = r.decimals)
                }
                Spacer(Modifier.height(16.dp))
            }

            Section(null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shiftMonth(-1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "이전 달", tint = c.textPrimary) }
                    Text("${s.month.year}년 ${s.month.monthValue}월", color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.shiftMonth(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "다음 달", tint = c.textPrimary) }
                }
                MonthGridHeat(s.month, s.today, s.weekStart, s.monthValues, color, showWeekdays = true)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { vm.setArchived(!r.archived) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (r.archived) "보관 해제" else "보관하기", color = c.textSecondary)
            }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier, sub: String? = null) {
    val c = AppTheme.colors
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(c.surface).padding(16.dp)) {
        Text(title, color = c.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = c.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = c.textTertiary, fontSize = 12.sp)
    }
}

@Composable
private fun Section(title: String?, content: @Composable () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface).padding(16.dp)) {
        if (title != null) {
            Text(title, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

@Composable
private fun ChartImage(points: List<Pair<java.time.LocalDate, Double>>, color: Int, bars: Boolean, decimals: Int) {
    val c = AppTheme.colors
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().height(180.dp)) {
        val w = with(density) { maxWidth.roundToPx() }
        val h = with(density) { maxHeight.roundToPx() }
        val style = ChartStyle(line = color, text = c.textPrimary.toArgb(), subText = c.textSecondary.toArgb(), grid = c.outline.toArgb(), density = density.density)
        val bmp = remember(points, w, h, style) {
            if (bars) ChartRenderer.barChart(w, h, points, style, decimals) else ChartRenderer.lineChart(w, h, points, style, decimals)
        }
        Image(bmp.asImageBitmap(), contentDescription = null)
    }
}
