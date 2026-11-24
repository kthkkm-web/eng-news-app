package app.myroutine.domain

import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.schedule.ScheduleResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay

class ScheduleResolverTest {
    // 2026-11-22 = 일요일, 23 월, 25 수, 28 토
    private val sun = LocalDate.of(2026, 11, 22)
    private val mon = LocalDate.of(2026, 11, 23)
    private val wed = LocalDate.of(2026, 11, 25)
    private val sat = LocalDate.of(2026, 11, 28)

    private val weekdayRule = RepeatRule(
        id = "wd", period = Period.WEEKLY, weekdays = RepeatRule.WEEKDAYS,
        timeEnabled = true, times = listOf(LocalTime.of(6, 50)), target = 3,
    )
    private val weekendRule = RepeatRule(
        id = "we", period = Period.WEEKLY, weekdays = RepeatRule.WEEKEND,
        timeEnabled = true, times = listOf(LocalTime.of(9, 0)), target = 1,
    )

    private fun routine(vararg rules: RepeatRule, start: LocalDate = LocalDate.of(2026, 11, 1), end: LocalDate? = null) =
        Routine(id = "r", name = "뉴스 읽기", emoji = "📰", color = "basic:2", startDate = start, endDate = end, rules = rules.toList())

    private fun log(date: LocalDate, count: Int) = RoutineLog("l-$date", "r", date, count)

    @Test fun weekday_and_weekend_rules_pick_correct_target_and_time() {
        val r = routine(weekendRule, weekdayRule)
        val w = ScheduleResolver.resolve(r, wed)
        assertTrue(w.isDue); assertEquals(3, w.target); assertEquals(listOf(LocalTime.of(6, 50)), w.times)
        val s = ScheduleResolver.resolve(r, sat)
        assertTrue(s.isDue); assertEquals(1, s.target); assertEquals(listOf(LocalTime.of(9, 0)), s.times)
    }

    @Test fun overlapping_rules_sum_targets_and_merge_times() {
        val daily = RepeatRule("d", Period.DAILY, timeEnabled = true, times = listOf(LocalTime.of(21, 0), LocalTime.of(6, 50)), target = 1)
        val s = ScheduleResolver.resolve(routine(weekdayRule, daily), wed)
        assertEquals(4, s.target)
        assertEquals(listOf(LocalTime.of(6, 50), LocalTime.of(21, 0)), s.times)
        assertEquals(listOf("wd", "d"), s.ruleIds)
        // 주말엔 daily 만
        assertEquals(1, ScheduleResolver.resolve(routine(weekdayRule, daily), sun).target)
    }

    @Test fun not_due_before_start_date() {
        val r = routine(weekdayRule, start = wed)
        assertFalse(ScheduleResolver.resolve(r, mon).isDue)
        assertTrue(ScheduleResolver.resolve(r, wed).isDue)
    }

    @Test fun not_due_after_end_date_but_due_on_end_date() {
        val r = routine(weekdayRule, end = mon)
        assertTrue(ScheduleResolver.resolve(r, mon).isDue)
        assertFalse(ScheduleResolver.resolve(r, wed).isDue)
    }

    @Test fun archived_or_no_rules_is_not_due() {
        assertFalse(ScheduleResolver.resolve(routine(weekdayRule).copy(archived = true), wed).isDue)
        assertFalse(ScheduleResolver.resolve(routine(), wed).isDue)
    }

    @Test fun weekly_anytime_due_until_weekly_count_reached() {
        val anytime = RepeatRule("a", Period.WEEKLY, weeklyAnytime = true, target = 3)
        val r = routine(anytime)
        // 아무 기록 없음 → 매일 해당, 목표 1, 중장기
        val s0 = ScheduleResolver.resolve(r, wed)
        assertTrue(s0.isDue); assertEquals(1, s0.target); assertTrue(s0.isLongTerm)

        // 일·월에 합계 3회 → 수요일엔 해당 없음
        val logs = listOf(log(sun, 1), log(mon, 2))
        assertFalse(ScheduleResolver.resolve(r, wed, logs).isDue)

        // 수요일 기록으로 3회를 채운 경우, 수요일 자신은 여전히 해당(완료 상태로 보임)
        val logs2 = listOf(log(sun, 1), log(mon, 1), log(wed, 1))
        assertTrue(ScheduleResolver.resolve(r, wed, logs2).isDue)
        assertTrue(ScheduleResolver.isComplete(ScheduleResolver.resolve(r, wed, logs2), r, log(wed, 1)))
        // 그 뒤 토요일은 해당 없음
        assertFalse(ScheduleResolver.resolve(r, sat, logs2).isDue)
    }

