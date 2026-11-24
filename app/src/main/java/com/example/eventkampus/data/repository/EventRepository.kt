package com.example.eventkampus.data.repository

import android.content.Context
import com.example.eventkampus.data.db.EventDatabaseHelper
import com.example.eventkampus.data.model.Category
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.model.Ticket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventRepository private constructor(context: Context) {

    private val dbHelper = EventDatabaseHelper.getInstance(context)

    fun getCategories(): List<Category> {
        return listOf(
            Category("all", "Semua", true),
            Category("Seminar", "Seminar"),
            Category("Workshop", "Workshop"),
            Category("Lomba", "Lomba"),
            Category("Kepanitiaan", "Kepanitiaan")
        )
    }

    /**
     * CREATE: Tambah kegiatan baru ke penyimpanan persisten SQLite
     */
    fun createEvent(event: Event): Boolean {
        return dbHelper.insertEvent(event) != -1L
    }

    /**
     * READ: Ambil seluruh kegiatan dari penyimpanan persisten SQLite
     */
    fun getAllEvents(): List<Event> {
        return dbHelper.getAllEvents()
    }

    /**
     * READ: Ambil kegiatan berdasarkan ID
     */
    fun getEventById(id: String): Event? {
        return dbHelper.getEventById(id)
    }

    /**
     * READ: Filter dan pencarian kegiatan
     */
    fun filterEvents(category: String?, query: String?): List<Event> {
        return dbHelper.filterEvents(category, query)
    }

    /**
     * UPDATE: Perbarui data kegiatan di penyimpanan persisten SQLite
     */
    fun updateEvent(event: Event): Boolean {
        return dbHelper.updateEvent(event) > 0
    }

    /**
     * DELETE: Hapus data kegiatan dari penyimpanan persisten SQLite
     */
    fun deleteEvent(id: String): Boolean {
        return dbHelper.deleteEvent(id) > 0
    }

    /**
     * Periksa apakah pengguna sudah mendaftar kegiatan tertentu (filter per NIM)
     */
    fun isUserRegistered(eventId: String, nim: String): Boolean {
        return getMyTickets(nim).any { it.eventId == eventId }
    }

    /**
     * Mendaftarkan pengguna ke kegiatan dan menyimpan tiket ke SQLite.
     * Melempar IllegalStateException jika kuota sudah habis atau sudah terdaftar.
     */
    fun registerEvent(
        event: Event,
        participantName: String,
        nim: String,
        prodi: String
    ): Ticket {
        // Guard: cek ulang ketersediaan kuota dari database (sumber kebenaran)
        val freshEvent = dbHelper.getEventById(event.id)
            ?: throw IllegalStateException("Kegiatan tidak ditemukan di database")

        if (freshEvent.remainingQuota <= 0) {
            throw IllegalStateException("Kuota kegiatan '${freshEvent.title}' sudah penuh")
        }

        // Guard: cek duplikasi tiket berdasarkan NIM + eventId
        val existingTickets = dbHelper.getAllTickets()
        if (existingTickets.any { it.eventId == freshEvent.id && it.participantNim == nim }) {
            throw IllegalStateException("NIM $nim sudah terdaftar pada kegiatan ini")
        }
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val currentDate = dateFormat.format(Date())
        val ticketId = "TCK-${System.currentTimeMillis().toString().takeLast(6)}"
        val qrToken = "EVENTCAMPUS|${freshEvent.id}|$ticketId|$nim"

        val ticket = Ticket(
            ticketId = ticketId,
            eventId = freshEvent.id,
            eventTitle = freshEvent.title,
            eventCategory = freshEvent.category,
            eventDateTime = freshEvent.dateTime,
            eventLocation = freshEvent.location,
            participantName = participantName,
            participantNim = nim,
            participantProdi = prodi,
            qrToken = qrToken,
            registeredDate = currentDate,
            status = "Terdaftar",
            price = freshEvent.price
        )

        // Increment count di SQLite
        dbHelper.incrementRegisteredCount(freshEvent.id)
        // Simpan tiket di SQLite
        dbHelper.insertTicket(ticket)

        return ticket
    }

    /**
     * READ: Mengambil seluruh tiket (admin only — untuk statistik & kelola peserta)
     */
    fun getTickets(): List<Ticket> {
        return dbHelper.getAllTickets()
    }

    /**
     * READ: Mengambil tiket milik user tertentu berdasarkan NIM (untuk tab Tiket Saya)
     */
    fun getMyTickets(nim: String): List<Ticket> {
        return dbHelper.getTicketsByNim(nim)
    }

    /**
     * READ: Mengambil semua tiket untuk satu event tertentu (admin)
     */
    fun getTicketsByEvent(eventId: String): List<Ticket> {
        return dbHelper.getTicketsByEventId(eventId)
    }

    /**
     * UPDATE: Perbarui status tiket — digunakan admin untuk approve/reject peserta
     * Status valid: "Terdaftar", "Disetujui", "Ditolak", "Hadir", "Batal"
     */
    fun updateTicketStatus(ticketId: String, newStatus: String): Boolean {
        return dbHelper.updateTicketStatus(ticketId, newStatus) > 0
    }

    /**
     * DELETE: Batalkan pendaftaran tiket dari SQLite
     */
    fun cancelTicket(ticketId: String): Boolean {
        return dbHelper.deleteTicket(ticketId) > 0
    }

    /**
     * Reset semua tiket di SQLite
     */
    fun clearAllTickets() {
        dbHelper.clearAllTickets()
    }

    companion object {
        @Volatile
        private var instance: EventRepository? = null

        fun getInstance(context: Context): EventRepository {
            return instance ?: synchronized(this) {
                instance ?: EventRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
