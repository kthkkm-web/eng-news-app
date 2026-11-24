package app.myroutine.ui.templates

import android.content.Context
import app.myroutine.core.newId
import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.ui.edit.EditState
import app.myroutine.ui.edit.RuleDraft
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TemplateFile(val categories: List<TemplateCategory>)

@Serializable
data class TemplateCategory(val key: String, val title: String, val emoji: String, val items: List<RoutineTemplate>)

/**
 * period: daily | weekdays | weekend | weekly(days) | anytime(target=주 N회) | monthly(monthDays)
 * days: "MON,WED,FRI"
 */
@Serializable
data class RoutineTemplate(
    val key: String,
    val emoji: String,
    val name: String,
    val period: String = "daily",
    val days: String? = null,
    val monthDays: List<Int> = emptyList(),
    val target: Int = 1,
    val time: String? = null,
    val tag: String? = null,
    val numeric: Boolean = false,
    val unit: String? = null,
) {
    /** 칩에 표시할 문구: "매일 물 2L 마시기" 형태 */
    val label: String get() = buildString {
        append(
            when (period) {
                "weekdays" -> "평일 "
                "weekend" -> "주말 "
                "weekly", "anytime" -> "매주 "
                "monthly" -> "매달 "
                else -> "매일 "
            },
        )
        append(name)
        if (period == "anytime" && target > 1) append(" ${target}회")
    }
}

@Singleton
class TemplateRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    val categories: List<TemplateCategory> by lazy {
        runCatching {
            context.assets.open("templates.json").bufferedReader().use { json.decodeFromString(TemplateFile.serializer(), it.readText()).categories }
        }.getOrDefault(emptyList())
    }

    fun find(key: String): RoutineTemplate? = categories.asSequence().flatMap { it.items }.firstOrNull { it.key == key }

    fun toRule(t: RoutineTemplate): RepeatRule {
        val times = t.time?.let { listOf(java.time.LocalTime.parse(it)) }.orEmpty()
        val base = RepeatRule(newId(), timeEnabled = times.isNotEmpty(), times = times, target = t.target.coerceAtLeast(1))
        return when (t.period) {
            "weekdays" -> base.copy(period = Period.WEEKLY, weekdays = RepeatRule.WEEKDAYS)
            "weekend" -> base.copy(period = Period.WEEKLY, weekdays = RepeatRule.WEEKEND)
            "weekly" -> base.copy(period = Period.WEEKLY, weekdays = parseDays(t.days))
            "anytime" -> base.copy(period = Period.WEEKLY, weeklyAnytime = true)
            "monthly" -> base.copy(period = Period.MONTHLY, monthDays = t.monthDays.ifEmpty { listOf(1) }.toSet())
            else -> base
        }
    }

    /** 템플릿 값을 편집 상태에 채운다. 태그는 이름이 같은 기존 태그에 연결. */
    fun applyTo(t: RoutineTemplate, s: EditState): EditState {
        val match = s.allTags.firstOrNull { it.name == t.tag }
        return s.copy(
        name = t.name,
        emoji = t.emoji,
        recordType = if (t.numeric) RecordType.NUMERIC else RecordType.COUNT,
        unit = t.unit.orEmpty(),
        rules = listOf(RuleDraft(toRule(t), expanded = true)),
        templateKey = t.key,
        tagIds = if (match != null) setOf(match.id) else s.tagIds,
        pendingTagName = if (match == null) t.tag else null,
        )
    }

    private fun parseDays(s: String?): Set<DayOfWeek> =
        s.orEmpty().split(',').mapNotNull { d -> DayOfWeek.entries.firstOrNull { it.name.startsWith(d.trim().uppercase()) && d.isNotBlank() } }.toSet()
}
