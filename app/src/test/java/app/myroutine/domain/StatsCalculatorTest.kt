package app.myroutine.domain

import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.stats.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsCalculatorTest {
    private val start = LocalDate.of(2026, 11, 1) // 일요일
    private val daily = Routine("r", "x", "", "basic:0", startDate = start, rules = listOf(RepeatRule("d", Period.DAILY, target = 2)))
    private fun log(day: Int, c: Int) = RoutineLog("l$day", "r", LocalDate.of(2026, 11, day), c)

    @Test fun streaks_and_month_rate() {
        // 1~3 완료, 4 부분, 5~7 완료, 8(오늘) 미기록
        val logs = listOf(log(1, 2), log(2, 2), log(3, 3), log(4, 1), log(5, 2), log(6, 2), log(7, 2))
        val s = StatsCalculator.stats(daily, logs, LocalDate.of(2026, 11, 8), DayOfWeek.SUNDAY)
        assertEquals(3, s.currentStreak)  // 오늘 미완료는 끊지 않음
        assertEquals(3, s.bestStreak)
        assertEquals(6, s.monthDone); assertEquals(8, s.monthDue)
        assertEquals(14, s.totalCount)
    }

    @Test fun today_done_extends_streak_and_non_due_days_do_not_break() {
        val weekdays = daily.copy(rules = listOf(RepeatRule("w", Period.WEEKLY, weekdays = RepeatRule.WEEKDAYS)))
        // 11/6 금, 11/7·8 주말(미해당), 11/9 월
        val logs = listOf(RoutineLog("a", "r", LocalDate.of(2026, 11, 5), 1), RoutineLog("b", "r", LocalDate.of(2026, 11, 6), 1), RoutineLog("c", "r", LocalDate.of(2026, 11, 9), 1))
        val s = StatsCalculator.stats(weekdays, logs, LocalDate.of(2026, 11, 9), DayOfWeek.SUNDAY)
        assertEquals(3, s.currentStreak)
    }

    @Test fun overall_ratio_future_is_null() {
        val m = StatsCalculator.overallRatios(listOf(daily), listOf(log(2, 1)), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 2), DayOfWeek.SUNDAY)
        assertEquals(0.5f, m[LocalDate.of(2026, 11, 2)]!!, 1e-6f)
        assertNull(m[LocalDate.of(2026, 11, 3)])
    }
}
