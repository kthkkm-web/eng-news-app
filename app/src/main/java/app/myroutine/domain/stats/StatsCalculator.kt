package app.myroutine.domain.stats

import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.schedule.ScheduleResolver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class RoutineStats(
    val currentStreak: Int,
    val bestStreak: Int,
    /** 이번 달(오늘까지) 해당일 중 완료 비율. 해당일 없으면 null. */
    val monthRate: Float?,
    val monthDone: Int,
    val monthDue: Int,
    /** 총 완료 횟수(횟수형: count 합, 수치형: 기록한 날 수). */
    val totalCount: Int,
    val totalDoneDays: Int,
)

/** 기록 → 통계. 순수 함수. */
object StatsCalculator {

    /** 날짜별 진행률. 해당 없는 날 = null. */
    fun dayProgress(routine: Routine, logs: List<RoutineLog>, from: LocalDate, to: LocalDate, weekStart: DayOfWeek): Map<LocalDate, Float?> {
        val byDate = logs.filter { it.routineId == routine.id }.associateBy { it.date }
        val mine = logs.filter { it.routineId == routine.id }
        val out = LinkedHashMap<LocalDate, Float?>()
        var d = from
        while (!d.isAfter(to)) {
            val s = ScheduleResolver.resolve(routine, d, mine, weekStart)
            out[d] = ScheduleResolver.progress(s, routine, byDate[d])
            d = d.plusDays(1)
        }
        return out
    }

    /** 여러 루틴의 날짜별 전체 달성률. 미래 날짜는 null. */
    fun overallRatios(routines: List<Routine>, logs: List<RoutineLog>, from: LocalDate, to: LocalDate, today: LocalDate, weekStart: DayOfWeek): Map<LocalDate, Float?> {
        val logsBy = logs.groupBy { it.routineId }
        val at = logs.associateBy { it.routineId to it.date }
        val out = LinkedHashMap<LocalDate, Float?>()
        var d = from
        while (!d.isAfter(to)) {
            if (d.isAfter(today)) { out[d] = null } else {
                val entries = routines.mapNotNull { r ->
                    val s = ScheduleResolver.resolve(r, d, logsBy[r.id].orEmpty(), weekStart)
                    if (!s.isDue) null else ScheduleResolver.countOf(r, at[r.id to d]) to s.target
                }
                out[d] = ScheduleResolver.dayRatio(entries)
            }
            d = d.plusDays(1)
        }
        return out
    }

    fun stats(routine: Routine, logs: List<RoutineLog>, today: LocalDate, weekStart: DayOfWeek): RoutineStats {
        val mine = logs.filter { it.routineId == routine.id }
        val byDate = mine.associateBy { it.date }
        val end = minOf(today, routine.endDate ?: today)

        // 시작일~오늘 순회: 해당일만 본다. 미해당일은 연속을 끊지 않는다.
        var best = 0; var run = 0
        var d = routine.startDate
        val dueDays = ArrayList<Pair<LocalDate, Boolean>>()
        while (!d.isAfter(end)) {
            val s = ScheduleResolver.resolve(routine, d, mine, weekStart)
            if (s.isDue) {
                val done = ScheduleResolver.isComplete(s, routine, byDate[d])
                dueDays += d to done
                if (done) { run++; best = maxOf(best, run) } else if (d != today) run = 0
            }
            d = d.plusDays(1)
        }
        // 현재 연속: 오늘이 아직 미완료면 어제까지 기준
        var current = 0
        for ((date, done) in dueDays.asReversed()) {
            if (done) current++ else if (date == today) continue else break
        }

        val ym = YearMonth.from(today)
        val monthDays = dueDays.filter { YearMonth.from(it.first) == ym }
        val monthDone = monthDays.count { it.second }
        val total = mine.sumOf { ScheduleResolver.countOf(routine, it) }
        return RoutineStats(
            currentStreak = current,
            bestStreak = best,
            monthRate = if (monthDays.isEmpty()) null else monthDone.toFloat() / monthDays.size,
            monthDone = monthDone,
            monthDue = monthDays.size,
            totalCount = total,
            totalDoneDays = dueDays.count { it.second },
        )
    }

    /** 수치형 루틴의 (날짜, 값) 목록. 날짜 오름차순. */
    fun numericSeries(routine: Routine, logs: List<RoutineLog>): List<Pair<LocalDate, Double>> =
        logs.filter { it.routineId == routine.id && it.value != null }.sortedBy { it.date }.map { it.date to it.value!! }
}
