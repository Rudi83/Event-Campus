package com.example.eventkampus.ui.manage

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.ActivityAddEditEventBinding
import com.example.eventkampus.ui.map.LocationPickerActivity
import com.example.eventkampus.util.EventScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditEventBinding
    private lateinit var repository: EventRepository

    private var existingEvent: Event? = null
    private var isEditMode: Boolean = false
    private var isSaveInProgress = false

    // Simpan epoch dari picker agar bisa dibaca saat saveEvent()
    private var pickedStartEpoch = 0L
    private var pickedEndEpoch   = 0L

    // Koordinat lokasi dari Maps (opsional, untuk info saja)
    private var pickedLatLng: Pair<Double, Double>? = null

    // Launcher ke LocationPickerActivity
    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val address = result.data?.getStringExtra(LocationPickerActivity.RESULT_ADDRESS)
            val lat     = result.data?.getDoubleExtra(LocationPickerActivity.RESULT_LAT, 0.0) ?: 0.0
            val lng     = result.data?.getDoubleExtra(LocationPickerActivity.RESULT_LNG, 0.0) ?: 0.0
            if (!address.isNullOrBlank()) {
                binding.etEventLocation.setText(address)
                pickedLatLng = Pair(lat, lng)
                binding.tilEventLocation.error = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddEditEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        repository = EventRepository.getInstance(this)

        @Suppress("DEPRECATION")
        existingEvent = intent.getSerializableExtra(EXTRA_EVENT) as? Event
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        if (existingEvent == null && !eventId.isNullOrBlank()) {
            existingEvent = repository.getEventById(eventId)
        }
        isEditMode = existingEvent != null

        setupToolbar()
        setupCategoryDropdown()
        if (isEditMode) setupEditMode()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbarAddEdit.title = if (isEditMode) "Edit Kegiatan" else "Tambah Kegiatan Baru"
        binding.toolbarAddEdit.setNavigationOnClickListener { finish() }
    }

    private fun setupCategoryDropdown() {
        val categories = listOf(
            "Seminar", "Workshop", "Lomba", "Kepanitiaan",
            "Webinar", "Kuliah Umum", "Pelatihan"
        )
        binding.actvEventCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        )
    }

    private fun setupEditMode() {
        val event = existingEvent ?: return
        binding.tvFormHeaderTitle.text = "Perbarui Data Kegiatan"
        binding.tvFormHeaderSubtitle.text =
            "Ubah informasi kegiatan '${event.title}' pada database SQLite."
        binding.btnSaveEvent.text = "Simpan Perubahan"

        binding.etEventTitle.setText(event.title)
        binding.actvEventCategory.setText(event.category, false)
        binding.etEventOrganizer.setText(event.organizer)
        binding.etEventSpeaker.setText(event.speaker)
        binding.etEventDateTime.setText(event.dateTime)
        binding.etEventLocation.setText(event.location)

        if (event.isOnline) binding.rbFormatOnline.isChecked = true
        else binding.rbFormatOffline.isChecked = true

        // Set visibility awal lokasi sesuai format di mode edit
        binding.layoutLocation.visibility = if (event.isOnline) View.GONE else View.VISIBLE

        binding.etEventQuota.setText(event.quota.toString())
        binding.etEventPrice.setText(if (event.price <= 0.0) "0" else event.price.toLong().toString())
        binding.etEventDescription.setText(event.description)

        if (event.benefits.isNotEmpty()) {
            binding.etEventBenefits.setText(event.benefits.joinToString(separator = "\n"))
        }
        if (event.terms.isNotEmpty()) {
            binding.etEventTerms.setText(event.terms.joinToString(separator = "\n"))
        }

        // Populate waktu jika sudah diset
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        if (event.startEpoch > 0L) {
            pickedStartEpoch = event.startEpoch
            binding.etEventStartTime.setText(sdf.format(event.startEpoch))
        }
        if (event.endEpoch > 0L) {
            pickedEndEpoch = event.endEpoch
            binding.etEventEndTime.setText(sdf.format(event.endEpoch))
        }
    }

    private fun setupListeners() {
        binding.btnSaveEvent.setOnClickListener {
            if (!isSaveInProgress) saveEvent()
        }

        // ── Format Pelaksanaan → tampilkan/sembunyikan lokasi ─────────────────
        updateLocationVisibility() // set state awal
        binding.rgEventFormat.setOnCheckedChangeListener { _, _ ->
            updateLocationVisibility()
        }

        // ── Tombol pilih lokasi dari Maps ─────────────────────────────────────
        binding.btnPickLocation.setOnClickListener {
            openMapsForLocationPick()
        }

        // Field tanggal/waktu kegiatan (teks deskriptif) → DateTimePicker → isi otomatis
        binding.etEventDateTime.isFocusable = false
        binding.etEventDateTime.isClickable = true
        binding.etEventDateTime.setOnClickListener {
            showDateTimePicker(isStart = null) { displayText, _ ->
                binding.etEventDateTime.setText(displayText)
                binding.tilEventDateTime.error = null
            }
        }

        // Waktu mulai event (epoch)
        binding.etEventStartTime.isFocusable = false
        binding.etEventStartTime.isClickable = true
        binding.etEventStartTime.setOnClickListener {
            showDateTimePicker(isStart = true) { displayText, epochMillis ->
                binding.etEventStartTime.setText(displayText)
                pickedStartEpoch = epochMillis
                binding.tilEventStartTime.error = null
                // Jika end belum diset, otomatis saran +2 jam
                if (pickedEndEpoch == 0L) {
                    val suggestEnd = epochMillis + 2 * 60 * 60 * 1000L
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
                    binding.etEventEndTime.setText(sdf.format(suggestEnd))
                    pickedEndEpoch = suggestEnd
                }
                // Update field display jika start diisi
                syncDateTimeDisplay()
            }
        }

        // Waktu selesai event (epoch)
        binding.etEventEndTime.isFocusable = false
        binding.etEventEndTime.isClickable = true
        binding.etEventEndTime.setOnClickListener {
            showDateTimePicker(isStart = false) { displayText, epochMillis ->
                binding.etEventEndTime.setText(displayText)
                pickedEndEpoch = epochMillis
                binding.tilEventEndTime.error = null
                syncDateTimeDisplay()
            }
        }
    }

    // ─── Format & Lokasi ──────────────────────────────────────────────────────

    private fun updateLocationVisibility() {
        val isOffline = binding.rbFormatOffline.isChecked
        binding.layoutLocation.visibility = if (isOffline) View.VISIBLE else View.GONE
        if (!isOffline) {
            // Daring: kosongkan lokasi agar tidak ada nilai offline yang tertinggal
            binding.etEventLocation.setText("")
        }
    }

    /**
     * Buka LocationPickerActivity — peta interaktif OSM untuk memilih lokasi presisi.
     * Jika sudah ada koordinat sebelumnya, kirim sebagai titik awal.
     */
    private fun openMapsForLocationPick() {
        val intent = Intent(this, LocationPickerActivity::class.java)
        pickedLatLng?.let { (lat, lng) ->
            intent.putExtra(LocationPickerActivity.EXTRA_LAT, lat)
            intent.putExtra(LocationPickerActivity.EXTRA_LNG, lng)
        }
        locationPickerLauncher.launch(intent)
    }

    // ─── Date + Time Picker ───────────────────────────────────────────────────

    /**
     * Tampilkan DatePickerDialog → lalu TimePickerDialog secara berurutan.
     * [isStart] = true (waktu mulai), false (waktu selesai), null (teks deskriptif)
     * Callback: (displayText, epochMillis)
     */
    private fun showDateTimePicker(
        isStart: Boolean?,
        onPicked: (displayText: String, epochMillis: Long) -> Unit
    ) {
        val cal = Calendar.getInstance()

        // Jika edit mode, pre-fill calendar dari nilai yang sudah ada
        when (isStart) {
            true  -> if (pickedStartEpoch > 0) cal.timeInMillis = pickedStartEpoch
            false -> if (pickedEndEpoch > 0)   cal.timeInMillis = pickedEndEpoch
            null  -> { /* gunakan waktu sekarang sebagai default */ }
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                // Setelah tanggal dipilih, buka TimePickerDialog
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        cal.set(year, month, day, hour, minute, 0)
                        cal.set(Calendar.MILLISECOND, 0)

                        val epochMillis = cal.timeInMillis

                        // Format tampilan
                        val dayNames  = listOf("Minggu","Senin","Selasa","Rabu","Kamis","Jumat","Sabtu")
                        val monthNames = listOf("Jan","Feb","Mar","Apr","Mei","Jun",
                                                "Jul","Agu","Sep","Okt","Nov","Des")
                        val dayName   = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                        val monthName = monthNames[month]
                        val timeStr   = "%02d:%02d".format(hour, minute)
                        val dateStr   = "$dayName, $day $monthName $year"

                        val displayText = when (isStart) {
                            null  -> "$dateStr • $timeStr WIB"   // untuk field teks deskriptif
                            true  -> "$day/${ "%02d".format(month + 1) }/$year $timeStr"
                            false -> "$day/${ "%02d".format(month + 1) }/$year $timeStr"
                        }

                        onPicked(displayText, epochMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true // format 24 jam
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Jika start dan end sudah diisi, otomatis update field teks deskriptif
     * dengan format: "Senin, 18 Nov 2026 • 09:00 - 12:30 WIB"
     */
    private fun syncDateTimeDisplay() {
        if (pickedStartEpoch <= 0L || pickedEndEpoch <= 0L) return

        val calS = Calendar.getInstance().apply { timeInMillis = pickedStartEpoch }
        val calE = Calendar.getInstance().apply { timeInMillis = pickedEndEpoch }

        val dayNames   = listOf("Minggu","Senin","Selasa","Rabu","Kamis","Jumat","Sabtu")
        val monthNames = listOf("Jan","Feb","Mar","Apr","Mei","Jun",
                                "Jul","Agu","Sep","Okt","Nov","Des")

        val dayName  = dayNames[calS.get(Calendar.DAY_OF_WEEK) - 1]
        val day      = calS.get(Calendar.DAY_OF_MONTH)
        val month    = monthNames[calS.get(Calendar.MONTH)]
        val year     = calS.get(Calendar.YEAR)
        val startT   = "%02d:%02d".format(calS.get(Calendar.HOUR_OF_DAY), calS.get(Calendar.MINUTE))
        val endT     = "%02d:%02d".format(calE.get(Calendar.HOUR_OF_DAY), calE.get(Calendar.MINUTE))

        val current = binding.etEventDateTime.text?.toString().orEmpty()
        // Hanya update jika field masih kosong atau formatnya belum diisi manual
        if (current.isEmpty() || current.matches(Regex(".+•.+-.+WIB"))) {
            binding.etEventDateTime.setText("$dayName, $day $month $year • $startT - $endT WIB")
        }
    }

    private fun saveEvent() {
        clearFieldErrors()

        val title = binding.etEventTitle.text?.toString()?.trim().orEmpty()
        val category = binding.actvEventCategory.text?.toString()?.trim().orEmpty()
        val organizer = binding.etEventOrganizer.text?.toString()?.trim().orEmpty()
        val speaker = binding.etEventSpeaker.text?.toString()?.trim().orEmpty()
        val dateTime = binding.etEventDateTime.text?.toString()?.trim().orEmpty()
        val location = binding.etEventLocation.text?.toString()?.trim().orEmpty()
        val isOnline = binding.rbFormatOnline.isChecked
        val quotaStr = binding.etEventQuota.text?.toString()?.trim().orEmpty()
        val priceStr = binding.etEventPrice.text?.toString()?.trim().orEmpty()
        val description = binding.etEventDescription.text?.toString()?.trim().orEmpty()
        val benefitsRaw = binding.etEventBenefits.text?.toString()?.trim().orEmpty()
        val termsRaw = binding.etEventTerms.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (title.isEmpty()) {
            binding.tilEventTitle.error = "Judul kegiatan wajib diisi"
            hasError = true
        } else if (title.length < 5) {
            binding.tilEventTitle.error = "Judul terlalu pendek (minimal 5 karakter)"
            hasError = true
        } else if (title.length > 120) {
            binding.tilEventTitle.error = "Judul terlalu panjang (maksimal 120 karakter)"
            hasError = true
        }

        if (category.isEmpty()) {
            binding.tilEventCategory.error = "Pilih kategori kegiatan"
            hasError = true
        }

        if (organizer.isEmpty()) {
            binding.tilEventOrganizer.error = "Penyelenggara / Ormawa wajib diisi"
            hasError = true
        } else if (organizer.length < 3) {
            binding.tilEventOrganizer.error = "Nama penyelenggara terlalu pendek"
            hasError = true
        }

        if (speaker.isEmpty()) {
            binding.tilEventSpeaker.error = "Nama pembicara / narasumber wajib diisi"
            hasError = true
        }

        if (dateTime.isEmpty()) {
            binding.tilEventDateTime.error = "Hari, tanggal, dan waktu wajib diisi"
            hasError = true
        } else if (dateTime.length < 8) {
            binding.tilEventDateTime.error = "Format tanggal kurang lengkap"
            hasError = true
        }

        // Lokasi wajib hanya jika tatap muka (offline)
        if (!isOnline && location.isEmpty()) {
            binding.tilEventLocation.error = "Lokasi / ruangan wajib diisi untuk kegiatan tatap muka"
            hasError = true
        }

        // Validasi kuota: harus angka positif, tidak boleh lebih dari 10.000
        val quota = quotaStr.toIntOrNull()
        when {
            quota == null -> {
                binding.tilEventQuota.error = "Kuota harus berupa angka"
                hasError = true
            }
            quota <= 0 -> {
                binding.tilEventQuota.error = "Kuota minimal 1 peserta"
                hasError = true
            }
            quota > 10_000 -> {
                binding.tilEventQuota.error = "Kuota maksimal 10.000 peserta"
                hasError = true
            }
        }

        // Validasi harga: harus angka non-negatif, maksimal 10 juta
        val price = priceStr.toDoubleOrNull()
        when {
            price == null -> {
                binding.tilEventPrice.error = "Biaya HTM harus berupa angka (isi 0 jika gratis)"
                hasError = true
            }
            price < 0.0 -> {
                binding.tilEventPrice.error = "Biaya HTM tidak boleh negatif"
                hasError = true
            }
            price > 10_000_000 -> {
                binding.tilEventPrice.error = "Biaya HTM tidak wajar (maksimal Rp 10.000.000)"
                hasError = true
            }
        }

        if (description.isEmpty()) {
            binding.tilEventDescription.error = "Deskripsi kegiatan wajib diisi"
            hasError = true
        } else if (description.length < 10) {
            binding.tilEventDescription.error = "Deskripsi terlalu singkat (minimal 10 karakter)"
            hasError = true
        }

        if (hasError) {
            Toast.makeText(
                this,
                "Mohon perbaiki kolom yang masih bermasalah sebelum menyimpan",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Semua validasi lolos — set loading
        setSaveLoading(true)

        val benefitsList = if (benefitsRaw.isNotBlank()) {
            benefitsRaw.split("\n", ",")
                .map { it.trim().removePrefix("•").trim() }
                .filter { it.isNotEmpty() }
                .take(20) // batas wajar
        } else {
            listOf("E-Sertifikat Digital", "Materi Kegiatan")
        }

        val termsList = if (termsRaw.isNotBlank()) {
            termsRaw.split("\n", ",")
                .map { it.trim().removePrefix("•").trim() }
                .filter { it.isNotEmpty() }
                .take(20)
        } else {
            listOf("Mahasiswa aktif kampus", "Hadir tepat waktu")
        }

        val validatedQuota = quota!!
        val validatedPrice = price!!

        // Gunakan epoch dari picker (sudah disimpan saat user memilih via DateTimePicker)
        val startEpoch = pickedStartEpoch
        val endEpoch   = pickedEndEpoch

        if (startEpoch > 0 && endEpoch > 0 && endEpoch <= startEpoch) {
            binding.tilEventEndTime.error = "Waktu selesai harus setelah waktu mulai"
            setSaveLoading(false)
            return
        }

        val success: Boolean
        val actionLabel: String

        if (isEditMode) {
            val oldEvent = existingEvent
            if (oldEvent == null) {
                setSaveLoading(false)
                Toast.makeText(this, "Data kegiatan tidak valid. Silakan kembali dan coba lagi.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            val updatedEvent = oldEvent.copy(
                title = title,
                category = category,
                organizer = organizer,
                speaker = speaker,
                dateTime = dateTime,
                location = location,
                isOnline = isOnline,
                quota = validatedQuota,
                price = validatedPrice,
                description = description,
                benefits = benefitsList,
                terms = termsList,
                startEpoch = startEpoch,
                endEpoch = endEpoch
            )
            success = repository.updateEvent(updatedEvent)
            actionLabel = "diperbarui"
            if (success) EventScheduler.schedule(this, updatedEvent)
        } else {
            val newId = "EVT-${System.currentTimeMillis().toString().takeLast(6)}"
            val newEvent = Event(
                id = newId,
                title = title,
                category = category,
                organizer = organizer,
                speaker = speaker,
                dateTime = dateTime,
                location = location,
                isOnline = isOnline,
                quota = validatedQuota,
                registeredCount = 0,
                price = validatedPrice,
                description = description,
                benefits = benefitsList,
                terms = termsList,
                startEpoch = startEpoch,
                endEpoch = endEpoch
            )
            success = repository.createEvent(newEvent)
            actionLabel = "ditambahkan"
            if (success) EventScheduler.schedule(this, newEvent)
        }

        setSaveLoading(false)

        if (success) {
            Toast.makeText(this, "Kegiatan '$title' berhasil $actionLabel!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(
                this,
                "Gagal menyimpan ke database. Silakan coba lagi.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setSaveLoading(isLoading: Boolean) {
        isSaveInProgress = isLoading
        binding.btnSaveEvent.isEnabled = !isLoading
        binding.btnSaveEvent.text = if (isLoading) "Menyimpan…" else
            if (isEditMode) "Simpan Perubahan" else "Simpan Kegiatan"
    }

    private fun clearFieldErrors() {
        binding.tilEventTitle.error = null
        binding.tilEventCategory.error = null
        binding.tilEventOrganizer.error = null
        binding.tilEventSpeaker.error = null
        binding.tilEventDateTime.error = null
        binding.tilEventLocation.error = null
        binding.tilEventQuota.error = null
        binding.tilEventPrice.error = null
        binding.tilEventDescription.error = null
        binding.tilEventStartTime.error = null
        binding.tilEventEndTime.error = null
    }

    companion object {
        const val EXTRA_EVENT = "extra_event"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }
}
