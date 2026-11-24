package app.myroutine.ui.home

import app.myroutine.data.settings.GroupMode
import app.myroutine.data.settings.ViewMode
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.model.Tag
import app.myroutine.domain.model.Todo
import app.myroutine.domain.schedule.DaySchedule
import java.time.DayOfWeek
import java.time.LocalDate

/** 카드형 하단의 과거 달성 표시. */
sealed interface HistoryMark {
    data object Missed : HistoryMark
    data class Partial(val progress: Float) : HistoryMark
    data object Done : HistoryMark
}

data class HomeItem(
    val routine: Routine,
    val schedule: DaySchedule,
    val log: RoutineLog?,
    val count: Int,
    val progress: Float,
    val complete: Boolean,
    val history: List<HistoryMark>,
    val periodLabel: String,
    val timeLabel: String?,
) {
    val id get() = routine.id
    val target get() = schedule.target
}

data class HomeGroup(val key: String, val title: String, val emoji: String, val items: List<HomeItem>) {
    val done get() = items.count { it.complete }
}

sealed interface HomeFilter {
    data object All : HomeFilter
    data object Incomplete : HomeFilter
    data object Complete : HomeFilter
    data class ByTag(val tagId: String) : HomeFilter
}

data class FilterChipInfo(val filter: HomeFilter, val label: String, val count: Int)

data class HomeUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    val dayRatios: Map<LocalDate, Float?> = emptyMap(),
    val groups: List<HomeGroup> = emptyList(),
    val filterChips: List<FilterChipInfo> = emptyList(),
    val filter: HomeFilter = HomeFilter.All,
    val viewMode: ViewMode = ViewMode.CARD,
    val groupMode: GroupMode = GroupMode.DEFAULT,
    val tags: List<Tag> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val todosExpanded: Boolean = false,
    val hasAnyRoutine: Boolean = false,
)

sealed interface HomeEvent {
    data class Snackbar(val message: String, val undo: (suspend () -> Unit)? = null) : HomeEvent
    data class AskNumeric(val item: HomeItem) : HomeEvent
}
