package com.example.eventkampus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventkampus.R
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.ActivityManageParticipantsBinding
import com.example.eventkampus.util.CertificateLinkStore

class ManageParticipantsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageParticipantsBinding
    private lateinit var repository: EventRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var adapter: ParticipantAdapter

    private var eventId: String = ""
    private var eventTitle: String = ""
    private var allTickets: List<Ticket> = emptyList()
    private var currentFilter: String = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityManageParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: run {
            Toast.makeText(this, "Data kegiatan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Kelola Peserta"

        repository     = EventRepository.getInstance(this)
        authRepository = AuthRepository.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        loadParticipants()
    }

    override fun onResume() {
        super.onResume()
        loadParticipants()
    }

    private fun setupToolbar() {
        binding.toolbarManage.title    = "Kelola Peserta"
        binding.toolbarManage.subtitle = eventTitle
        binding.toolbarManage.setNavigationOnClickListener { finish() }
        binding.toolbarManage.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_export_contacts -> { showExportOptions(); true }
                R.id.action_input_cert_link -> { showInputCertLinkDialog(); true }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ParticipantAdapter(
            onApprove = { ticket -> showApproveDialog(ticket) },
            onReject  = { ticket -> showRejectDialog(ticket) }
        )
        binding.rvParticipants.layoutManager = LinearLayoutManager(this)
        binding.rvParticipants.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                binding.chipPending.id  -> "Terdaftar"
                binding.chipApproved.id -> "Disetujui"
                binding.chipRejected.id -> "Ditolak"
                else                   -> "Semua"
            }
            applyFilter()
        }
    }

    private fun loadParticipants() {
        allTickets = repository.getTicketsByEvent(eventId)
        updateSummary()
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "Semua") {
            allTickets
        } else {
            allTickets.filter { it.status == currentFilter }
        }

        adapter.submitList(filtered)

        val isEmpty = filtered.isEmpty()
        binding.rvParticipants.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility    = if (isEmpty) View.VISIBLE else View.GONE

        if (isEmpty) {
            binding.tvEmptyMessage.text = when (currentFilter) {
                "Semua"     -> "Belum ada peserta yang mendaftar"
                "Terdaftar" -> "Tidak ada peserta yang menunggu review"
                "Disetujui" -> "Belum ada peserta yang disetujui"
                "Ditolak"   -> "Belum ada peserta yang ditolak"
                else        -> "Tidak ada data"
            }
        }
    }

    private fun updateSummary() {
        binding.tvSummaryTotal.text    = allTickets.size.toString()
        binding.tvSummaryApproved.text = allTickets.count { it.status == "Disetujui" }.toString()
        binding.tvSummaryPending.text  = allTickets.count { it.status == "Terdaftar" }.toString()
        binding.tvSummaryRejected.text = allTickets.count { it.status == "Ditolak" }.toString()
    }

    // ─────────────────────────────────────────
    // EKSPOR NOMOR WA
    // ─────────────────────────────────────────

    /**
     * Tampilkan pilihan: ekspor semua peserta atau hanya yang disetujui.
     */
    private fun showExportOptions() {
        if (allTickets.isEmpty()) {
            Toast.makeText(this, "Belum ada peserta pada kegiatan ini.", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf(
            "Semua Peserta (${allTickets.size})",
            "Disetujui saja (${allTickets.count { it.status == "Disetujui" }})",
            "Terdaftar / Menunggu (${allTickets.count { it.status == "Terdaftar" }})"
        )

        AlertDialog.Builder(this)
            .setTitle("Unduh Nomor WhatsApp Peserta")
            .setItems(options) { _, which ->
                val filtered = when (which) {
                    0 -> allTickets
                    1 -> allTickets.filter { it.status == "Disetujui" }
                    2 -> allTickets.filter { it.status == "Terdaftar" }
                    else -> allTickets
                }
                exportContacts(filtered)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportContacts(tickets: List<Ticket>) {
        if (tickets.isEmpty()) {
            Toast.makeText(this, "Tidak ada peserta pada filter ini.", Toast.LENGTH_SHORT).show()
            return
        }

        // Gabungkan data tiket dengan nomor WA dari tabel user (lookup by NIM)
        val rows = StringBuilder()
        rows.appendLine("=== DAFTAR NOMOR WA PESERTA ===")
        rows.appendLine("Kegiatan : $eventTitle")
        rows.appendLine("Total    : ${tickets.size} peserta")
        rows.appendLine("=" .repeat(34))
        rows.appendLine()

        var found = 0
        tickets.forEachIndexed { index, ticket ->
            val phone = authRepository.getPhoneByNim(ticket.participantNim)
            val phoneDisplay = formatPhone(phone)

            rows.appendLine("${index + 1}. ${ticket.participantName}")
            rows.appendLine("   NIM   : ${ticket.participantNim}")
            rows.appendLine("   Prodi : ${ticket.participantProdi}")
            rows.appendLine("   WA    : $phoneDisplay")
            rows.appendLine("   Status: ${ticket.status}")
            rows.appendLine()

            if (phone != null) found++
        }

        rows.appendLine("=" .repeat(34))
        rows.appendLine("Nomor WA ditemukan: $found dari ${tickets.size} peserta")
        if (found < tickets.size) {
            rows.appendLine("*Peserta tanpa nomor WA mungkin mendaftar")
            rows.appendLine(" sebelum fitur nomor WA tersedia.")
        }

        shareContactList(rows.toString(), found)
    }

    private fun shareContactList(content: String, found: Int) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Nomor WA Peserta – $eventTitle")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, "Ekspor Nomor WA via…"))

        Toast.makeText(
            this,
            "$found nomor WhatsApp diekspor.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun formatPhone(raw: String?): String {
        if (raw.isNullOrBlank()) return "Tidak tersedia"
        // Normalisasi ke format internasional +62
        val digits = raw.replace(Regex("[^0-9]"), "")
        return when {
            digits.startsWith("62") -> "+$digits"
            digits.startsWith("0")  -> "+62${digits.substring(1)}"
            else                     -> raw
        }
    }

    // ─────────────────────────────────────────
    // APPROVE / REJECT
    // ─────────────────────────────────────────

    private fun showApproveDialog(ticket: Ticket) {
        AlertDialog.Builder(this)
            .setTitle("Setujui Pendaftaran?")
            .setMessage(
                "Anda akan menyetujui pendaftaran:\n\n" +
                "Nama  : ${ticket.participantName}\n" +
                "NIM   : ${ticket.participantNim}\n" +
                "Prodi : ${ticket.participantProdi}"
            )
            .setPositiveButton("Setujui") { _, _ ->
                val success = repository.updateTicketStatus(ticket.ticketId, "Disetujui")
                if (success) {
                    Toast.makeText(this, "Pendaftaran ${ticket.participantName} telah disetujui.", Toast.LENGTH_SHORT).show()
                    loadParticipants()
                } else {
                    Toast.makeText(this, "Gagal memperbarui status.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showRejectDialog(ticket: Ticket) {
        AlertDialog.Builder(this)
            .setTitle("Tolak Pendaftaran?")
            .setMessage(
                "Anda akan menolak pendaftaran:\n\n" +
                "Nama  : ${ticket.participantName}\n" +
                "NIM   : ${ticket.participantNim}\n" +
                "Prodi : ${ticket.participantProdi}\n\n" +
                "Peserta akan mendapat status 'Ditolak' pada tiketnya."
            )
            .setPositiveButton("Tolak") { _, _ ->
                val success = repository.updateTicketStatus(ticket.ticketId, "Ditolak")
                if (success) {
                    Toast.makeText(this, "Pendaftaran ${ticket.participantName} telah ditolak.", Toast.LENGTH_SHORT).show()
                    loadParticipants()
                } else {
                    Toast.makeText(this, "Gagal memperbarui status.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─────────────────────────────────────────
    // INPUT LINK SERTIFIKAT (admin per event)
    // ─────────────────────────────────────────

    private fun showInputCertLinkDialog() {
        val currentLink = CertificateLinkStore.getLink(this, eventId).orEmpty()

        val editText = EditText(this).apply {
            inputType   = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            hint        = "https://drive.google.com/file/d/..."
            setSingleLine()
            setText(currentLink)
            setSelection(text.length)
            setPadding(48, 32, 48, 16)
        }

        val titleMsg = if (currentLink.isEmpty())
            "Input link Google Drive sertifikat untuk semua peserta kegiatan ini."
        else
            "Link saat ini:\n$currentLink\n\nGanti dengan link baru:"

        AlertDialog.Builder(this)
            .setTitle("🔗 Link Sertifikat – $eventTitle")
            .setMessage(titleMsg)
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val link = editText.text.toString().trim()
                when {
                    link.isEmpty() ->
                        Toast.makeText(this, "Link tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                    !link.startsWith("http") ->
                        Toast.makeText(this, "Link harus diawali http:// atau https://", Toast.LENGTH_SHORT).show()
                    else -> {
                        CertificateLinkStore.saveLink(this, eventId, link)
                        val eligible = allTickets.count {
                            it.status == "Disetujui" || it.status == "Hadir"
                        }
                        Toast.makeText(
                            this,
                            "Link disimpan. $eligible peserta eligible kini bisa mengakses sertifikat.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    companion object {
        const val EXTRA_EVENT_ID    = "extra_event_id"
        const val EXTRA_EVENT_TITLE = "extra_event_title"
    }
}
