package app.myroutine.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.myroutine.domain.model.Todo
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.ui.chart.ChartRenderer
import app.myroutine.ui.chart.ChartStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 공통 베이스: 상태(Preferences)의 VERSION/TARGET 이 바뀌면 데이터를 다시 읽는다.
 */
abstract class DataWidget<T> : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    abstract suspend fun load(context: Context, target: String?): T

    @Composable
    abstract fun Content(context: Context, data: T)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val initial = load(context, prefs.target())
        provideContent {
            val p = currentState<Preferences>()
            val version = p[WidgetKeys.VERSION] ?: 0L
            val target = p.target()
            val data by produceState(initial, version, target) { value = load(context, target) }
            Content(context, data)
        }
    }
}

private val MD = DateTimeFormatter.ofPattern("M월 d일")
private fun dayLabel(d: LocalDate) = "${d.format(MD)}(${RuleFormatter.dayName(d.dayOfWeek)})"

/** 비트맵 픽셀 크기: 메모리 제한 때문에 밀도는 최대 2.5배. */
private fun bitmapPx(context: Context, size: DpSize): Triple<Int, Int, Float> {
    val d = context.resources.displayMetrics.density.coerceAtMost(2.5f)
    return Triple((size.width.value * d).roundToInt(), (size.height.value * d).roundToInt(), d)
}

// ============================================================ 1. 오늘의 루틴 (2x2)

class TodayRoutinesWidget : DataWidget<Pair<WContext, List<WRoutineRow>>>() {
    override suspend fun load(context: Context, target: String?) = WidgetDataSource.todayRoutines(context, target)

    @Composable
    override fun Content(context: Context, data: Pair<WContext, List<WRoutineRow>>) {
        val (w, rows) = data
        val target = currentState<Preferences>().target()
        WidgetFrame(openApp(context, "home")) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WText("루틴 ", 15.sp, bold = true)
                WText("${rows.count { it.complete }}/${rows.size}", 15.sp, WColors.sub)
                Spacer(GlanceModifier.defaultWeight())
                if (target != null) WText(WidgetDataSource.tagLabel(w, target), 11.sp, WColors.tertiary)
            }
            Spacer(GlanceModifier.height(6.dp))
            if (rows.isEmpty()) {
                WText("오늘 할 루틴이 없어요", 12.sp, WColors.sub)
            } else {
                LazyColumn(GlanceModifier.fillMaxSize()) {
                    items(rows, itemId = { it.id.hashCode().toLong() }) { r ->
                        Row(
                            GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)
                                .clickable(actionRunCallback<IncrementRoutineAction>(actionParametersOf(WidgetKeys.ROUTINE_ID to r.id))),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ColorBar(r.color)
                            Spacer(GlanceModifier.width(6.dp))
                            WText("${r.emoji}${r.name}", 13.sp, if (r.complete) WColors.tertiary else WColors.text, modifier = GlanceModifier.defaultWeight())
                            val right = when {
                                r.complete -> "✓"
                                r.numeric -> r.valueText ?: (r.time ?: "-")
                                r.target > 1 && r.count > 0 -> "${r.count}/${r.target}"
                                else -> r.time ?: "${r.count}/${r.target}"
                            }
                            WText(right, 12.sp, if (r.complete) WColors.accent else WColors.sub)
                        }
                    }
                }
            }
        }
    }
}

class TodayRoutinesReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayRoutinesWidget() }

// ============================================================ 2. 오늘의 할 일 (2x2)

class TodayTodosWidget : DataWidget<Pair<LocalDate, List<Todo>>>() {
    override suspend fun load(context: Context, target: String?) = WidgetDataSource.todayTodos(context)

    @Composable
    override fun Content(context: Context, data: Pair<LocalDate, List<Todo>>) {
        val todos = data.second
        WidgetFrame(openApp(context, "home")) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WText("할 일 ", 15.sp, bold = true)
                WText("${todos.count { it.done }}/${todos.size}", 15.sp, WColors.sub, modifier = GlanceModifier.defaultWeight())
                Box(
                    GlanceModifier.size(26.dp).cornerRadius(13.dp).background(WColors.cell).clickable(actionStartActivity(android.content.Intent(context, QuickTodoActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))),
                    contentAlignment = Alignment.Center,
                ) { WText("+", 16.sp, WColors.text, bold = true) }
            }
            Spacer(GlanceModifier.height(6.dp))
            if (todos.isEmpty()) {
                WText("+ 를 눌러 할 일을 추가해요", 12.sp, WColors.sub)
            } else {
                LazyColumn(GlanceModifier.fillMaxSize()) {
                    items(todos, itemId = { it.id.hashCode().toLong() }) { t ->
                        Row(
                            GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)
                                .clickable(actionRunCallback<ToggleTodoAction>(actionParametersOf(WidgetKeys.TODO_ID to t.id))),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(GlanceModifier.size(14.dp).cornerRadius(4.dp).background(if (t.done) WColors.accent else WColors.cell), contentAlignment = Alignment.Center) {
                                if (t.done) WText("✓", 10.sp, WColors.fixed(0xFF06231A), bold = true)
                            }
                            Spacer(GlanceModifier.width(6.dp))
                            WText(t.title, 13.sp, if (t.done) WColors.tertiary else WColors.text, modifier = GlanceModifier.defaultWeight())
                            t.time?.let { WText("%02d:%02d".format(it.hour, it.minute), 12.sp, WColors.sub) }
                        }
                    }
                }
            }
        }
    }
}

class TodayTodosReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayTodosWidget() }

// ============================================================ 3. 타임라인 월간 (2x2)

class MonthTimelineWidget : DataWidget<Triple<WContext, java.time.YearMonth, Map<LocalDate, Float?>>>() {
    override suspend fun load(context: Context, target: String?) = WidgetDataSource.monthOverall(context)

    @Composable
    override fun Content(context: Context, data: Triple<WContext, java.time.YearMonth, Map<LocalDate, Float?>>) {
        val (w, month, values) = data
        val size = LocalSize.current
        val accent = if (WColors.isNight(context)) 0xFF3DDC97 else 0xFF13B57A
        val lead = ((month.atDay(1).dayOfWeek.value - w.weekStart.value) + 7) % 7
        val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val weeks = cells.chunked(7)
        val cellH = ((size.height.value - 24 - 34) / weeks.size).coerceIn(8f, 40f).dp
        WidgetFrame(openApp(context, "timeline")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WText("전체 ", 15.sp, bold = true)
                WText("${month.monthValue}월", 13.sp, WColors.sub)
            }
            Spacer(GlanceModifier.height(8.dp))
            weeks.forEach { week ->
                Row(GlanceModifier.fillMaxWidth()) {
                    week.forEach { d ->
                        Box(GlanceModifier.defaultWeight().height(cellH).padding(1.5.dp)) {
                            if (d != null) {
                                val heat = WColors.fixed(WColors.heatArgb(context, values[d], accent))
                                if (d == w.today) {
                                    Box(GlanceModifier.fillMaxSize().cornerRadius(4.dp).background(WColors.fixed(accent)).padding(1.5.dp)) {
                                        Box(GlanceModifier.fillMaxSize().cornerRadius(3.dp).background(heat)) {}
                                    }
                                } else {
                                    Box(GlanceModifier.fillMaxSize().cornerRadius(4.dp).background(heat)) {}
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Box(GlanceModifier.defaultWeight().height(cellH)) {} }
                }
            }
        }
    }
}

class MonthTimelineReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MonthTimelineWidget() }

// ============================================================ 4·5. 루틴 그래프 (4x2)

abstract class GraphWidget(private val bars: Boolean) : DataWidget<Pair<app.myroutine.domain.model.Routine?, List<Pair<LocalDate, Double>>>>() {
    override suspend fun load(context: Context, target: String?) = WidgetDataSource.numeric(context, target)

    @Composable
    override fun Content(context: Context, data: Pair<app.myroutine.domain.model.Routine?, List<Pair<LocalDate, Double>>>) {
        val (routine, series) = data
        val size = LocalSize.current
        WidgetFrame(if (routine != null) openApp(context, "routine/detail/${routine.id}") else openApp(context, "home"), padding = 12.dp) {
            if (routine == null) {
                WText("수치 그래프", 15.sp, bold = true)
                Spacer(GlanceModifier.height(6.dp))
                WText("루틴 추가 화면에서 기록 방식을 '수치'로 만든 루틴이 있어야 해요", 12.sp, WColors.sub, maxLines = 3)
                return@WidgetFrame
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                WText("${routine.emoji} ${routine.name}", 15.sp, bold = true)
                routine.unit?.let { WText("  ($it)", 12.sp, WColors.sub) }
            }
            Spacer(GlanceModifier.height(4.dp))
            val (pw, ph, d) = bitmapPx(context, DpSize(size.width - 24.dp, size.height - 24.dp - 26.dp))
            val p = WColors.palette(context)
            val style = ChartStyle(line = app.myroutine.ui.theme.RoutinePalettes.argb(routine.color).toInt(), text = p.text, subText = p.sub, grid = p.track, density = d)
            val pts = if (bars) series.takeLast(8) else series
            val bmp = if (bars) ChartRenderer.barChart(pw, ph, pts, style, routine.decimals) else ChartRenderer.lineChart(pw, ph, pts, style, routine.decimals)
            Image(ImageProvider(bmp), contentDescription = "${routine.name} 그래프", modifier = GlanceModifier.fillMaxSize())
        }
    }
}

class GraphLineWidget : GraphWidget(bars = false)
class GraphBarWidget : GraphWidget(bars = true)
class GraphLineReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = GraphLineWidget() }
class GraphBarReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = GraphBarWidget() }

// ============================================================ 6. 루틴 달성 흐름 (4x2)

class StreakFlowWidget : DataWidget<WidgetDataSource.Flow>() {
    override suspend fun load(context: Context, target: String?) = WidgetDataSource.streakFlow(context, target)

