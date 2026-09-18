package com.notifai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("schedule_id", -1L)
        val item = ScheduleStore(context).all().firstOrNull { it.id == id && it.enabled } ?: return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
        )
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(id.hashCode(), android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setStyle(android.app.Notification.BigTextStyle().bigText(item.message))
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build())
        AlarmScheduler.schedule(context, item)
    }

    companion object {
        const val CHANNEL_ID = "scheduled_activities"
    }
}
