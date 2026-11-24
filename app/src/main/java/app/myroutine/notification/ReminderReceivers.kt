package app.myroutine.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.ui.theme.RoutinePalettes
import app.myroutine.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** goAsync 로 코루틴 작업 후 종료. */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try { block() } finally { pending.finish() }
    }
}

/** 예약 알림 발화. 이미 그날 목표를 채웠으면 보내지 않는다. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync { handle(context, intent) }

    companion object {
        /** 발화 처리. 알림을 보냈으면 true. */
        suspend fun handle(context: Context, intent: Intent): Boolean {
            val ep = AppEntryPoint.get(context)
            val st = ep.settings().current()
            val routineId = intent.getStringExtra(EXTRA_ROUTINE) ?: return false
            val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse) ?: return false
            val time = intent.getStringExtra(EXTRA_TIME).orEmpty()
            val pre = intent.getIntExtra(EXTRA_PRE, 0)
            try {
                if (!st.notificationsEnabled) return false
                val r = ep.routines().getRoutine(routineId) ?: return false
                if (r.archived) return false
                val logs = ep.routines().getLogs(ScheduleResolver.weekStartOf(date, st.weekStart), date).filter { it.routineId == routineId }
                val s = ScheduleResolver.resolve(r, date, logs, st.weekStart)
                if (!s.isDue) return false
                val count = ScheduleResolver.countOf(r, logs.firstOrNull { it.date == date })
                if (count >= s.target) return false // 이미 달성
                val progress = if (r.recordType == RecordType.NUMERIC) "기록하기" else "$count/${s.target}회"
                val text = if (pre > 0) "${pre}분 후 $time · $progress" else "$time · $progress"
                Notifications.show(
                    context, r.id, "${r.emoji}${r.name}", text, date.toString(),
                    canComplete = r.recordType == RecordType.COUNT, color = RoutinePalettes.argb(r.color).toInt(),
                )
                return true
            } finally {
                ReminderScheduler.rescheduleAll(context)
            }
        }

        const val EXTRA_ROUTINE = "routineId"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
        const val EXTRA_PRE = "pre"

        fun intent(context: Context, p: PlannedReminder) = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_ROUTINE, p.routineId)
            .putExtra(EXTRA_DATE, p.date.toString())
            .putExtra(EXTRA_TIME, "%02d:%02d".format(p.time.hour, p.time.minute))
            .putExtra(EXTRA_PRE, p.preMinutes)
    }
}

/** 알림의 "완료 +1" 버튼. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync { handle(context, intent) }

    companion object {
      suspend fun handle(context: Context, intent: Intent) {
        val routineId = intent.getStringExtra(ReminderReceiver.EXTRA_ROUTINE) ?: return
        val date = intent.getStringExtra(ReminderReceiver.EXTRA_DATE)?.let(LocalDate::parse) ?: return
        val ep = AppEntryPoint.get(context)
        val st = ep.settings().current()
        val r = ep.routines().getRoutine(routineId)
        if (r != null) {
            val logs = ep.routines().getLogs(ScheduleResolver.weekStartOf(date, st.weekStart), date).filter { it.routineId == routineId }
            val s = ScheduleResolver.resolve(r, date, logs, st.weekStart)
            val count = ScheduleResolver.countOf(r, logs.firstOrNull { it.date == date })
            if (count < s.target.coerceAtLeast(1)) {
                if (r.quickRecord) ep.routines().setCount(routineId, date, s.target) else ep.routines().addCount(routineId, date, 1)
            }
        }
        Notifications.cancel(context, routineId)
        WidgetUpdater.updateAll(context)
      }

        fun intent(context: Context, routineId: String, date: String) = Intent(context, ReminderActionReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_ROUTINE, routineId)
            .putExtra(ReminderReceiver.EXTRA_DATE, date)
    }
}

/** 재부팅·시간 변경·앱 업데이트·정확한 알람 권한 변경 시 재등록. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync {
        ReminderScheduler.rescheduleAll(context)
        app.myroutine.widget.DayRolloverWorker.schedule(context)
        WidgetUpdater.updateAll(context)
    }
}
