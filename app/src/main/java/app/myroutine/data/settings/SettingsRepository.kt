package app.myroutine.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

enum class ViewMode { CARD, GRID, COMPACT, CHECK;
    fun next(): ViewMode = entries[(ordinal + 1) % entries.size]
}
enum class GroupMode { DEFAULT, TAG, MERGED }
enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val viewMode: ViewMode = ViewMode.CARD,
    val groupMode: GroupMode = GroupMode.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    /** 0~6. 이 시각 이전 기록은 전날로 본다. */
    val dayStartHour: Int = 0,
    val notificationsEnabled: Boolean = true,
    val completedToBottom: Boolean = false,
    val showArchived: Boolean = false,
    val todosExpanded: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val VIEW = stringPreferencesKey("view_mode")
        val GROUP = stringPreferencesKey("group_mode")
        val THEME = stringPreferencesKey("theme_mode")
        val WEEK_START = intPreferencesKey("week_start")
        val DAY_START = intPreferencesKey("day_start_hour")
        val NOTI = booleanPreferencesKey("notifications_enabled")
        val DONE_BOTTOM = booleanPreferencesKey("completed_to_bottom")
        val SHOW_ARCHIVED = booleanPreferencesKey("show_archived")
        val TODOS_EXPANDED = booleanPreferencesKey("todos_expanded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            viewMode = p[K.VIEW].enumOr(ViewMode.CARD),
            groupMode = p[K.GROUP].enumOr(GroupMode.DEFAULT),
            themeMode = p[K.THEME].enumOr(ThemeMode.DARK),
            weekStart = DayOfWeek.of(p[K.WEEK_START] ?: DayOfWeek.SUNDAY.value),
            dayStartHour = p[K.DAY_START] ?: 0,
            notificationsEnabled = p[K.NOTI] ?: true,
            completedToBottom = p[K.DONE_BOTTOM] ?: false,
            showArchived = p[K.SHOW_ARCHIVED] ?: false,
            todosExpanded = p[K.TODOS_EXPANDED] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setViewMode(v: ViewMode) = context.dataStore.edit { it[K.VIEW] = v.name }
    suspend fun setGroupMode(v: GroupMode) = context.dataStore.edit { it[K.GROUP] = v.name }
    suspend fun setThemeMode(v: ThemeMode) = context.dataStore.edit { it[K.THEME] = v.name }
    suspend fun setWeekStart(v: DayOfWeek) = context.dataStore.edit { it[K.WEEK_START] = v.value }
    suspend fun setDayStartHour(v: Int) = context.dataStore.edit { it[K.DAY_START] = v.coerceIn(0, 6) }
    suspend fun setNotificationsEnabled(v: Boolean) = context.dataStore.edit { it[K.NOTI] = v }
    suspend fun setCompletedToBottom(v: Boolean) = context.dataStore.edit { it[K.DONE_BOTTOM] = v }
    suspend fun setShowArchived(v: Boolean) = context.dataStore.edit { it[K.SHOW_ARCHIVED] = v }
    suspend fun setTodosExpanded(v: Boolean) = context.dataStore.edit { it[K.TODOS_EXPANDED] = v }

    private inline fun <reified E : Enum<E>> String?.enumOr(default: E): E =
        this?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: default
}
