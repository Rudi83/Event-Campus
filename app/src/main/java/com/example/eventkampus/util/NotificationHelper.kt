package com.example.eventkampus.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.eventkampus.R
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.ui.ticket.TicketDetailActivity

object NotificationHelper {

    const val CHANNEL_ID = "channel_event_kampus"
    const val CHANNEL_NAME = "Notifikasi Kegiatan Kampus"
    const val CHANNEL_DESC = "Notifikasi konfirmasi pendaftaran tiket dan pengingat kegiatan kampus"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Memunculkan notifikasi sistem saat pendaftaran kegiatan berhasil
     */
    fun showRegistrationNotification(context: Context, ticket: Ticket) {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If not granted, return gracefully
                return
            }
        }

        val intent = Intent(context, TicketDetailActivity::class.java).apply {
            putExtra(TicketDetailActivity.EXTRA_TICKET, ticket)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ticket.ticketId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Pendaftaran Berhasil! 🎟️")
            .setContentText("E-Tiket untuk '${ticket.eventTitle}' telah terbit.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Selamat, Anda telah terdaftar pada kegiatan '${ticket.eventTitle}'. Ketuk di sini untuk melihat e-tiket dan kode QR resmi Anda.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ticket.ticketId.hashCode(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Memunculkan notifikasi pengingat jadwal kegiatan
     */
    fun showEventReminderNotification(context: Context, event: Event) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle("Pengingat Kegiatan: ${event.title} ⏰")
            .setContentText("Jadwal: ${event.dateTime}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Jangan lewatkan kegiatan '${event.title}' yang akan berlangsung pada ${event.dateTime} di ${event.location}.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(event.id.hashCode(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
