package app.myroutine.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.myroutine.core.TimeProvider
import app.myroutine.core.newId
import app.myroutine.data.settings.SettingsRepository
import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.Tag
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.repository.TagRepository
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.ui.templates.TemplateRepository
import app.myroutine.ui.theme.RoutinePalettes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RuleDraft(val rule: RepeatRule, val expanded: Boolean = false)

data class EditState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val id: String = newId(),
    val name: String = "",
    val emoji: String = "",
    val color: String = RoutinePalettes.DEFAULT_KEY,
    val goalEnabled: Boolean = true,
    val recordType: RecordType = RecordType.COUNT,
    val unit: String = "",
    val decimals: Int = 1,
    val rules: List<RuleDraft> = listOf(RuleDraft(RepeatRule(newId()), expanded = true)),
    val quickRecord: Boolean = false,
    val tagIds: Set<String> = emptySet(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val templateKey: String? = null,
    val createdAt: Long = 0,
    val allTags: List<Tag> = emptyList(),
    /** 템플릿 태그 이름: 태그 목록이 로드되면 연결 후 비움. */
    val pendingTagName: String? = null,
    val error: String? = null,
) {
    fun toRoutine() = Routine(
        id = id, name = name.trim(), emoji = emoji, color = color, recordType = recordType,
        unit = unit.trim().ifBlank { null }, decimals = decimals, goalEnabled = goalEnabled, quickRecord = quickRecord,
        startDate = startDate, endDate = endDate, sortOrder = sortOrder, archived = archived, templateKey = templateKey,
        tagIds = tagIds.toList(), rules = rules.map { it.rule },
    )
}

sealed interface EditEvent { data object Saved : EditEvent }

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val routines: RoutineRepository,
    private val tags: TagRepository,
    private val settings: SettingsRepository,
    private val templates: TemplateRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val routineId: String? = savedState.get<String>("id")?.takeIf { it.isNotBlank() }
    private val templateKey: String? = savedState.get<String>("template")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state
    private val events = Channel<EditEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            tags.observeTags().collect { t ->
                _state.update { s ->
                    val pending = s.pendingTagName
                    val match = pending?.let { name -> t.firstOrNull { it.name == name } }
                    if (match != null) s.copy(allTags = t, tagIds = s.tagIds + match.id, pendingTagName = null) else s.copy(allTags = t)
                }
            }
        }
        viewModelScope.launch {
            val today = ScheduleResolver.logicalDate(time.nowDateTime(), settings.current().dayStartHour)
            val existing = routineId?.let { routines.getRoutine(it) }
            if (existing != null) {
                _state.update { s ->
                    s.copy(
                        loading = false, isNew = false, id = existing.id, name = existing.name, emoji = existing.emoji,
                        color = existing.color, goalEnabled = existing.goalEnabled, recordType = existing.recordType,
                        unit = existing.unit.orEmpty(), decimals = existing.decimals,
                        rules = existing.rules.map { RuleDraft(it) }.ifEmpty { listOf(RuleDraft(RepeatRule(newId()), true)) },
                        quickRecord = existing.quickRecord, tagIds = existing.tagIds.toSet(), startDate = existing.startDate,
                        endDate = existing.endDate, sortOrder = existing.sortOrder, archived = existing.archived,
                        templateKey = existing.templateKey,
                    )
                }
            } else {
                val count = routines.observeRoutines().first().size
                val tpl = templateKey?.let { templates.find(it) }
                _state.update { cur ->
                    val base = cur.copy(loading = false, startDate = today, color = RoutinePalettes.defaultFor(count))
                    if (tpl != null) templates.applyTo(tpl, base) else base
                }
            }
        }
    }

    fun update(block: (EditState) -> EditState) = _state.update { block(it).copy(error = null) }

    fun updateRule(index: Int, block: (RepeatRule) -> RepeatRule) = update { s ->
        s.copy(rules = s.rules.mapIndexed { i, d -> if (i == index) d.copy(rule = block(d.rule)) else d })
    }

    fun toggleRuleExpanded(index: Int) = update { s ->
        s.copy(rules = s.rules.mapIndexed { i, d -> if (i == index) d.copy(expanded = !d.expanded) else d })
    }

    fun addRule() = update { s ->
        s.copy(rules = s.rules.map { it.copy(expanded = false) } + RuleDraft(RepeatRule(newId()), expanded = true))
    }

    fun removeRule(index: Int) = update { s ->
        if (s.rules.size <= 1) s.copy(error = "반복 주기는 최소 1개가 필요해요")
        else s.copy(rules = s.rules.filterIndexed { i, _ -> i != index })
    }

    fun toggleTag(id: String?) = update { s ->
        when {
            id == null -> s.copy(tagIds = emptySet())
            id in s.tagIds -> s.copy(tagIds = s.tagIds - id)
            else -> s.copy(tagIds = s.tagIds + id)
        }
    }

    fun createTag(name: String, emoji: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val id = tags.saveTag(Tag(newId(), name.trim(), emoji.trim()))
        _state.update { it.copy(tagIds = it.tagIds + id) }
    }

    fun save() {
        val s = _state.value
        val err = validate(s)
        if (err != null) { _state.update { it.copy(error = err) }; return }
        viewModelScope.launch {
            routines.saveRoutine(s.toRoutine())
            events.send(EditEvent.Saved)
        }
    }

    private fun validate(s: EditState): String? {
        if (s.name.isBlank()) return "루틴 이름을 입력해 주세요"
        if (s.endDate != null && s.endDate.isBefore(s.startDate)) return "종료일이 시작일보다 빨라요"
        s.rules.forEachIndexed { i, d ->
            val r = d.rule
            val n = if (s.rules.size > 1) "${i + 1}번째 반복 주기: " else ""
            when {
                r.period == Period.WEEKLY && !r.weeklyAnytime && r.weekdays.isEmpty() -> return "${n}요일을 선택해 주세요"
                r.period == Period.MONTHLY && r.monthDays.isEmpty() -> return "${n}날짜를 선택해 주세요"
                r.period == Period.YEARLY && r.yearDates.isEmpty() -> return "${n}날짜를 추가해 주세요"
                r.period == Period.CUSTOM && r.customDates.isEmpty() -> return "${n}날짜를 추가해 주세요"
                r.timeEnabled && r.times.isEmpty() -> return "${n}시간을 추가하거나 시간 지정을 꺼 주세요"
            }
        }
        return null
    }
}
