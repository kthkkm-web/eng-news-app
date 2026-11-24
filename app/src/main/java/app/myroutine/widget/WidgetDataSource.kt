package app.myroutine.widget

import android.content.Context
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.model.Tag
import app.myroutine.domain.model.Todo
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.domain.stats.StatsCalculator
import app.myroutine.ui.theme.RoutinePalettes
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class WRoutineRow(
    val id: String, val emoji: String, val name: String, val color: Long, val time: String?,
    val count: Int, val target: Int, val complete: Boolean, val numeric: Boolean, val valueText: String?,
)

enum class CellState { DONE, PARTIAL, MISSED, NOT_DUE, FUTURE }

data class WWeekRow(val emoji: String, val name: String, val color: Long, val cells: List<CellState>)

data class WContext(val today: LocalDate, val weekStart: DayOfWeek, val routines: List<Routine>, val tags: List<Tag>)

/** 위젯용 데이터 계산. 모두 suspend, Room 에서 1회 읽기. */
object WidgetDataSource {

    suspend fun context(ctx: Context): WContext {
        val ep = AppEntryPoint.get(ctx)
        val st = ep.settings().current()
        val today = ScheduleResolver.logicalDate(ep.time().nowDateTime(), st.dayStartHour)
        return WContext(today, st.weekStart, ep.routines().observeRoutines().first().filter { !it.archived }, ep.tags().observeTags().first())
    }

    private suspend fun logs(ctx: Context, from: LocalDate, to: LocalDate): List<RoutineLog> =
        AppEntryPoint.get(ctx).routines().getLogs(from, to)

    private fun scoped(w: WContext, tagId: String?): List<Routine> =
        if (tagId.isNullOrBlank()) w.routines else w.routines.filter { tagId in it.tagIds }

    fun tagLabel(w: WContext, tagId: String?): String =
        w.tags.firstOrNull { it.id == tagId }?.let { "${it.emoji} ${it.name}".trim() } ?: "전체"

    // 1. 오늘의 루틴
    suspend fun todayRoutines(ctx: Context, tagId: String?): Pair<WContext, List<WRoutineRow>> {
        val w = context(ctx)
        val ws = ScheduleResolver.weekStartOf(w.today, w.weekStart)
        val all = logs(ctx, ws, w.today)
        val byR = all.groupBy { it.routineId }
        val rows = scoped(w, tagId).mapNotNull { r ->
            val s = ScheduleResolver.resolve(r, w.today, byR[r.id].orEmpty(), w.weekStart)
            if (!s.isDue) return@mapNotNull null
            val log = byR[r.id]?.firstOrNull { it.date == w.today }
            val count = ScheduleResolver.countOf(r, log)
            WRoutineRow(
                r.id, r.emoji, r.name, RoutinePalettes.argb(r.color), s.times.firstOrNull()?.let { "%02d:%02d".format(it.hour, it.minute) },
                count, s.target, count >= s.target, r.recordType == RecordType.NUMERIC,
                log?.value?.let { "%.${r.decimals}f".format(it) + r.unit.orEmpty() },
            ) to (s.times.firstOrNull()?.toSecondOfDay() ?: Int.MAX_VALUE)
        }.sortedWith(compareBy({ it.first.complete }, { it.second })).map { it.first }
        return w to rows
    }

    // 2. 오늘의 할 일
    suspend fun todayTodos(ctx: Context): Pair<LocalDate, List<Todo>> {
        val w = context(ctx)
        return w.today to AppEntryPoint.get(ctx).todos().getTodos(w.today)
    }

    // 3. 월간 타임라인 (전체 달성률)
    suspend fun monthOverall(ctx: Context): Triple<WContext, YearMonth, Map<LocalDate, Float?>> {
        val w = context(ctx)
        val m = YearMonth.from(w.today)
        val l = logs(ctx, m.atDay(1).minusDays(7), m.atEndOfMonth())
        return Triple(w, m, StatsCalculator.overallRatios(w.routines, l, m.atDay(1), m.atEndOfMonth(), w.today, w.weekStart))
    }

    // 4·5. 수치 그래프
    suspend fun numeric(ctx: Context, routineId: String?): Pair<Routine?, List<Pair<LocalDate, Double>>> {
        val w = context(ctx)
        val numericRoutines = w.routines.filter { it.recordType == RecordType.NUMERIC }
        val r = numericRoutines.firstOrNull { it.id == routineId } ?: numericRoutines.firstOrNull() ?: return null to emptyList()
        val l = logs(ctx, r.startDate.minusDays(1), w.today)
        return r to StatsCalculator.numericSeries(r, l)
    }

    // 6. 달성 흐름: 이번 주 날짜별 달성 여부(대상 루틴 모두 완료)
    data class Flow(val title: String, val weekStart: LocalDate, val today: LocalDate, val achieved: List<Boolean?>, val doneDays: Int, val color: Long)

    suspend fun streakFlow(ctx: Context, target: String?): Flow {
        val w = context(ctx)
        val ws = ScheduleResolver.weekStartOf(w.today, w.weekStart)
        val routine = w.routines.firstOrNull { it.id == target }
        val tag = w.tags.firstOrNull { it.id == target }
        val scope = when {
            routine != null -> listOf(routine)
            tag != null -> w.routines.filter { tag.id in it.tagIds }
            else -> w.routines
        }
        val title = routine?.let { "${it.emoji}${it.name}" } ?: tag?.let { "${it.emoji} ${it.name}".trim() } ?: "전체 루틴"
        val l = logs(ctx, ws, ws.plusDays(6))
        val ratios = StatsCalculator.overallRatios(scope, l, ws, ws.plusDays(6), w.today, w.weekStart)
        val achieved = (0L until 7L).map { i -> ratios[ws.plusDays(i)]?.let { it >= 1f } }
        val color = routine?.let { RoutinePalettes.argb(it.color) } ?: 0xFF3DDC97
        return Flow(title, ws, w.today, achieved, achieved.count { it == true }, color)
    }

    // 7. 일일 달성량
    suspend fun weekRatios(ctx: Context): Triple<WContext, LocalDate, List<Float?>> {
        val w = context(ctx)
        val ws = ScheduleResolver.weekStartOf(w.today, w.weekStart)
        val l = logs(ctx, ws, ws.plusDays(6))
        val r = StatsCalculator.overallRatios(w.routines, l, ws, ws.plusDays(6), w.today, w.weekStart)
        return Triple(w, ws, (0L until 7L).map { r[ws.plusDays(it)] })
    }

    // 8. 주간 타임라인
    suspend fun weeklyTimeline(ctx: Context, tagId: String?): Triple<WContext, LocalDate, List<WWeekRow>> {
        val w = context(ctx)
        val ws = ScheduleResolver.weekStartOf(w.today, w.weekStart)
        val l = logs(ctx, ws, ws.plusDays(6))
        val rows = scoped(w, tagId).mapNotNull { r ->
            val p = StatsCalculator.dayProgress(r, l, ws, ws.plusDays(6), w.weekStart)
            val cells = (0L until 7L).map { i ->
                val d = ws.plusDays(i); val v = p[d]
                when {
                    d.isAfter(w.today) -> if (v == null) CellState.NOT_DUE else CellState.FUTURE
                    v == null -> CellState.NOT_DUE
                    v >= 1f -> CellState.DONE
                    v > 0f -> CellState.PARTIAL
                    else -> CellState.MISSED
                }
            }
            if (cells.all { it == CellState.NOT_DUE }) null else WWeekRow(r.emoji, r.name, RoutinePalettes.argb(r.color), cells)
        }
        return Triple(w, ws, rows)
    }
}
