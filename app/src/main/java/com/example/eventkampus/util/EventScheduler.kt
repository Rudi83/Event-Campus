package com.example.eventkampus.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.receiver.EventAlarmReceiver

object EventScheduler {

    /**
     * Jadwalkan dua alarm untuk satu event:
     * 1. Saat event mulai  → notif admin untuk share link absensi
     * 2. 10 menit sebelum selesai → notif bahwa jendela absensi terbuka
     */
    fun schedule(context: Context, event: Event) {
        if (!event.hasSchedule) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Alarm 1: saat mulai
        val startIntent = buildIntent(context, event, EventAlarmReceiver.TYPE_START)
        val startPi = PendingIntent.getBroadcast(
            context, event.id.hashCode(), startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(am, event.startEpoch, startPi)

        // Alarm 2: 10 menit sebelum selesai
        val windowEpoch = event.endEpoch - 10 * 60 * 1000L
        if (windowEpoch > System.currentTimeMillis()) {
            val winIntent = buildIntent(context, event, EventAlarmReceiver.TYPE_ATTENDANCE_WINDOW)
            val winPi = PendingIntent.getBroadcast(
                context, event.id.hashCode() + 1, winIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExact(am, windowEpoch, winPi)
        }
    }

    /** Batalkan semua alarm untuk event tertentu */
    fun cancel(context: Context, event: Event) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(0, 1).forEach { offset ->
            val pi = PendingIntent.getBroadcast(
                context, event.id.hashCode() + offset,
                Intent(context, EventAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { am.cancel(it) }
        }
    }

    private fun buildIntent(context: Context, event: Event, type: String) =
        Intent(context, EventAlarmReceiver::class.java).apply {
            putExtra(EventAlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(EventAlarmReceiver.EXTRA_EVENT_TITLE, event.title)
            putExtra(EventAlarmReceiver.EXTRA_ALARM_TYPE, type)
        }

    private fun setExact(am: AlarmManager, triggerMs: Long, pi: PendingIntent) {
        if (triggerMs <= System.currentTimeMillis()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Fallback ke inexact jika izin SCHEDULE_EXACT_ALARM belum diberikan
            am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }
}
