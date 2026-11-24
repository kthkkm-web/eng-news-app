@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package app.myroutine

import android.app.Application
import app.myroutine.core.DataChanges
import app.myroutine.widget.DayRolloverWorker
import app.myroutine.widget.WidgetUpdater
import kotlinx.coroutines.flow.debounce
import app.myroutine.domain.repository.TagRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyRoutineApp : Application() {
    @Inject lateinit var tagRepository: TagRepository
    @Inject lateinit var dataChanges: DataChanges

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { tagRepository.seedDefaultsIfEmpty() }
        // 기록이 바뀌면 위젯 즉시 갱신 (연속 입력은 묶어서 한 번)
        appScope.launch {
            dataChanges.events.debounce(400).collect { runCatching { WidgetUpdater.updateAll(this@MyRoutineApp) } }
        }
        appScope.launch { runCatching { DayRolloverWorker.schedule(this@MyRoutineApp) } }
    }
}
