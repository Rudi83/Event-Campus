package com.example.eventkampus.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.eventkampus.R
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.ui.admin.EventSessionActivity

/**
 * BroadcastReceiver yang dipicu AlarmManager tepat saat event dimulai.
 * Menampilkan notifikasi ke admin untuk membagikan link absensi ke peserta.
 */
class EventAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId    = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Event"
        val alarmType  = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: TYPE_START

        when (alarmType) {
            TYPE_START -> handleEventStart(context, eventId, eventTitle)
            TYPE_ATTENDANCE_WINDOW -> handleAttendanceWindow(context, eventId, eventTitle)
        }
    }

    private fun handleEventStart(context: Context, eventId: String, eventTitle: String) {
        // Buka Activity sesi event untuk admin
        val sessionIntent = Intent(context, EventSessionActivity::class.java).apply {
            putExtra(EventSessionActivity.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, eventId.hashCode(), sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            context,
            id = eventId.hashCode(),
            title = "⏰ Event Dimulai Sekarang!",
            body = "\"$eventTitle\" telah dimulai. Ketuk untuk membagikan link absensi ke peserta.",
            pendingIntent = pendingIntent,
            channelId = CHANNEL_ADMIN
        )
    }

    private fun handleAttendanceWindow(context: Context, eventId: String, eventTitle: String) {
        val sessionIntent = Intent(context, EventSessionActivity::class.java).apply {
            putExtra(EventSessionActivity.EXTRA_EVENT_ID, eventId)
            putExtra(EventSessionActivity.EXTRA_OPEN_ATTENDANCE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, eventId.hashCode() + 1, sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            context,
            id = eventId.hashCode() + 1,
            title = "🕐 Jendela Absensi Terbuka!",
            body = "10 menit terakhir \"$eventTitle\". Peserta dapat melakukan absensi sekarang.",
            pendingIntent = pendingIntent,
            channelId = CHANNEL_ADMIN
        )
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        body: String,
        pendingIntent: PendingIntent,
        channelId: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "EventKampus Admin", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(id, notification)
    }

    companion object {
        const val EXTRA_EVENT_ID    = "extra_event_id"
        const val EXTRA_EVENT_TITLE = "extra_event_title"
        const val EXTRA_ALARM_TYPE  = "extra_alarm_type"
        const val TYPE_START             = "start"
        const val TYPE_ATTENDANCE_WINDOW = "attendance_window"
        const val CHANNEL_ADMIN = "eventkampus_admin"
    }
}