    @Test fun weekly_anytime_resets_on_week_start() {
        val r = routine(RepeatRule("a", Period.WEEKLY, weeklyAnytime = true, target = 1))
        val logs = listOf(log(sat, 1))
        // 일요일 시작 기준: 다음 주 일요일(29일)은 새 주 → 해당
        assertTrue(ScheduleResolver.resolve(r, LocalDate.of(2026, 11, 29), logs, DayOfWeek.SUNDAY).isDue)
        // 월요일 시작 기준: 29일(일)은 토요일과 같은 주 → 해당 없음
        assertFalse(ScheduleResolver.resolve(r, LocalDate.of(2026, 11, 29), logs, DayOfWeek.MONDAY).isDue)
    }

    @Test fun goal_disabled_forces_target_one() {
        val r = routine(weekdayRule).copy(goalEnabled = false)
        assertEquals(1, ScheduleResolver.resolve(r, wed).target)
    }

    @Test fun numeric_routine_completes_with_value() {
        val r = routine(RepeatRule("d", Period.DAILY, target = 3)).copy(recordType = RecordType.NUMERIC, unit = "kg")
        val s = ScheduleResolver.resolve(r, wed)
        assertEquals(1, s.target)
        assertFalse(ScheduleResolver.isComplete(s, r, RoutineLog("x", "r", wed, 0, null)))
        assertTrue(ScheduleResolver.isComplete(s, r, RoutineLog("x", "r", wed, 0, 52.1)))
    }

    @Test fun monthly_clamps_missing_day_to_month_end() {
        val rule = RepeatRule("m", Period.MONTHLY, monthDays = setOf(31))
        assertTrue(ScheduleResolver.matches(rule, LocalDate.of(2026, 11, 30)))
        assertFalse(ScheduleResolver.matches(rule, LocalDate.of(2026, 11, 29)))
        assertTrue(ScheduleResolver.matches(rule, LocalDate.of(2026, 12, 31)))
        assertFalse(ScheduleResolver.matches(rule, LocalDate.of(2026, 12, 30)))
    }

    @Test fun yearly_and_custom() {
        val y = RepeatRule("y", Period.YEARLY, yearDates = setOf(MonthDay.of(2, 29)))
        assertTrue(ScheduleResolver.matches(y, LocalDate.of(2027, 2, 28)))   // 평년 보정
        assertTrue(ScheduleResolver.matches(y, LocalDate.of(2028, 2, 29)))
        val c = RepeatRule("c", Period.CUSTOM, customDates = setOf(wed))
        assertTrue(ScheduleResolver.matches(c, wed)); assertFalse(ScheduleResolver.matches(c, mon))
    }

    @Test fun progress_and_day_ratio() {
        val r = routine(weekdayRule)
        val s = ScheduleResolver.resolve(r, wed)
        assertEquals(1f / 3, ScheduleResolver.progress(s, r, log(wed, 1))!!, 1e-6f)
        assertEquals(1f, ScheduleResolver.progress(s, r, log(wed, 5))!!, 1e-6f)
        assertNull(ScheduleResolver.progress(ScheduleResolver.resolve(r, sat), r, null))
        assertEquals(0.5f, ScheduleResolver.dayRatio(listOf(1 to 3, 5 to 1))!!, 1e-6f) // (1+1)/4
        assertNull(ScheduleResolver.dayRatio(emptyList()))
    }

    @Test fun logical_date_respects_day_start_hour() {
        val t = LocalDateTime.of(2026, 11, 25, 3, 59)
        assertEquals(LocalDate.of(2026, 11, 24), ScheduleResolver.logicalDate(t, 4))
        assertEquals(LocalDate.of(2026, 11, 25), ScheduleResolver.logicalDate(t.withHour(4).withMinute(0), 4))
        assertEquals(LocalDate.of(2026, 11, 25), ScheduleResolver.logicalDate(t, 0))
    }
}
