@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package app.myroutine

import android.app.Application
import androidx.work.Configuration
import app.myroutine.core.DataChanges
import app.myroutine.widget.DayRolloverWorker
import app.myroutine.widget.WidgetUpdater
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import app.myroutine.data.settings.SettingsRepository
import app.myroutine.notification.Notifications
import app.myroutine.notification.ReminderRefreshWorker
import app.myroutine.notification.ReminderScheduler
import app.myroutine.domain.repository.TagRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyRoutineApp : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().build()

    @Inject lateinit var tagRepository: TagRepository
    @Inject lateinit var dataChanges: DataChanges
    @Inject lateinit var settingsRepository: SettingsRepository

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { tagRepository.seedDefaultsIfEmpty() }
        // 기록이 바뀌면 위젯 즉시 갱신 (연속 입력은 묶어서 한 번)
        appScope.launch {
            dataChanges.events.debounce(400).collect {
                runCatching { WidgetUpdater.updateAll(this@MyRoutineApp) }
                runCatching { ReminderScheduler.rescheduleAll(this@MyRoutineApp) }
            }
        }
        // 알림 on/off, 하루 시작 시각, 주 시작 요일이 바뀌면 재예약
        appScope.launch {
            settingsRepository.settings.map { Triple(it.notificationsEnabled, it.dayStartHour, it.weekStart) }.distinctUntilChanged().collect {
                runCatching { ReminderScheduler.rescheduleAll(this@MyRoutineApp) }
                runCatching { DayRolloverWorker.schedule(this@MyRoutineApp) }
                runCatching { WidgetUpdater.updateAll(this@MyRoutineApp) }
            }
        }
        Notifications.ensureChannels(this)
        ReminderRefreshWorker.schedule(this)

    }
}
