package app.myroutine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.myroutine.core.TimeProvider
import app.myroutine.core.newId
import app.myroutine.data.settings.AppSettings
import app.myroutine.data.settings.GroupMode
import app.myroutine.data.settings.SettingsRepository
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.model.Tag
import app.myroutine.domain.model.Todo
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.repository.TagRepository
import app.myroutine.domain.repository.TodoRepository
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.domain.schedule.ScheduleResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routines: RoutineRepository,
    private val tagsRepo: TagRepository,
    private val todosRepo: TodoRepository,
    private val settingsRepo: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val todayFlow = MutableStateFlow(LocalDate.now())
    private val selected = MutableStateFlow<LocalDate?>(null) // null = 오늘 따라감
    private val filter = MutableStateFlow<HomeFilter>(HomeFilter.All)
    /** 드래그 중 임시 순서(손을 떼면 저장). */
    private val pendingOrder = MutableStateFlow<List<String>?>(null)

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    private val settings = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val selectedDate: Flow<LocalDate> = combine(selected, todayFlow) { s, t -> s ?: t }.distinctUntilChanged()

    /** 선택 주 기준 앞 5주 ~ 뒤 2주 기록. 주간 스트립 링, 카드 이력, 주중 아무때나 계산에 사용. */
    private val logsWindow: Flow<Pair<LocalDate, List<RoutineLog>>> =
        combine(selectedDate, settings.map { it.weekStart }.distinctUntilChanged()) { d, ws -> ScheduleResolver.weekStartOf(d, ws) }
            .distinctUntilChanged()
            .flatMapLatest { ws ->
                val from = ws.minusDays(42)
                routines.observeLogs(from, ws.plusDays(13)).map { from to it }
            }

    private val todos: Flow<List<Todo>> = selectedDate.flatMapLatest { todosRepo.observeTodos(it) }

    private data class Inputs(
        val routines: List<Routine>, val logs: List<RoutineLog>, val tags: List<Tag>,
        val todos: List<Todo>, val settings: AppSettings,
    )

    val state: StateFlow<HomeUiState> = combine(
        combine(routines.observeRoutines(), logsWindow, tagsRepo.observeTags(), todos, settings) { r, lw, tg, td, st -> Inputs(r, lw.second, tg, td, st) },
        selectedDate, todayFlow, filter, pendingOrder,
    ) { inp, sel, today, flt, order -> build(inp, sel, today, flt, order) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun build(inp: Inputs, sel: LocalDate, today: LocalDate, flt: HomeFilter, order: List<String>?): HomeUiState {
        val st = inp.settings
        val active = inp.routines.filter { !it.archived }
            .let { list -> if (order == null) list else list.sortedBy { order.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } } }
        val logsBy = inp.logs.groupBy { it.routineId }
        val logAt = inp.logs.associateBy { it.routineId to it.date }

        // 주간 스트립: 선택 주 ±1주 달성률
        val ws = ScheduleResolver.weekStartOf(sel, st.weekStart)
        val ratios = (-7L until 14L).associate { off ->
            val d = ws.plusDays(off)
            val entries = active.mapNotNull { r ->
                val s = ScheduleResolver.resolve(r, d, logsBy[r.id].orEmpty(), st.weekStart)
                if (!s.isDue) null else ScheduleResolver.countOf(r, logAt[r.id to d]) to s.target
            }
            d to (if (d.isAfter(today)) null else ScheduleResolver.dayRatio(entries))
        }

        val items = active.mapNotNull { r ->
            val rl = logsBy[r.id].orEmpty()
            val s = ScheduleResolver.resolve(r, sel, rl, st.weekStart)
            if (!s.isDue) return@mapNotNull null
            val log = logAt[r.id to sel]
            val count = ScheduleResolver.countOf(r, log)
            val applied = r.rules.filter { it.id in s.ruleIds }
            HomeItem(
                routine = r, schedule = s, log = log, count = count,
                progress = ScheduleResolver.progress(s, r, log) ?: 0f,
                complete = count >= s.target,
                history = history(r, sel, rl, logAt, st),
                periodLabel = RuleFormatter.labels(applied) + weeklySuffix(r, applied, rl, sel, st),
                timeLabel = s.times.firstOrNull()?.format(HM),
            )
        }

        val chips = buildList {
            add(FilterChipInfo(HomeFilter.All, "전체", items.size))
            add(FilterChipInfo(HomeFilter.Incomplete, "미완료", items.count { !it.complete }))
            add(FilterChipInfo(HomeFilter.Complete, "완료", items.count { it.complete }))
            inp.tags.forEach { t ->
                val n = items.count { t.id in it.routine.tagIds }
                if (n > 0) add(FilterChipInfo(HomeFilter.ByTag(t.id), "${t.emoji} ${t.name}".trim(), n))
            }
        }
        val effectiveFilter = if (chips.any { it.filter == flt }) flt else HomeFilter.All
        var shown = items.filter {
            when (val f = effectiveFilter) {
                HomeFilter.All -> true
                HomeFilter.Incomplete -> !it.complete
                HomeFilter.Complete -> it.complete
                is HomeFilter.ByTag -> f.tagId in it.routine.tagIds
            }
        }
        if (st.completedToBottom && order == null) shown = shown.sortedBy { it.complete }

        val dayTitle = if (sel == today) "오늘의 루틴" else sel.format(MD_TITLE) + " 루틴"
        val groups = when (st.groupMode) {
            GroupMode.DEFAULT -> listOf(
                HomeGroup("today", dayTitle, "🔥", shown.filter { !it.schedule.isLongTerm }),
                HomeGroup("long", "중장기 루틴", "🗓️", shown.filter { it.schedule.isLongTerm }),
            ).filter { it.items.isNotEmpty() }
            GroupMode.TAG -> inp.tags.map { t -> HomeGroup(t.id, t.name, t.emoji, shown.filter { t.id in it.routine.tagIds }) }
                .plus(HomeGroup("none", "태그 없음", "🏷️", shown.filter { r -> r.routine.tagIds.none { id -> inp.tags.any { it.id == id } } }))
                .filter { it.items.isNotEmpty() }
            GroupMode.MERGED -> listOf(HomeGroup("all", dayTitle, "🔥", shown)).filter { it.items.isNotEmpty() }
        }

        return HomeUiState(
            loading = false, today = today, selectedDate = sel, weekStart = st.weekStart, dayRatios = ratios,
            groups = groups, filterChips = chips, filter = effectiveFilter, viewMode = st.viewMode,
            groupMode = st.groupMode, tags = inp.tags, todos = inp.todos, todosExpanded = st.todosExpanded,
            hasAnyRoutine = active.isNotEmpty(),
        )
    }

    /** 주중 아무때나 규칙이면 " · 이번 주 1/3" */
    private fun weeklySuffix(r: Routine, applied: List<app.myroutine.domain.model.RepeatRule>, rl: List<RoutineLog>, sel: LocalDate, st: AppSettings): String {
        val rule = applied.firstOrNull { it.weeklyAnytime } ?: return ""
        val from = ScheduleResolver.weekStartOf(sel, st.weekStart)
        val done = rl.filter { !it.date.isBefore(from) && !it.date.isAfter(from.plusDays(6)) }.sumOf { ScheduleResolver.countOf(r, it) }
        return " · 이번 주 ${done.coerceAtMost(rule.target)}/${rule.target}"
    }

    /** 선택 날짜 이전, 해당했던 최근 3일(오래된 → 최근 순). */
    private fun history(
        r: Routine, sel: LocalDate, rl: List<RoutineLog>, logAt: Map<Pair<String, LocalDate>, RoutineLog>, st: AppSettings,
    ): List<HistoryMark> {
        val out = ArrayList<HistoryMark>(3)
        var d = sel.minusDays(1)
        var guard = 0
        while (out.size < 3 && guard < 40 && !d.isBefore(r.startDate)) {
            val s = ScheduleResolver.resolve(r, d, rl, st.weekStart)
            if (s.isDue) {
                val p = ScheduleResolver.progress(s, r, logAt[r.id to d]) ?: 0f
                out += when { p >= 1f -> HistoryMark.Done; p > 0f -> HistoryMark.Partial(p); else -> HistoryMark.Missed }
            }
            d = d.minusDays(1); guard++
        }
        return out.reversed()
    }

    // ---------- 입력 ----------

    fun refreshToday() {
        viewModelScope.launch {
            val h = settingsRepo.current().dayStartHour
            todayFlow.value = ScheduleResolver.logicalDate(time.nowDateTime(), h)
        }
    }

    fun selectDate(date: LocalDate) { selected.value = if (date == todayFlow.value) null else date }
    fun shiftWeek(weeks: Long) = selectDate(state.value.selectedDate.plusWeeks(weeks))
    fun setFilter(f: HomeFilter) { filter.value = f }
    fun cycleViewMode() = viewModelScope.launch { settingsRepo.setViewMode(state.value.viewMode.next()) }
    fun setGroupMode(m: GroupMode) = viewModelScope.launch { settingsRepo.setGroupMode(m) }
    fun setTodosExpanded(v: Boolean) = viewModelScope.launch { settingsRepo.setTodosExpanded(v) }

    private fun guardFuture(): Boolean {
        if (state.value.selectedDate.isAfter(todayFlow.value)) {
            events.trySend(HomeEvent.Snackbar("미래 날짜는 기록할 수 없어요"))
            return true
        }
        return false
    }

    private fun snapshotUndo(item: HomeItem): suspend () -> Unit {
        val date = state.value.selectedDate
        val prev = item.log
        return { routines.setLog(item.id, date, prev?.count ?: 0, prev?.value, prev?.timestamps.orEmpty()) }
    }

    /** 탭: +1 (빠른 기록이면 목표치까지 한 번에). */
    fun tap(item: HomeItem) {
        if (guardFuture()) return
        if (item.routine.recordType == RecordType.NUMERIC) { events.trySend(HomeEvent.AskNumeric(item)); return }
        if (item.complete) {
            events.trySend(HomeEvent.Snackbar("이미 완료했어요 · 오른쪽으로 밀면 취소돼요"))
            return
        }
        val date = state.value.selectedDate
        viewModelScope.launch {
            if (item.routine.quickRecord) routines.setCount(item.id, date, item.target)
            else routines.addCount(item.id, date, 1)
        }
    }

    /** 왼쪽 스와이프: 그날 목표치까지 완료. */
    fun swipeComplete(item: HomeItem) {
        if (guardFuture()) return
        if (item.routine.recordType == RecordType.NUMERIC) { events.trySend(HomeEvent.AskNumeric(item)); return }
        if (item.complete) return
        val undo = snapshotUndo(item)
        val date = state.value.selectedDate
        viewModelScope.launch {
            routines.setCount(item.id, date, item.target)
            events.send(HomeEvent.Snackbar("${item.routine.name} 완료!", undo))
        }
    }

    /** 오른쪽 스와이프: 그날 기록 취소(0). */
    fun swipeCancel(item: HomeItem) {
        if (item.count == 0 && item.log?.value == null) return
        val undo = snapshotUndo(item)
        val date = state.value.selectedDate
        viewModelScope.launch {
            routines.resetLog(item.id, date)
            events.send(HomeEvent.Snackbar("${item.routine.name} 기록을 취소했어요", undo))
        }
    }

    fun minusOne(item: HomeItem) = viewModelScope.launch { routines.addCount(item.id, state.value.selectedDate, -1) }

    fun resetLog(item: HomeItem) = swipeCancel(item)

    fun setNumeric(item: HomeItem, value: Double?) {
        val undo = snapshotUndo(item)
        viewModelScope.launch {
            routines.setValue(item.id, state.value.selectedDate, value)
            if (value == null) events.send(HomeEvent.Snackbar("기록을 지웠어요", undo))
        }
    }

    fun archive(item: HomeItem) = viewModelScope.launch {
        routines.setArchived(item.id, true)
        events.send(HomeEvent.Snackbar("보관했어요", { routines.setArchived(item.id, false) }))
    }

    fun delete(item: HomeItem) = viewModelScope.launch { routines.deleteRoutine(item.id) }

    fun runUndo(block: suspend () -> Unit) = viewModelScope.launch { block() }

    // ---------- 정렬 (드래그) ----------

    fun movePending(fromId: String, toId: String) {
        val current = pendingOrder.value ?: state.value.groups.flatMap { g -> g.items.map { it.id } }.distinct()
            .let { visible -> visible + (allRoutineIds() - visible.toSet()) }
        val from = current.indexOf(fromId); val to = current.indexOf(toId)
        if (from < 0 || to < 0) return
        pendingOrder.value = current.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun commitOrder() {
        val order = pendingOrder.value ?: return
        viewModelScope.launch {
            routines.reorder(order)
            pendingOrder.value = null
        }
    }

    private var cachedIds: List<String> = emptyList()
    private fun allRoutineIds(): List<String> = cachedIds

    init {
        refreshToday()
        viewModelScope.launch { routines.observeRoutines().collect { list -> cachedIds = list.map { it.id } } }
    }

    // ---------- 할 일 ----------

    fun addTodo(title: String, time: LocalTime?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            todosRepo.saveTodo(Todo(newId(), title, state.value.selectedDate, time, false, state.value.todos.size))
        }
    }
    fun toggleTodo(todo: Todo) = viewModelScope.launch { todosRepo.toggleDone(todo.id) }
    fun deleteTodo(todo: Todo) = viewModelScope.launch {
        todosRepo.deleteTodo(todo.id)
        events.send(HomeEvent.Snackbar("할 일을 삭제했어요", { todosRepo.saveTodo(todo) }))
    }
    fun updateTodo(todo: Todo) = viewModelScope.launch { todosRepo.saveTodo(todo) }

    companion object {
        private val HM = DateTimeFormatter.ofPattern("HH:mm")
        private val MD_TITLE = DateTimeFormatter.ofPattern("M월 d일")
    }
}
