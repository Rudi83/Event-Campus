package com.example.eventkampus.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.eventkampus.R
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.FragmentProfileBinding
import com.example.eventkampus.ui.admin.AdminDetailBottomSheet
import com.example.eventkampus.ui.admin.RowParcel
import com.example.eventkampus.ui.auth.LoginActivity
import com.example.eventkampus.ui.certificate.MyCertificatesActivity
import com.example.eventkampus.ui.web.CampusWebActivity
import com.example.eventkampus.util.SkpHelper
import com.example.eventkampus.util.SkpHelper.isEligibleForCertificate

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventRepository: EventRepository
    private lateinit var authRepository: AuthRepository

    // Runtime permission launcher untuk POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(
                requireContext(),
                "Izin notifikasi diberikan. Anda akan menerima pengingat kegiatan kampus.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Izin notifikasi ditolak. Aktifkan manual di Pengaturan → Aplikasi → EventKampus.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eventRepository = EventRepository.getInstance(requireContext())
        authRepository = AuthRepository.getInstance(requireContext())

        setupProfileData()
        setupListeners()

        // Auto-request notifikasi saat pertama kali buka profil (Android 13+)
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        setupProfileData()
    }

    private fun setupProfileData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            binding.tvProfileName.text = currentUser.namaLengkap
            binding.tvProfileNim.text = "NIM: ${currentUser.nim} • ${currentUser.prodi}"
            binding.tvAvatarInitial.text = currentUser.initial

            // Tampilkan badge role
            if (currentUser.isAdmin) {
                binding.tvRoleBadge.text = "Administrator"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_rounded)
                binding.tvRoleBadge.backgroundTintList = ColorStateList.valueOf(
                    Color.parseColor("#33FF8F00")
                )
                binding.tvRoleBadge.setTextColor(Color.parseColor("#FFB74D"))
            } else {
                binding.tvRoleBadge.text = "Mahasiswa"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_rounded)
                binding.tvRoleBadge.backgroundTintList = ColorStateList.valueOf(
                    Color.parseColor("#33FFFFFF")
                )
                binding.tvRoleBadge.setTextColor(Color.WHITE)
            }
        }

        // Stats card — konten berbeda tergantung role
        if (currentUser?.isAdmin == true) {
            // Tampilkan card dengan statistik admin
            binding.cardStats.visibility = View.VISIBLE

            val allTickets  = eventRepository.getTickets()
            val allUsers    = authRepository.getAllRegisteredUsers()
            val allEvents   = eventRepository.getAllEvents()

            // Kolom 1: Peserta Aktif
            val activeTickets = allTickets.filter { it.status == "Disetujui" || it.status == "Hadir" }
            binding.tvTotalEventsJoined.text = activeTickets.size.toString()
            binding.tvLabelStat1.text = "Peserta Aktif"

            // Kolom 2: Total Akun
            binding.tvTotalSkp.text = allUsers.size.toString()
            binding.tvLabelStat2.text = "Total Akun"

            // Kolom 3: Total Event
            binding.tvTotalCertificates.text = allEvents.size.toString()
            binding.tvLabelStat3.text = "Total Event"

            // Sembunyikan menu Sertifikat dan Info Akademik
            binding.menuCertificates.visibility = View.GONE
            binding.dividerCertificates.visibility = View.GONE
            binding.menuAcademic.visibility = View.GONE
            binding.dividerAcademic.visibility = View.GONE

            // Klik kolom 1: Peserta Aktif
            binding.statCol1.setOnClickListener {
                val rows = activeTickets.map { t ->
                    RowParcel(
                        primary   = t.participantName,
                        secondary = "${t.participantNim} • ${t.participantProdi}",
                        badge     = t.status,
                        badgeBg   = if (t.status == "Hadir") "#E3F2FD" else "#E8F5E9",
                        badgeTextColor = if (t.status == "Hadir") "#0D47A1" else "#2E7D32"
                    )
                }
                AdminDetailBottomSheet.newInstance("Peserta Aktif", rows)
                    .show(parentFragmentManager, AdminDetailBottomSheet.TAG)
            }

            // Klik kolom 2: Total Akun
            binding.statCol2.setOnClickListener {
                val rows = allUsers.map { u ->
                    RowParcel(
                        primary   = u.namaLengkap,
                        secondary = "${u.nim} • ${u.prodi}",
                        badge     = if (u.isAdmin) "Admin" else "Mahasiswa",
                        badgeBg   = if (u.isAdmin) "#FFF3E0" else "#E3F2FD",
                        badgeTextColor = if (u.isAdmin) "#E65100" else "#0D47A1"
                    )
                }
                AdminDetailBottomSheet.newInstance("Semua Akun Terdaftar", rows)
                    .show(parentFragmentManager, AdminDetailBottomSheet.TAG)
            }

            // Klik kolom 3: Total Event
            binding.statCol3.setOnClickListener {
                val rows = allEvents.map { e ->
                    RowParcel(
                        primary   = e.title,
                        secondary = "${e.category} • ${e.registeredCount}/${e.quota} peserta",
                        badge     = e.category,
                        badgeBg   = when (e.category.lowercase()) {
                            "seminar"     -> "#E3F2FD"
                            "workshop"    -> "#FFF3E0"
                            "lomba"       -> "#FCE4EC"
                            "kepanitiaan" -> "#E8F5E9"
                            else          -> "#F3E5F5"
                        },
                        badgeTextColor = when (e.category.lowercase()) {
                            "seminar"     -> "#0D47A1"
                            "workshop"    -> "#E65100"
                            "lomba"       -> "#880E4F"
                            "kepanitiaan" -> "#1B5E20"
                            else          -> "#4A148C"
                        }
                    )
                }
                AdminDetailBottomSheet.newInstance("Semua Kegiatan", rows)
                    .show(parentFragmentManager, AdminDetailBottomSheet.TAG)
            }

        } else {
            binding.cardStats.visibility = View.VISIBLE
            binding.menuCertificates.visibility = View.VISIBLE
            binding.dividerCertificates.visibility = View.VISIBLE
            binding.menuAcademic.visibility = View.VISIBLE
            binding.dividerAcademic.visibility = View.VISIBLE

            // Pulihkan label default untuk user biasa
            binding.tvLabelStat1.text = getString(R.string.profile_stat_events)
            binding.tvLabelStat2.text = getString(R.string.profile_stat_skp)
            binding.tvLabelStat3.text = getString(R.string.profile_stat_cert)

            val nim = currentUser?.nim.orEmpty()
            val tickets = if (nim.isNotEmpty()) eventRepository.getMyTickets(nim) else emptyList()
            binding.tvTotalEventsJoined.text = tickets.size.toString()
            // SKP akurat berdasarkan kategori event (hanya tiket Disetujui/Hadir)
            binding.tvTotalSkp.text = SkpHelper.totalSkp(tickets).toString()
            // E-Sertifikat = tiket yang sudah berhak sertifikat
            binding.tvTotalCertificates.text = tickets.count { it.isEligibleForCertificate }.toString()
        }
    }

    private fun setupListeners() {
        binding.menuCertificates.setOnClickListener {
            startActivity(Intent(requireContext(), MyCertificatesActivity::class.java))
        }

        binding.menuAcademic.setOnClickListener {
            Toast.makeText(requireContext(), "Portal SKP Terintegrasi Sistem Informasi Akademik Kampus", Toast.LENGTH_SHORT).show()
        }

        binding.menuHelp.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Tentang EventCampus")
                .setMessage("EventCampus v1.0\nAplikasi Informasi dan Pendaftaran Kegiatan Kampus Berbasis Android.\n\nDibuat Oleh Nasyiruddin untuk Pemrograman Mobile - Universitas Malikussaleh (UNIMAL).")
                .setPositiveButton("Tutup", null)
                .show()
        }

        // Buka WebView portal web kampus UNIMAL
        binding.menuCampusWeb.setOnClickListener {
            val intent = Intent(requireContext(), CampusWebActivity::class.java).apply {
                putExtra(CampusWebActivity.EXTRA_URL, "http://portal.unimal.ac.id/index.php")
                putExtra(CampusWebActivity.EXTRA_TITLE, "Portal UNIMAL")
            }
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari akun EventCampus Anda?")
                .setPositiveButton("Ya, Keluar") { _, _ ->
                    authRepository.logout()
                    Toast.makeText(requireContext(), "Anda telah berhasil logout.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    activity?.finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnResetData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset Data Tiket?")
                .setMessage("Apakah Anda yakin ingin mereset seluruh tiket pendaftaran yang tersimpan di perangkat ini?")
                .setPositiveButton("Ya, Reset") { _, _ ->
                    eventRepository.clearAllTickets()
                    setupProfileData()
                    Toast.makeText(requireContext(), "Data tiket berhasil direset.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    /**
     * Meminta izin POST_NOTIFICATIONS pada Android 13+ (API 33).
     * Pada versi lebih lama izin ini tidak diperlukan.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                // Tampilkan rationale singkat sebelum meminta izin
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Izinkan Notifikasi")
                        .setMessage("EventKampus membutuhkan izin notifikasi untuk mengirimkan konfirmasi pendaftaran dan pengingat jadwal kegiatan kampus.")
                        .setPositiveButton("Izinkan") { _, _ ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Nanti", null)
                        .show()
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
