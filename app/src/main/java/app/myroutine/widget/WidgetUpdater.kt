package app.myroutine.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.myroutine.di.AppEntryPoint
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    val all: List<GlanceAppWidget> by lazy {
        listOf(
            TodayRoutinesWidget(), TodayTodosWidget(), MonthTimelineWidget(),
            GraphLineWidget(), GraphBarWidget(), StreakFlowWidget(), DailyRatioWidget(), WeeklyTimelineWidget(),
        )
    }

    /** 리시버 클래스 이름 → 해당 위젯 (설정 화면에서 그 위젯만 갱신할 때). */
    fun forReceiver(className: String?): GlanceAppWidget? = when (className?.substringAfterLast('.')) {
        "TodayRoutinesReceiver" -> TodayRoutinesWidget()
        "TodayTodosReceiver" -> TodayTodosWidget()
        "MonthTimelineReceiver" -> MonthTimelineWidget()
        "GraphLineReceiver" -> GraphLineWidget()
        "GraphBarReceiver" -> GraphBarWidget()
        "StreakFlowReceiver" -> StreakFlowWidget()
        "DailyRatioReceiver" -> DailyRatioWidget()
        "WeeklyTimelineReceiver" -> WeeklyTimelineWidget()
        else -> null
    }

    /** 모든 위젯의 버전을 올리고 다시 그린다. */
    suspend fun updateAll(context: Context) {
        val mgr = GlanceAppWidgetManager(context)
        val v = System.currentTimeMillis()
        all.forEach { widget ->
            runCatching {
                mgr.getGlanceIds(widget.javaClass).forEach { id ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { it.toMutablePreferences().apply { this[WidgetKeys.VERSION] = v } }
                    widget.update(context, id)
                }
            }
        }
    }
}

/**
 * 하루 시작 시각(설정값)에 위젯 날짜를 넘긴다. 실행할 때마다 다음 날 같은 시각으로 다시 예약.
 * 알림 재등록도 여기서 같이 한다(Phase 6).
 */
class DayRolloverWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.updateAll(applicationContext)
        app.myroutine.notification.ReminderScheduler.rescheduleAll(applicationContext)
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "day-rollover"

        suspend fun schedule(context: Context) {
            val ep = AppEntryPoint.get(context)
            val hour = ep.settings().current().dayStartHour
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(hour, 0, 30)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMillis()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NAME, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DayRolloverWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build(),
            )
        }
    }
}
