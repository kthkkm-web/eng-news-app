package app.myroutine.domain

import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.schedule.RuleFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RuleFormatterTest {
    @Test fun summary_like_reference_screen() {
        val rules = listOf(
            RepeatRule("a", Period.WEEKLY, weekdays = RepeatRule.WEEKEND, timeEnabled = true, times = listOf(LocalTime.of(9, 0)), target = 1),
            RepeatRule("b", Period.WEEKLY, weekdays = RepeatRule.WEEKDAYS, timeEnabled = true, times = listOf(LocalTime.of(6, 50)), target = 3),
        )
        assertEquals("주말 09:00 1회씩 / 평일 06:50 3회씩", RuleFormatter.summary(rules))
        assertEquals("주말 / 평일", RuleFormatter.labels(rules))
    }

    @Test fun labels() {
        assertEquals("매일", RuleFormatter.ruleText(RepeatRule("x"), goalEnabled = false))
        assertEquals("월, 수, 금 2회씩", RuleFormatter.ruleText(RepeatRule("x", Period.WEEKLY, weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), target = 2)))
        assertEquals("주중 아무때나 주 3회", RuleFormatter.ruleText(RepeatRule("x", Period.WEEKLY, weeklyAnytime = true, target = 3)))
        assertEquals("매월 1, 15일 1회씩", RuleFormatter.ruleText(RepeatRule("x", Period.MONTHLY, monthDays = setOf(15, 1))))
    }
}
