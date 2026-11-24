package com.example.eventkampus.ui.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.MainActivity
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.ActivityAttendanceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menangani deep link: eventkampus://attendance?event=EVT-001
 * User mengklik link dari WhatsApp → app terbuka → absensi diproses otomatis.
 */
class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        showLoading()
        processAttendance()
    }

    private fun processAttendance() {
        // Ambil eventId dari deep link atau Intent
        val data     = intent?.data
        val eventId  = data?.getQueryParameter("event")
            ?: intent?.getStringExtra("event_id")

        if (eventId.isNullOrBlank()) {
            showError("Link tidak valid", "Link absensi yang Anda gunakan tidak dikenali.")
            return
        }

        val authRepo  = AuthRepository.getInstance(this)
        val eventRepo = EventRepository.getInstance(this)

        // Harus sudah login
        val currentUser = authRepo.getCurrentUser()
        if (currentUser == null) {
            showError("Belum Login", "Silakan login ke EventKampus terlebih dahulu.")
            return
        }

        // Ambil event
        val event = eventRepo.getEventById(eventId)
        if (event == null) {
            showError("Event Tidak Ditemukan", "Data kegiatan tidak ditemukan di sistem.")
            return
        }

        // Cek window absensi (10 menit terakhir)
        if (!event.isAttendanceWindowOpen()) {
            val now = System.currentTimeMillis()
            val msg = when {
                !event.hasSchedule ->
                    "Jadwal kegiatan belum diset oleh admin."
                now < event.startEpoch ->
                    "Kegiatan belum dimulai. Absensi dibuka 10 menit sebelum event selesai."
                now > event.endEpoch ->
                    "Waktu absensi telah berakhir. Event sudah selesai."
                else ->
                    "Jendela absensi belum terbuka. Absensi dibuka 10 menit sebelum event berakhir."
            }
            showError("Absensi Belum Dibuka", msg)
            return
        }

        // Cari tiket milik user ini
        val myTickets = eventRepo.getMyTickets(currentUser.nim)
        val ticket = myTickets.find { it.eventId == eventId }

        if (ticket == null) {
            showError(
                "Tidak Terdaftar",
                "Anda tidak terdaftar sebagai peserta kegiatan ini."
            )
            return
        }

        // Sudah hadir sebelumnya?
        if (ticket.status == "Hadir") {
            showError(
                "Sudah Absen",
                "Anda sudah tercatat hadir pada kegiatan \"${event.title}\"."
            )
            return
        }

        // Tiket harus disetujui dulu
        if (ticket.status != "Disetujui" && ticket.status != "Terdaftar") {
            showError(
                "Pendaftaran Belum Disetujui",
                "Status pendaftaran Anda: ${ticket.status}. Hubungi panitia untuk informasi lebih lanjut."
            )
            return
        }

        // Tandai hadir
        val success = eventRepo.updateTicketStatus(ticket.ticketId, "Hadir")
        if (!success) {
            showError("Gagal Memproses", "Terjadi kesalahan saat menyimpan absensi. Coba lagi.")
            return
        }

        // Berhasil
        val timeNow = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        showSuccess(
            name     = ticket.participantName,
            eventTitle = event.title,
            time     = "Waktu absensi: $timeNow",
            ticketId = ticket.ticketId
        )
    }

    private fun showLoading() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutSuccess.visibility = View.GONE
        binding.layoutError.visibility   = View.GONE
    }

    private fun showSuccess(name: String, eventTitle: String, time: String, ticketId: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE
        binding.layoutError.visibility   = View.GONE

        binding.tvSuccessName.text   = name
        binding.tvSuccessEvent.text  = eventTitle
        binding.tvSuccessTime.text   = time
        binding.tvSuccessTicket.text = "Tiket: $ticketId"

        binding.btnDoneAttendance.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            finish()
        }
    }

    private fun showError(title: String, message: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutSuccess.visibility = View.GONE
        binding.layoutError.visibility   = View.VISIBLE

        binding.tvErrorTitle.text   = title
        binding.tvErrorMessage.text = message

        binding.btnCloseError.setOnClickListener { finish() }
    }
}
