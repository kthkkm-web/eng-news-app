package app.myroutine.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.myroutine.R
import app.myroutine.widget.DeepLink

object Notifications {
    const val CHANNEL_REMINDER = "routine_reminders"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_REMINDER) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_REMINDER, "루틴 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "루틴 지정 시각과 미리 알림"
                },
            )
        }
    }

    fun notificationId(routineId: String) = routineId.hashCode()

    fun show(context: Context, routineId: String, title: String, text: String, date: String, canComplete: Boolean, color: Int) {
        if (!canPost(context)) return
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, notificationId(routineId), DeepLink.intent(context, "home"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(color)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (canComplete) {
            val done = PendingIntent.getBroadcast(
                context, notificationId(routineId), ReminderActionReceiver.intent(context, routineId, date),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            b.addAction(0, "완료 +1", done)
        }
        try { NotificationManagerCompat.from(context).notify(notificationId(routineId), b.build()) } catch (_: SecurityException) {}
    }

    fun cancel(context: Context, routineId: String) = NotificationManagerCompat.from(context).cancel(notificationId(routineId))

    // ---------- 권한 상태 ----------

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    fun ignoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    fun appNotificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** 배터리 최적화 예외 요청 화면(삼성 One UI 에서 알람·위젯 누락 방지). */
    @android.annotation.SuppressLint("BatteryLife")
    fun batteryOptimizationRequest(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
}
