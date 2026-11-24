package app.myroutine.domain

import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.notification.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderPlanTest {
    private val today = LocalDate.of(2026, 11, 25) // 수
    private val rule = RepeatRule("w", Period.WEEKLY, weekdays = RepeatRule.WEEKDAYS, timeEnabled = true,
        times = listOf(LocalTime.of(6, 50), LocalTime.of(21, 0)), target = 3, notify = true, preNotifyMinutes = 10)
    private val r = Routine("r", "뉴스", "📰", "basic:0", startDate = today.minusDays(5), rules = listOf(rule))

    @Test fun plans_future_times_with_pre_notify_for_today_and_tomorrow() {
        val now = LocalDateTime.of(2026, 11, 25, 7, 0)
        val p = ReminderScheduler.plan(listOf(r), emptyList(), today, now, 0, DayOfWeek.SUNDAY)
        // 오늘 06:40 은 지남 → 오늘 20:50, 내일 06:40, 20:50
        assertEquals(listOf(LocalDateTime.of(2026, 11, 25, 20, 50), LocalDateTime.of(2026, 11, 26, 6, 40), LocalDateTime.of(2026, 11, 26, 20, 50)), p.map { it.fireAt })
    }

    @Test fun skips_rules_without_notify_and_non_due_days() {
        val quiet = r.copy(rules = listOf(rule.copy(notify = false)))
        assertTrue(ReminderScheduler.plan(listOf(quiet), emptyList(), today, LocalDateTime.of(2026, 11, 25, 0, 0), 0, DayOfWeek.SUNDAY).isEmpty())
        // 금요일 → 내일 토요일은 평일 규칙 미해당
        val fri = LocalDate.of(2026, 11, 27)
        val p = ReminderScheduler.plan(listOf(r), emptyList(), fri, fri.atTime(22, 0), 0, DayOfWeek.SUNDAY)
        assertTrue(p.isEmpty())
    }

    @Test fun early_morning_time_belongs_to_next_calendar_day_with_day_start_hour() {
        val late = r.copy(rules = listOf(rule.copy(times = listOf(LocalTime.of(1, 0)), preNotifyMinutes = 0)))
        val p = ReminderScheduler.plan(listOf(late), emptyList(), today, LocalDateTime.of(2026, 11, 25, 12, 0), 4, DayOfWeek.SUNDAY)
        assertEquals(LocalDateTime.of(2026, 11, 26, 1, 0), p.first().fireAt)
        assertEquals(today, p.first().date)
    }

    @Test fun completed_weekly_anytime_is_not_planned() {
        val any = r.copy(rules = listOf(RepeatRule("a", Period.WEEKLY, weeklyAnytime = true, target = 1, timeEnabled = true, times = listOf(LocalTime.of(20, 0)), notify = true)))
        val logs = listOf(RoutineLog("l", "r", LocalDate.of(2026, 11, 23), 1))
        assertTrue(ReminderScheduler.plan(listOf(any), logs, today, today.atTime(8, 0), 0, DayOfWeek.SUNDAY).isEmpty())
    }
}
