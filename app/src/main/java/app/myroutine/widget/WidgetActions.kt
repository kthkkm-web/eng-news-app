package app.myroutine.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.schedule.ScheduleResolver

/** 오늘의 루틴 행 탭: 앱을 열지 않고 +1 (빠른 기록이면 목표치까지). 수치형은 앱을 연다. */
class IncrementRoutineAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetKeys.ROUTINE_ID] ?: return
        val ep = AppEntryPoint.get(context)
        val routine = ep.routines().getRoutine(id) ?: return
        if (routine.recordType == RecordType.NUMERIC) {
            context.startActivity(DeepLink.intent(context, "home"))
            return
        }
        val st = ep.settings().current()
        val today = ScheduleResolver.logicalDate(ep.time().nowDateTime(), st.dayStartHour)
        val ws = ScheduleResolver.weekStartOf(today, st.weekStart)
        val logs = ep.routines().getLogs(ws, today).filter { it.routineId == id }
        val s = ScheduleResolver.resolve(routine, today, logs, st.weekStart)
        val count = ScheduleResolver.countOf(routine, logs.firstOrNull { it.date == today })
        if (!s.isDue || count >= s.target) return
        if (routine.quickRecord) ep.routines().setCount(id, today, s.target) else ep.routines().addCount(id, today, 1)
        WidgetUpdater.updateAll(context)
    }
}

class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetKeys.TODO_ID] ?: return
        AppEntryPoint.get(context).todos().toggleDone(id)
        WidgetUpdater.updateAll(context)
    }
}
