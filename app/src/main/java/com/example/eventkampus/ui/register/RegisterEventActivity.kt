package com.example.eventkampus.ui.register

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.ActivityRegisterEventBinding
import com.example.eventkampus.ui.ticket.TicketDetailActivity
import com.example.eventkampus.util.NotificationHelper

class RegisterEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterEventBinding
    private lateinit var repository: EventRepository
    private lateinit var authRepository: AuthRepository
    private var event: Event? = null
    private var pendingTicket: Ticket? = null

    // Mencegah double-submit
    private var isSubmitInProgress = false

    // Permission launcher untuk notifikasi (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val ticket = pendingTicket ?: return@registerForActivityResult
        if (isGranted) {
            NotificationHelper.showRegistrationNotification(this, ticket)
        }
        showSuccessDialog(ticket)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        repository = EventRepository.getInstance(this)
        authRepository = AuthRepository.getInstance(this)

        @Suppress("DEPRECATION")
        event = intent.getSerializableExtra(EXTRA_EVENT) as? Event

        if (event == null) {
            Toast.makeText(this, "Data kegiatan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        populateEventSummary()
        populateUserData()
        setupSubmitAction()
    }

    private fun setupToolbar() {
        binding.toolbarRegister.setNavigationOnClickListener { finish() }
    }

    private fun populateEventSummary() {
        val currentEvent = event ?: return
        binding.tvRegEventTitle.text = currentEvent.title
        binding.tvRegEventPrice.text = "Biaya Pendaftaran: ${currentEvent.formattedPrice}"
    }

    private fun populateUserData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            binding.etName.setText(currentUser.namaLengkap)
            binding.etNim.setText(currentUser.nim)
            binding.etProdi.setText(currentUser.prodi)
            binding.etPhone.setText(currentUser.noWhatsapp)
        }
    }

    private fun setupSubmitAction() {
        binding.btnSubmitRegister.setOnClickListener {
            if (!isSubmitInProgress) performSubmit()
        }
    }

    private fun performSubmit() {
        // Reset semua error terlebih dahulu
        binding.tilName.error = null
        binding.tilNim.error = null
        binding.tilProdi.error = null
        binding.tilPhone.error = null

        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val nim = binding.etNim.text?.toString()?.trim().orEmpty()
        val prodi = binding.etProdi.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()

        var hasError = false

        // Validasi Nama Peserta
        if (name.isEmpty()) {
            binding.tilName.error = "Nama lengkap wajib diisi"
            hasError = true
        } else if (name.length < 3) {
            binding.tilName.error = "Nama terlalu pendek (minimal 3 karakter)"
            hasError = true
        } else if (!name.matches(Regex("^[a-zA-Z .'-]+$"))) {
            binding.tilName.error = "Nama hanya boleh berisi huruf dan spasi"
            hasError = true
        }

        // Validasi NIM
        if (nim.isEmpty()) {
            binding.tilNim.error = "NIM wajib diisi"
            hasError = true
        } else if (!nim.matches(Regex("^[0-9]+$"))) {
            binding.tilNim.error = "NIM hanya boleh berisi angka"
            hasError = true
        } else if (nim.length < 5 || nim.length > 15) {
            binding.tilNim.error = "NIM harus antara 5–15 digit angka"
            hasError = true
        }

        // Validasi Program Studi
        if (prodi.isEmpty()) {
            binding.tilProdi.error = "Program studi wajib diisi"
            hasError = true
        } else if (prodi.length < 3) {
            binding.tilProdi.error = "Program studi terlalu pendek"
            hasError = true
        }

        // Validasi Nomor WhatsApp
        val phoneDigits = phone.replace(Regex("[\\s\\-()]"), "")
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Nomor WhatsApp wajib diisi"
            hasError = true
        } else if (!phoneDigits.matches(Regex("^(\\+62|62|0)[0-9]{8,13}$"))) {
            binding.tilPhone.error = "Format nomor tidak valid (contoh: 0812-3456-7890)"
            hasError = true
        }

        // Validasi persetujuan tata tertib
        if (!binding.cbAgree.isChecked) {
            Toast.makeText(
                this,
                "Harap menyetujui pernyataan tata tertib kegiatan",
                Toast.LENGTH_SHORT
            ).show()
            hasError = true
        }

        if (hasError) return

        val currentEvent = event ?: run {
            Toast.makeText(this, "Data kegiatan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cek ketersediaan kuota sebelum mendaftar
        val freshEvent = repository.getEventById(currentEvent.id)
        if (freshEvent == null) {
            Toast.makeText(this, "Kegiatan tidak ditemukan atau sudah dihapus.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Guard: event sudah selesai — tutup pendaftaran
        if (freshEvent.hasSchedule && System.currentTimeMillis() > freshEvent.endEpoch) {
            AlertDialog.Builder(this)
                .setTitle("Pendaftaran Ditutup")
                .setMessage("Kegiatan '${freshEvent.title}' telah berakhir. Pendaftaran tidak lagi dapat dilakukan.")
                .setPositiveButton("Kembali") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        if (freshEvent.remainingQuota <= 0) {
            AlertDialog.Builder(this)
                .setTitle("Kuota Penuh")
                .setMessage("Maaf, kuota untuk kegiatan '${freshEvent.title}' sudah penuh. Silakan cari kegiatan lain.")
                .setPositiveButton("Kembali") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        // Cek apakah sudah terdaftar
        if (repository.isUserRegistered(currentEvent.id, nim)) {
            AlertDialog.Builder(this)
                .setTitle("Sudah Terdaftar")
                .setMessage("Anda sudah terdaftar pada kegiatan ini. Tiket Anda tersedia di halaman Tiket Saya.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Nonaktifkan tombol agar tidak double-submit
        setSubmitLoading(true)

        val ticket = try {
            repository.registerEvent(
                event = freshEvent,
                participantName = name,
                nim = nim,
                prodi = prodi
            )
        } catch (e: Exception) {
            setSubmitLoading(false)
            Toast.makeText(this, "Gagal mendaftar: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        setSubmitLoading(false)

        // Kirim notifikasi konfirmasi — cek permission Android 13+
        pendingTicket = ticket
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                NotificationHelper.showRegistrationNotification(this, ticket)
                showSuccessDialog(ticket)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationHelper.showRegistrationNotification(this, ticket)
            showSuccessDialog(ticket)
        }
    }

    private fun setSubmitLoading(isLoading: Boolean) {
        isSubmitInProgress = isLoading
        binding.btnSubmitRegister.isEnabled = !isLoading
        binding.btnSubmitRegister.text = if (isLoading) "Memproses…" else "Daftar & Terbitkan Tiket"
        binding.tilName.isEnabled = !isLoading
        binding.tilNim.isEnabled = !isLoading
        binding.tilProdi.isEnabled = !isLoading
        binding.tilPhone.isEnabled = !isLoading
    }

    private fun showSuccessDialog(ticket: Ticket) {
        val currentEvent = event ?: return
        AlertDialog.Builder(this)
            .setTitle("Pendaftaran Berhasil! 🎉")
            .setMessage(
                "Selamat, Anda telah terdaftar pada kegiatan '${currentEvent.title}'." +
                " Slip pendaftaran Anda telah diterbitkan."
            )
            .setCancelable(false)
            .setPositiveButton("Buka Tiket Saya") { _, _ ->
                startActivity(
                    Intent(this, TicketDetailActivity::class.java).apply {
                        putExtra(TicketDetailActivity.EXTRA_TICKET, ticket)
                    }
                )
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_EVENT = "extra_event"
    }
}
