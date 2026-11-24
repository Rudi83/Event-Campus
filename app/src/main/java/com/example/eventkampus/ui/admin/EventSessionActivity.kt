package com.example.eventkampus.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.ActivityEventSessionBinding

class EventSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventSessionBinding
    private lateinit var repository: EventRepository
    private var event: Event? = null

    // Countdown ticker setiap detik
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateCountdown()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        repository = EventRepository.getInstance(this)

        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: run {
            Toast.makeText(this, "Event tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        event = repository.getEventById(eventId) ?: run {
            Toast.makeText(this, "Event tidak ditemukan di database", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        binding.toolbarSession.setNavigationOnClickListener { finish() }
        setupUI()

        // Jika dibuka dari notifikasi window absensi, langsung tampilkan dialog share
        if (intent.getBooleanExtra(EXTRA_OPEN_ATTENDANCE, false)) {
            showShareConfirmDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
        refreshRecap()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun setupUI() {
        val e = event ?: return
        binding.tvSessionEventTitle.text = e.title

        // Buat link absensi deep-link
        val link = buildAttendanceLink(e.id)
        binding.tvAttendanceLink.text = link

        // Info window absensi
        binding.tvAttendanceWindowInfo.text =
            "Link aktif 10 menit sebelum event selesai hingga jam berakhir."

        binding.btnShareWhatsApp.setOnClickListener {
            showShareConfirmDialog()
        }

        refreshRecap()
    }

    private fun showShareConfirmDialog() {
        val e = event ?: return
        val tickets = repository.getTicketsByEvent(e.id)
            .filter { it.status == "Disetujui" || it.status == "Terdaftar" }

        if (tickets.isEmpty()) {
            Toast.makeText(this,
                "Tidak ada peserta disetujui untuk diberi link absensi.",
                Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Bagikan Link Absensi?")
            .setMessage(
                "Anda akan membagikan link absensi ke ${tickets.size} peserta via WhatsApp.\n\n" +
                "Link hanya aktif dalam 10 menit terakhir sebelum event selesai.\n\n" +
                "Lanjutkan?"
            )
            .setPositiveButton("Ya, Bagikan") { _, _ ->
                shareToAllParticipants(e, tickets)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun shareToAllParticipants(event: Event, tickets: List<Ticket>) {
        val link = buildAttendanceLink(event.id)
        val pesan = buildString {
            appendLine("📋 *Absensi Kegiatan EventKampus*")
            appendLine()
            appendLine("Halo! Anda terdaftar sebagai peserta:")
            appendLine("*${event.title}*")
            appendLine()
            appendLine("📍 ${event.location}")
            appendLine("🕐 ${event.dateTime}")
            appendLine()
            appendLine("Silakan klik link berikut untuk mengisi absensi kehadiran.")
            appendLine("⚠️ Link hanya aktif 10 menit sebelum event selesai.")
            appendLine()
            appendLine("🔗 $link")
            appendLine()
            appendLine("_EventKampus – UNIMAL_")
        }

        // Kirim satu per satu via WhatsApp ke nomor peserta
        var sentCount = 0
        for (ticket in tickets) {
            val phone = normalizePhone(ticket.qrToken) // qrToken menyimpan info, cukup buka WhatsApp umum
            val waIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/?text=${Uri.encode(pesan)}")
            )
            try {
                startActivity(waIntent)
                sentCount++
                break // Buka sekali saja — user akan lanjut manual per peserta
            } catch (e: Exception) {
                // WhatsApp tidak terpasang
            }
        }

        // Fallback: share umum jika WhatsApp tidak ada
        if (sentCount == 0) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, pesan)
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Link Absensi via…"))
        }

        Toast.makeText(this,
            "Link absensi disiapkan. Bagikan ke masing-masing peserta.",
            Toast.LENGTH_LONG).show()
    }

    private fun updateCountdown() {
        val e = event ?: return
        val now = System.currentTimeMillis()

        when {
            !e.hasSchedule -> {
                binding.tvSessionStatus.text   = "Jadwal belum diset"
                binding.tvSessionCountdown.text = "Admin belum mengatur waktu mulai & selesai."
            }
            now < e.startEpoch -> {
                val diff = e.startEpoch - now
                binding.tvSessionStatus.text    = "Belum Dimulai"
                binding.tvSessionCountdown.text = "Mulai dalam: ${formatDuration(diff)}"
            }
            now in e.startEpoch..e.endEpoch -> {
                val remaining = e.endEpoch - now
                val inWindow  = e.isAttendanceWindowOpen(now)
                binding.tvSessionStatus.text    = if (inWindow) "🟢 Jendela Absensi Terbuka" else "🔵 Sedang Berlangsung"
                binding.tvSessionCountdown.text = "Berakhir dalam: ${formatDuration(remaining)}"
            }
            else -> {
                binding.tvSessionStatus.text    = "✅ Telah Selesai"
                binding.tvSessionCountdown.text = "Event telah berakhir."
            }
        }
    }

    private fun refreshRecap() {
        val e = event ?: return
        val tickets = repository.getTicketsByEvent(e.id)
        val disetujui = tickets.count { it.status == "Disetujui" || it.status == "Terdaftar" }
        val hadir     = tickets.count { it.status == "Hadir" }
        val belum     = disetujui - hadir

        binding.tvRecapTotal.text   = disetujui.toString()
        binding.tvRecapPresent.text = hadir.toString()
        binding.tvRecapAbsent.text  = belum.coerceAtLeast(0).toString()
    }

    private fun buildAttendanceLink(eventId: String): String {
        // Deep link format: eventkampus://attendance?event=EVT-001
        return "eventkampus://attendance?event=$eventId"
    }

    private fun normalizePhone(raw: String) = raw.replace(Regex("[^0-9+]"), "")

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d jam %02d menit" .format(h, m)
        else             "%02d:%02d" .format(m, s)
    }

    companion object {
        const val EXTRA_EVENT_ID         = "extra_event_id"
        const val EXTRA_OPEN_ATTENDANCE  = "extra_open_attendance"
    }
}
