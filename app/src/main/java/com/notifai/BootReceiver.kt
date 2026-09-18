package com.notifai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ScheduleStore(context).all().filter { it.enabled }.forEach {
            AlarmScheduler.schedule(context, it)
        }
    }
}
