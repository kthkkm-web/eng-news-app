package app.myroutine.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.myroutine.core.TimeProvider
import app.myroutine.data.settings.AppSettings
import app.myroutine.data.settings.SettingsRepository
import app.myroutine.domain.model.Routine
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.domain.stats.RoutineStats
import app.myroutine.domain.stats.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class DetailUiState(
    val routine: Routine? = null,
    val stats: RoutineStats? = null,
    val month: YearMonth = YearMonth.now(),
    val monthValues: Map<LocalDate, Float?> = emptyMap(),
    val series: List<Pair<LocalDate, Double>> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    val deleted: Boolean = false,
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val routines: RoutineRepository,
    settingsRepo: SettingsRepository,
    time: TimeProvider,
) : ViewModel() {
    private val id: String = checkNotNull(savedState["id"])
    private val month = MutableStateFlow(YearMonth.now())
    private val settings = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val state: StateFlow<DetailUiState> = combine(
        routines.observeRoutines().map { list -> list.firstOrNull { it.id == id } },
        routines.observeLogsOf(id),
        month,
        settings,
    ) { r, logs, m, st ->
        if (r == null) return@combine DetailUiState(deleted = true)
        val today = ScheduleResolver.logicalDate(time.nowDateTime(), st.dayStartHour)
        val calc = r.copy(archived = false)
        DetailUiState(
            routine = r,
            stats = StatsCalculator.stats(calc, logs, today, st.weekStart),
            month = m,
            monthValues = StatsCalculator.dayProgress(calc, logs, m.atDay(1), minOf(m.atEndOfMonth(), today), st.weekStart),
            series = StatsCalculator.numericSeries(r, logs),
            today = today,
            weekStart = st.weekStart,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun shiftMonth(delta: Long) { month.value = month.value.plusMonths(delta) }
    fun setArchived(v: Boolean) = viewModelScope.launch { routines.setArchived(id, v) }
}
