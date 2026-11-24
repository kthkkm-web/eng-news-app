package app.myroutine.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.myroutine.core.newId
import app.myroutine.data.backup.BackupFile
import app.myroutine.data.backup.BackupManager
import app.myroutine.data.backup.summary
import app.myroutine.data.settings.AppSettings
import app.myroutine.data.settings.SettingsRepository
import app.myroutine.data.settings.ThemeMode
import app.myroutine.data.settings.ViewMode
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.Tag
import app.myroutine.domain.model.User
import app.myroutine.domain.repository.AuthRepository
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    auth: AuthRepository,
    private val settingsRepo: SettingsRepository,
    private val tags: TagRepository,
    private val routines: RoutineRepository,
    private val backup: BackupManager,
) : ViewModel() {
    val user: StateFlow<User?> = auth.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val tagList: StateFlow<List<Tag>> = tags.observeTags().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val archived: StateFlow<List<Routine>> = routines.observeRoutines().map { l -> l.filter { it.archived } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** 복원 확인 대기 중인 파일. */
    val pendingRestore = MutableStateFlow<BackupFile?>(null)

    fun setNotifications(v: Boolean) = viewModelScope.launch { settingsRepo.setNotificationsEnabled(v) }
    fun setViewMode(v: ViewMode) = viewModelScope.launch { settingsRepo.setViewMode(v) }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(v) }
    fun setWeekStart(v: DayOfWeek) = viewModelScope.launch { settingsRepo.setWeekStart(v) }
    fun setDayStart(v: Int) = viewModelScope.launch { settingsRepo.setDayStartHour(v) }
    fun setCompletedToBottom(v: Boolean) = viewModelScope.launch { settingsRepo.setCompletedToBottom(v) }

    // 태그
    fun saveTag(tag: Tag) = viewModelScope.launch { tags.saveTag(tag) }
    fun addTag(name: String, emoji: String) = viewModelScope.launch { tags.saveTag(Tag(newId(), name, emoji)) }
    fun deleteTag(id: String) = viewModelScope.launch { tags.deleteTag(id) }
    fun reorderTags(ids: List<String>) = viewModelScope.launch { tags.reorder(ids) }

    // 보관함
    fun unarchive(id: String) = viewModelScope.launch { routines.setArchived(id, false) }
    fun deleteRoutine(id: String) = viewModelScope.launch { routines.deleteRoutine(id) }

    // 백업
    fun export(uri: Uri) = viewModelScope.launch {
        runCatching { backup.exportTo(uri) }
            .onSuccess { _messages.send("백업 완료 · 루틴 ${it.routines}개, 기록 ${it.logs}일") }
            .onFailure { _messages.send("백업 실패: ${it.message}") }
    }

    fun pickRestore(uri: Uri) = viewModelScope.launch {
        runCatching { backup.read(uri) }
            .onSuccess { pendingRestore.value = it }
            .onFailure { _messages.send(it.message ?: "파일을 읽을 수 없어요") }
    }

    fun confirmRestore() = viewModelScope.launch {
        val f = pendingRestore.value ?: return@launch
        pendingRestore.value = null
        runCatching { backup.restore(f) }
            .onSuccess { _messages.send("복원 완료 · 루틴 ${it.routines}개, 기록 ${it.logs}일") }
            .onFailure { _messages.send("복원 실패: ${it.message}") }
    }

    fun summaryOf(f: BackupFile) = f.summary()
}
