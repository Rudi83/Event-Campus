package com.example.eventkampus.ui.detail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.R
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.ActivityDetailEventBinding
import com.example.eventkampus.ui.admin.ManageParticipantsActivity
import com.example.eventkampus.ui.manage.AddEditEventActivity
import com.example.eventkampus.ui.register.RegisterEventActivity
import com.example.eventkampus.ui.ticket.TicketDetailActivity
import com.example.eventkampus.util.NotificationHelper

class DetailEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailEventBinding
    private lateinit var repository: EventRepository
    private lateinit var authRepository: AuthRepository
    private var event: Event? = null
    private var eventId: String? = null

    // Runtime permission launcher untuk POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Izin diberikan — kirim notifikasi reminder sekarang
            event?.let { NotificationHelper.showEventReminderNotification(this, it) }
            Toast.makeText(this, "Pengingat berhasil diaktifkan! ⏰", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin notifikasi diperlukan untuk fitur pengingat.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailEventBinding.inflate(layoutInflater)
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
        eventId = event?.id ?: intent.getStringExtra(EXTRA_EVENT_ID)

        if (event == null && eventId != null) {
            event = repository.getEventById(eventId!!)
        }

        if (event == null) {
            Toast.makeText(this, "Data kegiatan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        populateEventDetails()
        setupCrudActions()
    }

    override fun onResume() {
        super.onResume()
        // Reload fresh data from database SQLite in case it was updated
        eventId?.let { id ->
            val refreshed = repository.getEventById(id)
            if (refreshed != null) {
                event = refreshed
                populateEventDetails()
            } else {
                // Was deleted or no longer exists
                finish()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun populateEventDetails() {
        val currentEvent = event ?: return

        // Sembunyikan menu kelola (edit/delete) untuk user biasa — admin only
        val currentUser = authRepository.getCurrentUser()
        binding.cardManageEvent.visibility = if (currentUser?.isAdmin == true) View.VISIBLE else View.GONE

        binding.tvDetailCategory.text = currentEvent.category
        binding.tvDetailHeaderSpeaker.text = "Pemateri: ${currentEvent.speaker}"
        binding.tvDetailTitle.text = currentEvent.title
        binding.tvDetailOrganizer.text = "Diselenggarakan oleh:\n${currentEvent.organizer}"
        binding.tvDetailDateTime.text = currentEvent.dateTime
        binding.tvDetailLocation.text = if (currentEvent.isOnline) "Online (${currentEvent.location})" else currentEvent.location
        binding.tvDetailSpeaker.text = currentEvent.speaker
        binding.tvDetailQuota.text = "Sisa ${currentEvent.remainingQuota} dari ${currentEvent.quota} Kuota"

        // Price badge & bottom price
        if (currentEvent.isFree) {
            binding.tvDetailPriceBadge.text = "Gratis"
            binding.tvDetailPriceBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.badge_free_container)
            )
            binding.tvDetailPriceBadge.setTextColor(
                ContextCompat.getColor(this, R.color.badge_free)
            )
            binding.tvBottomPrice.text = "Gratis"
        } else {
            binding.tvDetailPriceBadge.text = currentEvent.formattedPrice
            binding.tvDetailPriceBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.secondary_container)
            )
            binding.tvDetailPriceBadge.setTextColor(
                ContextCompat.getColor(this, R.color.secondary)
            )
            binding.tvBottomPrice.text = currentEvent.formattedPrice
        }

        binding.tvDetailDescription.text = currentEvent.description

        // Benefits
        val benefitsText = currentEvent.benefits.joinToString(separator = "\n") { "• $it" }
        binding.tvDetailBenefits.text = benefitsText

        // Terms
        val termsText = currentEvent.terms.joinToString(separator = "\n") { "• $it" }
        binding.tvDetailTerms.text = termsText

        updateRegistrationButtonState()
    }

    private fun setupCrudActions() {
        // Google Maps Intent / Lokasi Kampus
        binding.btnOpenMaps.setOnClickListener {
            val currentEvent = event ?: return@setOnClickListener
            if (currentEvent.isOnline) {
                Toast.makeText(
                    this,
                    "Kegiatan ini diselenggarakan secara daring (Online via Zoom). Tautan akses tersedia di E-Tiket Anda.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val locationQuery = "${currentEvent.location}, Universitas Malikussaleh, Lhokseumawe"
                try {
                    val gmmIntentUri = android.net.Uri.parse(
                        "geo:5.2285,97.0658?q=" + android.net.Uri.encode(locationQuery)
                    )
                    startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                } catch (e: Exception) {
                    // Fallback ke browser jika tidak ada aplikasi Maps
                    try {
                        val webUri = android.net.Uri.parse(
                            "https://www.google.com/maps/search/?api=1&query=" +
                                android.net.Uri.encode(locationQuery)
                        )
                        startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    } catch (ex: Exception) {
                        Toast.makeText(
                            this,
                            "Tidak dapat membuka peta. Pastikan ada browser atau Maps terpasang.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // Pengingat Jadwal (Notification)
        binding.btnSetReminder.setOnClickListener {
            val currentEvent = event ?: return@setOnClickListener
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    NotificationHelper.showEventReminderNotification(this, currentEvent)
                    Toast.makeText(this, "Pengingat berhasil diaktifkan! Notifikasi jadwal telah dikirim ke status bar. ⏰", Toast.LENGTH_SHORT).show()
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                NotificationHelper.showEventReminderNotification(this, currentEvent)
                Toast.makeText(this, "Pengingat berhasil diaktifkan! Notifikasi jadwal telah dikirim ke status bar. ⏰", Toast.LENGTH_SHORT).show()
            }
        }

        // ADMIN: Kelola sesi event (timer & absensi)
        binding.btnManageParticipants.setOnClickListener {
            val currentEvent = event ?: return@setOnClickListener
            val intent = Intent(this, ManageParticipantsActivity::class.java).apply {
                putExtra(ManageParticipantsActivity.EXTRA_EVENT_ID, currentEvent.id)
                putExtra(ManageParticipantsActivity.EXTRA_EVENT_TITLE, currentEvent.title)
            }
            startActivity(intent)
        }

        // ADMIN: Buka halaman sesi event (countdown & link absensi)
        binding.btnEventSession.setOnClickListener {
            val currentEvent = event ?: return@setOnClickListener
            val intent = Intent(this, com.example.eventkampus.ui.admin.EventSessionActivity::class.java).apply {
                putExtra(com.example.eventkampus.ui.admin.EventSessionActivity.EXTRA_EVENT_ID, currentEvent.id)
            }
            startActivity(intent)
        }

        // UPDATE: Buka form edit
        binding.btnEditEvent.setOnClickListener {
            val currentEvent = event ?: return@setOnClickListener
            val intent = Intent(this, AddEditEventActivity::class.java).apply {
                putExtra(AddEditEventActivity.EXTRA_EVENT, currentEvent)
                putExtra(AddEditEventActivity.EXTRA_EVENT_ID, currentEvent.id)
            }
            startActivity(intent)
        }

        // DELETE: Hapus data dengan konfirmasi dialog
        binding.btnDeleteEvent.setOnClickListener {
            val currentEvent = event ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Hapus Kegiatan?")
                .setMessage("Apakah Anda yakin ingin menghapus kegiatan '${currentEvent.title}'?\n\nData akan dihapus permanen dari penyimpanan SQLite lokal.")
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton("Ya, Hapus") { _, _ ->
                    val success = repository.deleteEvent(currentEvent.id)
                    if (success) {
                        Toast.makeText(this, "Kegiatan berhasil dihapus dari database.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal menghapus kegiatan.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun updateRegistrationButtonState() {
        val currentEvent = event ?: return
        val currentNim   = authRepository.getCurrentUser()?.nim.orEmpty()
        val isRegistered = repository.isUserRegistered(currentEvent.id, currentNim)
        val now          = System.currentTimeMillis()

        // Prioritas tertinggi: event sudah selesai (endEpoch terlewati)
        val isEnded = currentEvent.hasSchedule && now > currentEvent.endEpoch

        if (isRegistered) {
            // Sudah daftar — tampilkan tiket terlepas event sudah selesai atau belum
            binding.btnRegister.text = "Lihat Tiket Saya"
            binding.btnRegister.isEnabled = true
            binding.btnRegister.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.badge_free)
            )
            binding.btnRegister.setOnClickListener {
                val myTicket = repository.getMyTickets(currentNim).find { it.eventId == currentEvent.id }
                if (myTicket != null) {
                    startActivity(
                        Intent(this, TicketDetailActivity::class.java).apply {
                            putExtra(TicketDetailActivity.EXTRA_TICKET, myTicket)
                        }
                    )
                }
            }
        } else if (isEnded) {
            // Event sudah selesai — tutup pendaftaran
            binding.btnRegister.text = "Pendaftaran Ditutup"
            binding.btnRegister.isEnabled = false
            binding.btnRegister.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.text_tertiary)
            )
            binding.btnRegister.setOnClickListener(null)
        } else if (currentEvent.remainingQuota <= 0) {
            binding.btnRegister.text = "Kuota Penuh"
            binding.btnRegister.isEnabled = false
            binding.btnRegister.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.text_tertiary)
            )
        } else {
            binding.btnRegister.text = "Daftar Sekarang"
            binding.btnRegister.isEnabled = true
            binding.btnRegister.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.secondary)
            )
            binding.btnRegister.setOnClickListener {
                startActivity(
                    Intent(this, RegisterEventActivity::class.java).apply {
                        putExtra(RegisterEventActivity.EXTRA_EVENT, currentEvent)
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_EVENT = "extra_event"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }
}
