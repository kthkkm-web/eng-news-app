package app.myroutine.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.schedule.ScheduleResolver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/** 예약할 알림 1건. */
data class PlannedReminder(
    val routineId: String,
    /** 논리적 날짜(기록이 들어갈 날). */
    val date: LocalDate,
    val time: LocalTime,
    val fireAt: LocalDateTime,
    val preMinutes: Int,
) {
    val requestCode: Int get() = "$routineId|$date|$time|$preMinutes".hashCode()
}

/**
 * 오늘·내일(논리 날짜) 알림을 AlarmManager 로 예약한다.
 * - 정확한 알람 권한이 있으면 setExactAndAllowWhileIdle, 없으면 setAndAllowWhileIdle(수 분 오차 가능).
 * - 예약한 requestCode 를 SharedPreferences 에 저장해 다음 재예약 때 모두 취소한다.
 */
object ReminderScheduler {
    private const val PREFS = "reminders"
    private const val KEY_CODES = "codes"

    /** 순수 계산: 다음 알림 목록. (테스트 대상) */
    fun plan(
        routines: List<app.myroutine.domain.model.Routine>,
        logs: List<app.myroutine.domain.model.RoutineLog>,
        today: LocalDate,
        now: LocalDateTime,
        dayStartHour: Int,
        weekStart: java.time.DayOfWeek,
    ): List<PlannedReminder> {
        val out = mutableListOf<PlannedReminder>()
        for (offset in 0L..1L) {
            val date = today.plusDays(offset)
            for (r in routines) {
                if (r.archived) continue
                val s = ScheduleResolver.resolve(r, date, logs.filter { it.routineId == r.id }, weekStart)
                if (!s.isDue) continue
                val applied = r.rules.filter { it.id in s.ruleIds && it.notify && it.timeEnabled }
                for (rule in applied) for (t in rule.times) {
                    // 하루 시작 시각 이전 시각(예: 새벽 1시, 시작 4시)은 달력상 다음 날
                    val calDate = if (t.hour < dayStartHour) date.plusDays(1) else date
                    val fire = calDate.atTime(t).minusMinutes(rule.preNotifyMinutes.toLong())
                    if (fire.isAfter(now)) out += PlannedReminder(r.id, date, t, fire, rule.preNotifyMinutes)
                }
            }
        }
        return out.distinctBy { it.requestCode }.sortedBy { it.fireAt }
    }

    suspend fun rescheduleAll(context: Context) {
        val ep = AppEntryPoint.get(context)
        val st = ep.settings().current()
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // 기존 예약 취소
        prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.forEach { code ->
            PendingIntent.getBroadcast(context, code, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { am.cancel(it); it.cancel() }
        }
        if (!st.notificationsEnabled) { prefs.edit { putStringSet(KEY_CODES, emptySet()) }; return }

        val now = ep.time().nowDateTime()
        val today = ScheduleResolver.logicalDate(now, st.dayStartHour)
        val routines = ep.routines().observeRoutinesOnce()
        val logs = ep.routines().getLogs(ScheduleResolver.weekStartOf(today, st.weekStart), today.plusDays(1))
        val planned = plan(routines, logs, today, now, st.dayStartHour, st.weekStart).take(60)

        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        planned.forEach { p ->
            val pi = PendingIntent.getBroadcast(
                context, p.requestCode, ReminderReceiver.intent(context, p),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val at = p.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
        prefs.edit { putStringSet(KEY_CODES, planned.map { it.requestCode.toString() }.toSet()) }
    }
}

private suspend fun app.myroutine.domain.repository.RoutineRepository.observeRoutinesOnce() =
    observeRoutines().first()
