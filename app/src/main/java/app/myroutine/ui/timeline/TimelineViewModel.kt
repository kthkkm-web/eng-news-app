package app.myroutine.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.myroutine.core.TimeProvider
import app.myroutine.data.settings.AppSettings
import app.myroutine.data.settings.SettingsRepository
import app.myroutine.domain.model.Routine
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.repository.TagRepository
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.domain.stats.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class TimelinePeriod(val label: String) { WEEK("1W"), MONTH("1M"), YEAR("1Y");
    fun next() = entries[(ordinal + 1) % entries.size]
}

data class TimelineCard(val routine: Routine, val label: String, val values: Map<LocalDate, Float?>)
data class TimelineGroup(val key: String, val title: String, val emoji: String, val cards: List<TimelineCard>)

data class TimelineUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    val period: TimelinePeriod = TimelinePeriod.MONTH,
    val showEnded: Boolean = false,
    val activeCount: Int = 0,
    val endedCount: Int = 0,
    val overall: Map<LocalDate, Float?> = emptyMap(),
    val groups: List<TimelineGroup> = emptyList(),
    val collapsed: Set<String> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    routines: RoutineRepository,
    tags: TagRepository,
    settingsRepo: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val period = MutableStateFlow(TimelinePeriod.MONTH)
    private val showEnded = MutableStateFlow(false)
    private val collapsed = MutableStateFlow<Set<String>>(emptySet())
    private val today = MutableStateFlow(LocalDate.now())
    private val settings = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        viewModelScope.launch { settings.collect { today.value = ScheduleResolver.logicalDate(time.nowDateTime(), it.dayStartHour) } }
    }

    private val logs = today.flatMapLatest { t -> routines.observeLogs(t.minusDays(380), t.plusDays(7)) }

    val state: StateFlow<TimelineUiState> = combine(
        combine(routines.observeRoutines(), logs, tags.observeTags(), settings) { r, l, t, s -> Quad(r, l, t, s) },
        period, showEnded, collapsed, today,
    ) { (all, logList, tagList, st), p, ended, coll, t ->
        val isEnded = { r: Routine -> r.archived || (r.endDate?.isBefore(t) == true) }
        val active = all.filter { !isEnded(it) }
        val endedList = all.filter(isEnded)
        val shown = if (ended) endedList else active

        val (from, to) = when (p) {
            TimelinePeriod.WEEK -> ScheduleResolver.weekStartOf(t, st.weekStart).let { it to it.plusDays(6) }
            TimelinePeriod.MONTH -> YearMonth.from(t).let { it.atDay(1) to it.atEndOfMonth() }
            TimelinePeriod.YEAR -> ScheduleResolver.weekStartOf(t, st.weekStart).minusWeeks(52) to t
        }
        // 진행률 계산은 보관 여부와 무관하게 보이도록 archived=false 사본으로 계산
        val cards = shown.map { r ->
            val calc = r.copy(archived = false)
            TimelineCard(r, RuleFormatter.labels(r.rules), StatsCalculator.dayProgress(calc, logList, from, minOf(to, t), st.weekStart))
        }
        val groups = tagList.map { tag -> TimelineGroup(tag.id, tag.name, tag.emoji, cards.filter { tag.id in it.routine.tagIds }) } +
            TimelineGroup("none", "태그 없음", "", cards.filter { c -> c.routine.tagIds.none { id -> tagList.any { it.id == id } } })

        val overallFrom = ScheduleResolver.weekStartOf(t, st.weekStart).minusWeeks(52)
        TimelineUiState(
            loading = false, today = t, weekStart = st.weekStart, period = p, showEnded = ended,
            activeCount = active.size, endedCount = endedList.size,
            overall = StatsCalculator.overallRatios(active, logList, overallFrom, t, t, st.weekStart),
            groups = groups.filter { it.cards.isNotEmpty() }, collapsed = coll,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())

    fun cyclePeriod() { period.value = period.value.next() }
    fun setShowEnded(v: Boolean) { showEnded.value = v }
    fun toggleGroup(key: String) { collapsed.value = collapsed.value.let { if (key in it) it - key else it + key } }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
