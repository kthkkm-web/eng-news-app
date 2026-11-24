package app.myroutine.data.db

import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.model.Tag
import app.myroutine.domain.model.Todo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.format.DateTimeFormatter

private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MD: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")

fun LocalDate.key(): String = toString()
fun String.toDate(): LocalDate = LocalDate.parse(this)
fun LocalTime.key(): String = format(HM)
fun String.toTime(): LocalTime = LocalTime.parse(this, HM)

fun Set<DayOfWeek>.toMask(): Int = fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
fun Int.toWeekdays(): Set<DayOfWeek> = DayOfWeek.entries.filter { this and (1 shl (it.value - 1)) != 0 }.toSet()

fun RoutineEntity.toDomain(rules: List<RepeatRuleEntity>, tagIds: List<String>) = Routine(
    id = id,
    name = name,
    emoji = emoji,
    color = color,
    recordType = runCatching { RecordType.valueOf(recordType) }.getOrDefault(RecordType.COUNT),
    unit = unit,
    decimals = decimals,
    goalEnabled = goalEnabled,
    quickRecord = quickRecord,
    startDate = startDate.toDate(),
    endDate = endDate?.toDate(),
    sortOrder = sortOrder,
    archived = archived,
    templateKey = templateKey,
    tagIds = tagIds,
    rules = rules.sortedBy { it.sortOrder }.map { it.toDomain() },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Routine.toEntity(userId: String, now: Long, createdAt: Long) = RoutineEntity(
    id = id, userId = userId, name = name, emoji = emoji, color = color,
    recordType = recordType.name, unit = unit, decimals = decimals, goalEnabled = goalEnabled,
    quickRecord = quickRecord, startDate = startDate.key(), endDate = endDate?.key(),
    sortOrder = sortOrder, archived = archived, templateKey = templateKey,
    createdAt = createdAt, updatedAt = now,
)

fun RepeatRuleEntity.toDomain() = RepeatRule(
    id = id,
    period = runCatching { Period.valueOf(period) }.getOrDefault(Period.DAILY),
    weekdays = weekdays.toWeekdays(),
    weeklyAnytime = weeklyAnytime,
    monthDays = monthDays.toSet(),
    yearDates = yearDates.map { MonthDay.parse("--$it") }.toSet(),
    customDates = customDates.map { it.toDate() }.toSet(),
    timeEnabled = timeEnabled,
    times = times.map { it.toTime() },
    target = target,
    notify = notify,
    preNotifyMinutes = preNotifyMinutes,
)

fun RepeatRule.toEntity(userId: String, routineId: String, order: Int, now: Long, createdAt: Long) = RepeatRuleEntity(
    id = id, userId = userId, routineId = routineId, sortOrder = order, period = period.name,
    weekdays = weekdays.toMask(), weeklyAnytime = weeklyAnytime, monthDays = monthDays.sorted(),
    yearDates = yearDates.sorted().map { it.format(MD) }, customDates = customDates.sorted().map { it.key() },
    timeEnabled = timeEnabled, times = times.sorted().map { it.key() }, target = target,
    notify = notify, preNotifyMinutes = preNotifyMinutes, createdAt = createdAt, updatedAt = now,
)

fun RoutineLogEntity.toDomain() = RoutineLog(id, routineId, date.toDate(), count, value, timestamps)

fun TagEntity.toDomain() = Tag(id, name, emoji, sortOrder)

fun TodoEntity.toDomain() = Todo(id, title, date.toDate(), time?.toTime(), done, sortOrder)
