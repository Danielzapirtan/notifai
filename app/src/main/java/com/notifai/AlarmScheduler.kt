package com.notifai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    private const val EXTRA_ID = "schedule_id"

    fun schedule(context: Context, item: Schedule) {
        cancel(context, item.id)
        val trigger = item.nextTriggerMillis() ?: return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context, item.id)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation)
        }
    }

    fun cancel(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, id))
    }

    private fun pendingIntent(context: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context, id.hashCode(), Intent(context, AlarmReceiver::class.java)
                .putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
