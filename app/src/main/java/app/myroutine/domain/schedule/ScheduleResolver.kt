package app.myroutine.domain.schedule

import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.temporal.TemporalAdjusters

/** 특정 날짜에 대한 루틴의 일정 해석 결과. */
data class DaySchedule(
    val isDue: Boolean,
    /** 그날 달성해야 하는 횟수. isDue=false 이면 0. */
    val target: Int,
    /** 그날 알림/표시용 시각(정렬, 중복 제거). */
    val times: List<LocalTime>,
    /** "주중 아무 때나 N회" 처럼 주 단위 목표에서 온 일정이면 true (홈의 "중장기 루틴" 그룹). */
    val isLongTerm: Boolean,
    /** 그날 적용된 규칙 ID. */
    val ruleIds: List<String>,
) {
    companion object {
        val NOT_DUE = DaySchedule(false, 0, emptyList(), false, emptyList())
    }
}

/**
 * 반복 규칙 해석기. 순수 함수만 둔다(안드로이드 의존 없음) → 유닛 테스트 대상.
 *
 * 정한 규칙(CLAUDE.md "결정 사항" 참고):
 * - 같은 날 여러 규칙이 겹치면 목표치는 합산, 시각은 합집합.
 * - routine.goalEnabled=false 이면 목표치는 1.
 * - NUMERIC 루틴은 목표치 1 (값이 있으면 완료).
 * - "주중 아무 때나 N회": 그 주(weekStart 기준)의 다른 날 기록 합계가 이미 N 이상이면 그날은 해당 없음.
 *   아직 N 미만이면 매일 해당, 그날 목표 1.
 * - 월간 규칙에서 그 달에 없는 날짜(예: 31일)는 그 달의 말일로 취급.
 */
object ScheduleResolver {

    fun resolve(
        routine: Routine,
        date: LocalDate,
        logs: List<RoutineLog> = emptyList(),
        weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    ): DaySchedule {
        if (routine.archived) return DaySchedule.NOT_DUE
        if (date.isBefore(routine.startDate)) return DaySchedule.NOT_DUE
        val end = routine.endDate
        if (end != null && date.isAfter(end)) return DaySchedule.NOT_DUE

        var target = 0
        val times = sortedSetOf<LocalTime>()
        val ruleIds = mutableListOf<String>()
        var longTerm = false

        for (rule in routine.rules) {
            if (rule.period == Period.WEEKLY && rule.weeklyAnytime) {
                val needed = rule.target.coerceAtLeast(1)
                val doneElsewhere = weekLogs(logs, routine.id, date, weekStart)
                    .filter { it.date != date }
                    .sumOf { countOf(routine, it) }
                if (doneElsewhere >= needed) continue
                target += 1
                longTerm = true
            } else {
                if (!matches(rule, date)) continue
                target += rule.target.coerceAtLeast(1)
            }
            ruleIds += rule.id
            if (rule.timeEnabled) times += rule.times
        }

        if (ruleIds.isEmpty()) return DaySchedule.NOT_DUE
        if (!routine.goalEnabled || routine.recordType == RecordType.NUMERIC) target = 1
        return DaySchedule(true, target, times.toList(), longTerm, ruleIds)
    }

    /** 고정 일정 규칙이 해당 날짜에 맞는지 (주중 아무 때나 규칙은 여기서 다루지 않음). */
    fun matches(rule: RepeatRule, date: LocalDate): Boolean = when (rule.period) {
        Period.DAILY -> true
        Period.WEEKLY -> date.dayOfWeek in rule.weekdays
        Period.MONTHLY -> {
            val last = date.lengthOfMonth()
            rule.monthDays.any { d -> d.coerceAtMost(last) == date.dayOfMonth }
        }
        Period.YEARLY -> rule.yearDates.any { md -> md.atYearSafe(date.year) == date }
        Period.CUSTOM -> date in rule.customDates
    }

    /** 그날 기록이 그날 목표에 기여하는 값. */
    fun countOf(routine: Routine, log: RoutineLog?): Int {
        if (log == null) return 0
        return if (routine.recordType == RecordType.NUMERIC) (if (log.value != null) 1 else 0) else log.count
    }

    /** 0.0 ~ 1.0 진행률. 해당 없는 날은 null. */
    fun progress(schedule: DaySchedule, routine: Routine, log: RoutineLog?): Float? {
        if (!schedule.isDue || schedule.target <= 0) return null
        val c = countOf(routine, log).coerceAtMost(schedule.target)
        return c.toFloat() / schedule.target
    }

    fun isComplete(schedule: DaySchedule, routine: Routine, log: RoutineLog?): Boolean =
        schedule.isDue && countOf(routine, log) >= schedule.target

    /**
     * 여러 루틴의 하루 달성률 = Σmin(완료,목표) / Σ목표. 해당 루틴이 없으면 null.
     */
    fun dayRatio(entries: List<Pair<Int, Int>>): Float? {
        val total = entries.sumOf { it.second }
        if (total <= 0) return null
        val done = entries.sumOf { (c, t) -> c.coerceAtMost(t) }
        return done.toFloat() / total
    }

    /** 하루 시작 시각을 반영한 "논리적 오늘". 예: dayStartHour=4 이면 03:59 는 전날. */
    fun logicalDate(now: LocalDateTime, dayStartHour: Int): LocalDate =
        now.minusHours(dayStartHour.toLong()).toLocalDate()

    fun weekStartOf(date: LocalDate, weekStart: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(weekStart))

    private fun weekLogs(logs: List<RoutineLog>, routineId: String, date: LocalDate, weekStart: DayOfWeek): List<RoutineLog> {
        val from = weekStartOf(date, weekStart)
        val to = from.plusDays(6)
        return logs.filter { it.routineId == routineId && !it.date.isBefore(from) && !it.date.isAfter(to) }
    }

    private fun MonthDay.atYearSafe(year: Int): LocalDate = atYear(year) // 2/29 는 평년에 2/28 로 보정됨
}