    @Composable
    override fun Content(context: Context, data: WidgetDataSource.Flow) {
        val size = LocalSize.current
        val p = WColors.palette(context)
        WidgetFrame(openApp(context, "timeline"), padding = 14.dp) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WText(data.title, 15.sp, bold = true, modifier = GlanceModifier.defaultWeight())
                WText("${data.weekStart.format(MD)} - ${data.weekStart.plusDays(6).dayOfMonth}일", 12.sp, WColors.sub)
            }
            Spacer(GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WText("이번주 ", 16.sp, bold = true)
                WText("${data.doneDays}일 달성", 16.sp, WColors.fixed(data.color), bold = true)
                WText("했어요!", 16.sp, bold = true)
            }
            Spacer(GlanceModifier.height(6.dp))
            val (pw, ph, d) = bitmapPx(context, DpSize(size.width - 28.dp, size.height - 28.dp - 52.dp))
            val bmp = WidgetBitmaps.streakRow(pw, ph, data.weekStart, data.today, data.achieved, data.color.toInt(), p, d)
            Image(ImageProvider(bmp), contentDescription = "이번 주 달성 흐름", modifier = GlanceModifier.fillMaxSize())
        }
    }
}

class StreakFlowReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StreakFlowWidget() }

// ============================================================ 7. 일일 달성량 (4x2)

class DailyRatioWidget : DataWidget<Triple<WContext, LocalDate, List<Float?>>>() {
    override suspend fun load(context: Context, target: String?) = WidgetDataSource.weekRatios(context)

    @Composable
    override fun Content(context: Context, data: Triple<WContext, LocalDate, List<Float?>>) {
        val (w, ws, ratios) = data
        val size = LocalSize.current
        WidgetFrame(openApp(context, "home"), padding = 14.dp) {
            WText("일일 달성량", 12.sp, WColors.sub)
            WText("${dayLabel(ws)} - ${dayLabel(ws.plusDays(6))}", 15.sp, bold = true)
            Spacer(GlanceModifier.height(6.dp))
            val (pw, ph, d) = bitmapPx(context, DpSize(size.width - 28.dp, size.height - 28.dp - 44.dp))
            val bmp = WidgetBitmaps.ringRow(pw, ph, ws, w.today, ratios, WColors.palette(context), d)
            Image(ImageProvider(bmp), contentDescription = "요일별 달성률", modifier = GlanceModifier.fillMaxSize())
        }
    }
}

class DailyRatioReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DailyRatioWidget() }

// ============================================================ 8. 주간 타임라인 (4x2)

class WeeklyTimelineWidget : DataWidget<Triple<WContext, LocalDate, List<WWeekRow>>>() {
    override suspend fun load(context: Context, target: String?) = WidgetDataSource.weeklyTimeline(context, target)

    @Composable
    override fun Content(context: Context, data: Triple<WContext, LocalDate, List<WWeekRow>>) {
        val (w, ws, rows) = data
        val target = currentState<Preferences>().target()
        val accent = if (WColors.isNight(context)) 0xFF3DDC97 else 0xFF13B57A
        val todayIdx = java.time.temporal.ChronoUnit.DAYS.between(ws, w.today).toInt()
        WidgetFrame(openApp(context, "timeline"), padding = 14.dp) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WText(if (target == null) "전체 루틴" else WidgetDataSource.tagLabel(w, target), 15.sp, bold = true, modifier = GlanceModifier.defaultWeight())
                WText("${ws.format(MD)} - ${w.today.format(MD)}", 12.sp, WColors.sub)
            }
            Spacer(GlanceModifier.height(6.dp))
            if (rows.isEmpty()) {
                WText("이번 주 해당하는 루틴이 없어요", 12.sp, WColors.sub)
                return@WidgetFrame
            }
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(rows, itemId = { (it.name + it.emoji).hashCode().toLong() }) { r ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(GlanceModifier.width(96.dp), verticalAlignment = Alignment.CenterVertically) {
                            ColorBar(r.color, 12.dp)
                            Spacer(GlanceModifier.width(5.dp))
                            WText("${r.emoji}${r.name}", 12.sp)
                        }
                        r.cells.forEachIndexed { i, cs ->
                            val bg = when (cs) {
                                CellState.DONE -> WColors.fixed(r.color)
                                CellState.PARTIAL -> WColors.fixed((r.color and 0x00FFFFFF) or 0x80000000)
                                CellState.MISSED, CellState.FUTURE -> WColors.cell
                                CellState.NOT_DUE -> WColors.bg
                            }
                            Box(GlanceModifier.defaultWeight().height(18.dp).padding(horizontal = 1.5.dp)) {
                                if (i == todayIdx) {
                                    Box(GlanceModifier.fillMaxSize().cornerRadius(4.dp).background(WColors.fixed(accent)).padding(1.5.dp)) {
                                        Box(GlanceModifier.fillMaxSize().cornerRadius(3.dp).background(if (cs == CellState.NOT_DUE) WColors.cell else bg)) {}
                                    }
                                } else {
                                    Box(GlanceModifier.fillMaxSize().cornerRadius(4.dp).background(bg)) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class WeeklyTimelineReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeeklyTimelineWidget() }

