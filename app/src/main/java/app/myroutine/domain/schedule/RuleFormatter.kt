package app.myroutine.domain.schedule

import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

/** 반복 규칙 → 한국어 표시 문구. 순수 함수. */
object RuleFormatter {
    private val HM = DateTimeFormatter.ofPattern("HH:mm")

    fun dayName(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "월"; DayOfWeek.TUESDAY -> "화"; DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"; DayOfWeek.FRIDAY -> "금"; DayOfWeek.SATURDAY -> "토"; DayOfWeek.SUNDAY -> "일"
    }

    /** 카드 상단 "평일" 같은 짧은 라벨. */
    fun periodLabel(rule: RepeatRule): String = when (rule.period) {
        Period.DAILY -> "매일"
        Period.WEEKLY -> when {
            rule.weeklyAnytime -> "주중 아무때나"
            rule.weekdays.size == 7 -> "매일"
            rule.weekdays == RepeatRule.WEEKDAYS -> "평일"
            rule.weekdays == RepeatRule.WEEKEND -> "주말"
            rule.weekdays.isEmpty() -> "요일 미선택"
            else -> rule.weekdays.sortedBy { it.value }.joinToString(", ") { dayName(it) }
        }
        Period.MONTHLY -> if (rule.monthDays.isEmpty()) "매월" else "매월 " + rule.monthDays.sorted().joinToString(", ") + "일"
        Period.YEARLY -> if (rule.yearDates.isEmpty()) "매년" else "매년 " + rule.yearDates.sorted().joinToString(", ") { "${it.monthValue}/${it.dayOfMonth}" }
        Period.CUSTOM -> "지정일"
    }

    /** "평일 06:50 3회씩", "주중 아무때나 주 3회" */
    fun ruleText(rule: RepeatRule, goalEnabled: Boolean = true): String {
        val parts = mutableListOf(periodLabel(rule))
        if (rule.timeEnabled && rule.times.isNotEmpty()) parts += rule.times.sorted().joinToString(", ") { it.format(HM) }
        if (rule.period == Period.WEEKLY && rule.weeklyAnytime) parts += "주 ${rule.target}회"
        else if (goalEnabled) parts += "${rule.target}회씩"
        return parts.joinToString(" ")
    }

    fun summary(rules: List<RepeatRule>, goalEnabled: Boolean = true): String =
        rules.joinToString(" / ") { ruleText(it, goalEnabled) }

    fun summary(routine: Routine): String = summary(routine.rules, routine.goalEnabled)

    /** 여러 규칙 라벨: "주말 / 평일" */
    fun labels(rules: List<RepeatRule>): String = rules.map { periodLabel(it) }.distinct().joinToString(" / ")

    /** "1~3회" / "3회" — 규칙별 목표치 범위 */
    fun targetRange(routine: Routine): String {
        if (!routine.goalEnabled || routine.rules.isEmpty()) return ""
        val ts = routine.rules.map { it.target }
        val min = ts.min(); val max = ts.max()
        return if (min == max) "${min}회" else "$min~${max}회"
    }
}
