package com.example.eventkampus.ui.ticket

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.databinding.ActivityTicketDetailBinding

class TicketDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTicketDetailBinding
    private var ticket: Ticket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTicketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        @Suppress("DEPRECATION")
        ticket = intent.getSerializableExtra(EXTRA_TICKET) as? Ticket

        if (ticket == null) {
            Toast.makeText(this, "Data tiket tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        populateTicketData()
    }

    private fun setupToolbar() {
        binding.toolbarTicket.setNavigationOnClickListener {
            finish()
        }
        binding.btnDone.setOnClickListener {
            finish()
        }
    }

    private fun populateTicketData() {
        val currentTicket = ticket ?: return

        binding.tvTicketDetailCode.text = "SLIP #${currentTicket.ticketId}"
        binding.tvTicketDetailTitle.text = currentTicket.eventTitle
        binding.tvTicketDetailCategory.text = currentTicket.eventCategory
        binding.tvTicketDetailName.text = currentTicket.participantName
        binding.tvTicketDetailNim.text = "${currentTicket.participantNim} (${currentTicket.participantProdi})"
        binding.tvTicketDetailTime.text = currentTicket.eventDateTime
        binding.tvTicketDetailLocation.text = currentTicket.eventLocation
        binding.tvQrToken.text = currentTicket.qrToken
        binding.tvTicketRegisteredDate.text = currentTicket.registeredDate

        binding.btnShareSlip.setOnClickListener {
            shareSlip(currentTicket)
        }
    }

    /**
     * Bagikan slip pendaftaran via teks (WhatsApp, email, dll.)
     */
    private fun shareSlip(ticket: Ticket) {
        val slipText = buildString {
            appendLine("╔══════════════════════════════╗")
            appendLine("     SLIP PENDAFTARAN PESERTA   ")
            appendLine("╚══════════════════════════════╝")
            appendLine()
            appendLine("Kegiatan  : ${ticket.eventTitle}")
            appendLine("Kategori  : ${ticket.eventCategory}")
            appendLine("Waktu     : ${ticket.eventDateTime}")
            appendLine("Lokasi    : ${ticket.eventLocation}")
            appendLine()
            appendLine("── Data Peserta ──────────────")
            appendLine("Nama      : ${ticket.participantName}")
            appendLine("NIM       : ${ticket.participantNim}")
            appendLine("Prodi     : ${ticket.participantProdi}")
            appendLine()
            appendLine("── Nomor Registrasi ──────────")
            appendLine(ticket.qrToken)
            appendLine()
            appendLine("Tgl Daftar: ${ticket.registeredDate}")
            appendLine("Status    : ${ticket.status}")
            appendLine()
            appendLine("Tunjukkan slip ini kepada panitia")
            appendLine("di meja registrasi saat tiba di lokasi.")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Slip Pendaftaran – ${ticket.eventTitle}")
            putExtra(Intent.EXTRA_TEXT, slipText)
        }
        startActivity(Intent.createChooser(shareIntent, "Bagikan Slip via…"))
    }

    companion object {
        const val EXTRA_TICKET = "extra_ticket"
    }
}
