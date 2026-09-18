package com.notifai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class Schedule(
    val id: Long,
    var title: String,
    var message: String,
    var hour: Int,
    var minute: Int,
    var days: Set<Int>,
    var enabled: Boolean = true
) {
    fun daySummary(): String = if (days.size == 7) "Every day"
    else days.sorted().joinToString(", ") { DAY_NAMES[it] }

    companion object {
        val DAY_NAMES = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }
}

class ScheduleStore(context: Context) {
    private val preferences = context.getSharedPreferences("schedules", Context.MODE_PRIVATE)

    fun all(): List<Schedule> {
        val raw = preferences.getString("items", "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val days = item.getJSONArray("days").let { values ->
                (0 until values.length()).map { values.getInt(it) }.toSet()
            }
            Schedule(
                item.getLong("id"), item.getString("title"), item.getString("message"),
                item.getInt("hour"), item.getInt("minute"), days, item.optBoolean("enabled", true)
            )
        }
    }

    fun save(items: List<Schedule>) {
        val array = JSONArray()
        items.forEach { schedule ->
            array.put(JSONObject().apply {
                put("id", schedule.id)
                put("title", schedule.title)
                put("message", schedule.message)
                put("hour", schedule.hour)
                put("minute", schedule.minute)
                put("enabled", schedule.enabled)
                put("days", JSONArray(schedule.days.sorted()))
            })
        }
        preferences.edit().putString("items", array.toString()).apply()
    }

    fun nextId(): Long = (all().maxOfOrNull { it.id } ?: 0L) + 1
}

fun Schedule.nextTriggerMillis(now: Calendar = Calendar.getInstance()): Long? {
    for (offset in 0..7) {
        val candidate = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, offset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sundayBasedDay = candidate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        if (sundayBasedDay in days && candidate.timeInMillis > now.timeInMillis) {
            return candidate.timeInMillis
        }
    }
    return null
}
