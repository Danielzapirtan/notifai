package com.notifai

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var store: ScheduleStore
    private lateinit var list: LinearLayout
    private var selectedTime = Pair(9, 0)
    private val selectedDays = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ScheduleStore(this)
        requestNotificationPermission()
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 16)
        }
        root.addView(TextView(this).apply {
            text = "Notifai"
            textSize = 28f
            setTextColor(Color.BLACK)
        })
        root.addView(TextView(this).apply {
            text = "Scheduled activities"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        })
        root.addView(Button(this).apply {
            text = "Add activity"
            setOnClickListener { showEditor(null) }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            addView(list)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        })
        setContentView(root)
        renderList()
    }

    private fun renderList() {
        list.removeAllViews()
        val items = store.all().sortedWith(compareBy<Schedule> { it.hour }.thenBy { it.minute })
        if (items.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No activities yet. Add one to start receiving notifications."
                setPadding(0, 32, 0, 0)
            })
            return
        }
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 20, 0, 20)
            }
            val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            header.addView(TextView(this).apply {
                text = String.format(Locale.getDefault(), "%02d:%02d  %s", item.hour, item.minute, item.title)
                textSize = 18f
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            header.addView(Switch(this).apply {
                isChecked = item.enabled
                setOnCheckedChangeListener { _, checked ->
                    item.enabled = checked
                    store.save(store.all())
                    if (checked) AlarmScheduler.schedule(this@MainActivity, item)
                    else AlarmScheduler.cancel(this@MainActivity, item.id)
                }
            })
            row.addView(header)
            row.addView(TextView(this).apply {
                text = "${item.daySummary()}  •  ${item.message}"
                setPadding(0, 6, 0, 0)
            })
            row.setOnClickListener { showEditor(item) }
            list.addView(row)
        }
    }

    private fun showEditor(existing: Schedule?) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 0, 48, 0)
        }
        val title = EditText(this).apply {
            hint = "Activity name"
            setText(existing?.title ?: "")
        }
        val message = EditText(this).apply {
            hint = "Notification message"
            setText(existing?.message ?: "")
        }
        val time = Button(this).apply { text = formatTime(existing?.hour ?: 9, existing?.minute ?: 0) }
        selectedTime = Pair(existing?.hour ?: 9, existing?.minute ?: 0)
        selectedDays.clear()
        selectedDays.addAll(existing?.days ?: setOf(1, 2, 3, 4, 5))
        time.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime = Pair(hour, minute)
                time.text = formatTime(hour, minute)
            }, selectedTime.first, selectedTime.second, true).show()
        }
        container.addView(title)
        container.addView(message)
        container.addView(time)
        container.addView(TextView(this).apply { text = "Repeat on:"; setPadding(0, 12, 0, 4) })
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        days.forEachIndexed { index, name ->
            container.addView(CheckBox(this).apply {
                text = name
                isChecked = index in selectedDays
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedDays.add(index) else selectedDays.remove(index)
                }
            })
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(if (existing == null) "New activity" else "Edit activity")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
        if (existing != null) dialog.setNeutralButton("Delete", null)
        val shown = dialog.create()
        shown.setOnShowListener {
            shown.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (title.text.isNullOrBlank() || message.text.isNullOrBlank() || selectedDays.isEmpty()) {
                    Toast.makeText(this, "Enter a name, message, and at least one day.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val item = existing ?: Schedule(store.nextId(), "", "", 0, 0, emptySet())
                item.title = title.text.toString().trim()
                item.message = message.text.toString().trim()
                item.hour = selectedTime.first
                item.minute = selectedTime.second
                item.days = selectedDays.toSet()
                item.enabled = true
                val all = store.all().filterNot { it.id == item.id } + item
                store.save(all)
                AlarmScheduler.schedule(this, item)
                shown.dismiss()
                renderList()
            }
            shown.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                existing?.let { AlarmScheduler.cancel(this, it.id); store.save(store.all().filterNot { item -> item.id == it.id }) }
                shown.dismiss()
                renderList()
            }
        }
        shown.show()
    }

    private fun formatTime(hour: Int, minute: Int) = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
        val alarm = getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }
    }
}
