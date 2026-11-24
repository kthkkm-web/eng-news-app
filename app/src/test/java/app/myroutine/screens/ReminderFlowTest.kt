package app.myroutine.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.myroutine.MyRoutineApp
import app.myroutine.core.newId
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.notification.ReminderActionReceiver
import app.myroutine.notification.ReminderReceiver
import app.myroutine.notification.ReminderScheduler
import app.myroutine.notification.PlannedReminder
import app.myroutine.notification.Notifications
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import java.time.LocalDate
import java.time.LocalTime

/** 예약 → 발화 → 알림 → "완료 +1" 흐름 (실제 Room/Hilt). */
@RunWith(AndroidJUnit4::class)
class ReminderFlowTest {
    private val app = ApplicationProvider.getApplicationContext<MyRoutineApp>()

    private fun waitAsync() { repeat(60) { Thread.sleep(50); ShadowLooper.idleMainLooper() } }

    @Test fun schedule_fire_and_complete() = runBlocking {
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val ep = AppEntryPoint.get(app)
        val today = ScheduleResolver.logicalDate(java.time.LocalDateTime.now(), 0)
        val r = Routine(newId(), "물 마시기", "💧", "basic:4", startDate = today.minusDays(1), rules = listOf(
            RepeatRule(newId(), Period.DAILY, timeEnabled = true, times = listOf(LocalTime.of(23, 59)), target = 2, notify = true),
        ))
        ep.routines().saveRoutine(r)

        ReminderScheduler.rescheduleAll(app)
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertTrue("알람이 예약되어야 함 (${alarms.size})", alarms.isNotEmpty())

        // 발화
        val intent = ReminderReceiver.intent(app, PlannedReminder(r.id, today, LocalTime.of(23, 59), today.atTime(23, 59), 0))
        assertTrue(ReminderReceiver.handle(app, intent))
        val nm = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        assertEquals(1, nm.allNotifications.size)
        val n = nm.allNotifications.first()
        assertEquals("💧물 마시기", n.extras.getString("android.title"))
        assertEquals("완료 +1", n.actions.first().title.toString())

        // 완료 +1
        ReminderActionReceiver.handle(app, ReminderActionReceiver.intent(app, r.id, today.toString()))
        assertEquals(1, ep.routines().getLogs(today, today).first { it.routineId == r.id }.count)
        assertEquals(0, nm.allNotifications.size)

        // 목표 달성 후에는 알림을 보내지 않음
        ep.routines().addCount(r.id, today, 1)
        assertTrue(!ReminderReceiver.handle(app, intent))
        assertEquals(0, nm.allNotifications.size)
    }
}
