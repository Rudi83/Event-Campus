package com.example.eventkampus.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.model.Ticket

class EventDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "event_kampus.db"
        const val DATABASE_VERSION = 2  // dinaikkan untuk migrasi tambah kolom waktu

        // Table Events
        const val TABLE_EVENTS = "events"
        const val COL_EVENT_ID = "id"
        const val COL_EVENT_TITLE = "title"
        const val COL_EVENT_CATEGORY = "category"
        const val COL_EVENT_ORGANIZER = "organizer"
        const val COL_EVENT_SPEAKER = "speaker"
        const val COL_EVENT_DATE_TIME = "date_time"
        const val COL_EVENT_LOCATION = "location"
        const val COL_EVENT_IS_ONLINE = "is_online"
        const val COL_EVENT_QUOTA = "quota"
        const val COL_EVENT_REGISTERED = "registered_count"
        const val COL_EVENT_PRICE = "price"
        const val COL_EVENT_DESCRIPTION = "description"
        const val COL_EVENT_BENEFITS = "benefits"
        const val COL_EVENT_TERMS = "terms"
        const val COL_EVENT_CREATED_AT = "created_at"
        // Waktu mulai & selesai event dalam epoch millis (0 = belum diset)
        const val COL_EVENT_START_EPOCH = "start_epoch"
        const val COL_EVENT_END_EPOCH   = "end_epoch"

        // Table Tickets
        const val TABLE_TICKETS = "tickets"
        const val COL_TICKET_ID = "ticket_id"
        const val COL_TICKET_EVENT_ID = "event_id"
        const val COL_TICKET_TITLE = "event_title"
        const val COL_TICKET_CATEGORY = "event_category"
        const val COL_TICKET_DATE_TIME = "event_date_time"
        const val COL_TICKET_LOCATION = "event_location"
        const val COL_TICKET_NAME = "participant_name"
        const val COL_TICKET_NIM = "participant_nim"
        const val COL_TICKET_PRODI = "participant_prodi"
        const val COL_TICKET_QR = "qr_token"
        const val COL_TICKET_DATE = "registered_date"
        const val COL_TICKET_STATUS = "status"
        const val COL_TICKET_PRICE = "price"

        private const val DELIMITER = "|||"

        @Volatile
        private var instance: EventDatabaseHelper? = null

        fun getInstance(context: Context): EventDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: EventDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createEventsTable = """
            CREATE TABLE $TABLE_EVENTS (
                $COL_EVENT_ID TEXT PRIMARY KEY,
                $COL_EVENT_TITLE TEXT NOT NULL,
                $COL_EVENT_CATEGORY TEXT NOT NULL,
                $COL_EVENT_ORGANIZER TEXT NOT NULL,
                $COL_EVENT_SPEAKER TEXT NOT NULL,
                $COL_EVENT_DATE_TIME TEXT NOT NULL,
                $COL_EVENT_LOCATION TEXT NOT NULL,
                $COL_EVENT_IS_ONLINE INTEGER NOT NULL DEFAULT 0,
                $COL_EVENT_QUOTA INTEGER NOT NULL,
                $COL_EVENT_REGISTERED INTEGER NOT NULL DEFAULT 0,
                $COL_EVENT_PRICE REAL NOT NULL DEFAULT 0.0,
                $COL_EVENT_DESCRIPTION TEXT NOT NULL,
                $COL_EVENT_BENEFITS TEXT,
                $COL_EVENT_TERMS TEXT,
                $COL_EVENT_CREATED_AT INTEGER NOT NULL,
                $COL_EVENT_START_EPOCH INTEGER NOT NULL DEFAULT 0,
                $COL_EVENT_END_EPOCH INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()

        val createTicketsTable = """
            CREATE TABLE $TABLE_TICKETS (
                $COL_TICKET_ID TEXT PRIMARY KEY,
                $COL_TICKET_EVENT_ID TEXT NOT NULL,
                $COL_TICKET_TITLE TEXT NOT NULL,
                $COL_TICKET_CATEGORY TEXT NOT NULL,
                $COL_TICKET_DATE_TIME TEXT NOT NULL,
                $COL_TICKET_LOCATION TEXT NOT NULL,
                $COL_TICKET_NAME TEXT NOT NULL,
                $COL_TICKET_NIM TEXT NOT NULL,
                $COL_TICKET_PRODI TEXT NOT NULL,
                $COL_TICKET_QR TEXT NOT NULL,
                $COL_TICKET_DATE TEXT NOT NULL,
                $COL_TICKET_STATUS TEXT NOT NULL,
                $COL_TICKET_PRICE REAL NOT NULL DEFAULT 0.0
            )
        """.trimIndent()

        db.execSQL(createEventsTable)
        db.execSQL(createTicketsTable)

        // Seed initial campus events
        seedInitialEvents(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Tambah kolom waktu epoch tanpa menghapus data
            db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $COL_EVENT_START_EPOCH INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $COL_EVENT_END_EPOCH INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun seedInitialEvents(db: SQLiteDatabase) {
        val seedEvents = listOf(
            Event(
                id = "EVT-001",
                title = "Seminar Nasional AI & Perkembangan Masa Depan",
                category = "Seminar",
                organizer = "Himpunan Mahasiswa Teknik Informatika (HIMATIF)",
                speaker = "Dr. Pratama Yudha, M.Kom (AI Research Scientist)",
                dateTime = "Senin, 18 Nov 2026 • 09:00 - 12:30 WIB",
                location = "Auditorium Utama Gedung Rektorat Lt. 3",
                isOnline = false,
                quota = 250,
                registeredCount = 184,
                price = 0.0,
                description = "Seminar ini mengupas tuntas transformasi Artificial Intelligence (Generative AI & Machine Learning) di dunia kerja serta persiapan kompetensi lulusan IT masa kini.",
                benefits = listOf("E-Sertifikat Bernilai SKP 4 Poin", "Snack & Lunch Box", "Materi Presentasi PDF", "Doorprize Menarik"),
                terms = listOf("Mahasiswa aktif seluruh fakultas", "Membawa KTM pada hari-H", "Hadir 15 menit sebelum pembukaan")
            ),
            Event(
                id = "EVT-002",
                title = "Hands-on Workshop: Android App Development with Kotlin",
                category = "Workshop",
                organizer = "Google Developer Student Clubs (GDSC) Kampus",
                speaker = "Ahmad Nasyir & Tim Android Lead",
                dateTime = "Rabu, 20 Nov 2026 • 13:30 - 17:00 WIB",
                location = "Laboratorium Komputer Rekayasa Perangkat Lunak",
                isOnline = false,
                quota = 50,
                registeredCount = 42,
                price = 25000.0,
                description = "Pelatihan intensif praktik membuat aplikasi Android modern dari dasar hingga integrasi API dan arsitektur MVVM. Setiap peserta akan membawa pulang prototype aplikasi mereka sendiri.",
                benefits = listOf("E-Sertifikat Kompetensi", "Modul Praktikum Lengkap", "Source Code Proyek", "Relasi Komunitas Developer"),
                terms = listOf("Membawa laptop masing-masing", "Telah menginstal Android Studio", "Membayar HTM Rp 25.000")
            ),
            Event(
                id = "EVT-003",
                title = "National UI/UX Design Competition 2026",
                category = "Lomba",
                organizer = "Badan Eksekutif Mahasiswa (BEM) Fakultas",
                speaker = "Dewan Juri Profesional Industri Startup",
                dateTime = "Sabtu, 28 Nov 2026 • 08:00 WIB - Selesai",
                location = "Online via Zoom & Figma",
                isOnline = true,
                quota = 100,
                registeredCount = 68,
                price = 50000.0,
                description = "Kompetisi desain produk digital tingkat mahasiswa nasional bertema 'Solusi Digital Inovatif untuk Permasalahan Sosial Kampus'. Total hadiah jutaan rupiah beserta trofi.",
                benefits = listOf("Total Hadiah Rp 7.500.000 + Sertifikat Juara", "Sertifikat Peserta Nasional", "Feedback Portofolio dari Juri Profesional"),
                terms = listOf("Tim beranggotakan 2 - 3 mahasiswa", "Mengirimkan proposal dan link Figma Prototype", "KTM aktif tiap anggota")
            ),
            Event(
                id = "EVT-004",
                title = "Mastering Public Speaking & Leadership Bootcamp",
                category = "Workshop",
                organizer = "Unit Kegiatan Mahasiswa (UKM) Protokoler",
                speaker = "Rina Maharani, S.I.Kom (News Anchor & Trainer)",
                dateTime = "Jumat, 23 Nov 2026 • 08:30 - 11:45 WIB",
                location = "Ruang Teater Kampus Bukit Indah",
                isOnline = false,
                quota = 80,
                registeredCount = 75,
                price = 0.0,
                description = "Tingkatkan rasa percaya diri berbicara di depan umum, teknik vocal imaging, body language, serta kemampuan memimpin forum diskusi dan sidang organisasi.",
                benefits = listOf("E-Sertifikat Resmi", "Simulasi Langsung di Panggung", "Koleksi E-Book Public Speaking"),
                terms = listOf("Pakaian rapi kemeja berkerah", "Hadir tepat waktu")
            ),
            Event(
                id = "EVT-005",
                title = "Open Recruitment Panitia Festival Budaya & Musik",
                category = "Kepanitiaan",
                organizer = "Dewan Perwakilan Mahasiswa (DPM)",
                speaker = "Koordinator Steering Committee",
                dateTime = "Senin, 26 Nov 2026 • 10:00 - 16:00 WIB",
                location = "Student Center Kampus",
                isOnline = false,
                quota = 120,
                registeredCount = 95,
                price = 0.0,
                description = "Bergabunglah menjadi bagian dari pagelaran seni dan budaya terbesar kampus. Divisi yang dibuka: Acara, Humas, Publikasi & Dokumentasi, Logistik, dan Dana Usaha.",
                benefits = listOf("Sertifikat Panitia Kampus", "Poin SKP Keaktifan Organisasi", "Pengalaman Manajemen Event Besar"),
                terms = listOf("Mahasiswa semester 1 - 5", "Komitmen dan loyal terhadap tim")
            ),
            Event(
                id = "EVT-006",
                title = "Web Fullstack Development: Next.js & Cloud",
                category = "Workshop",
                organizer = "Club Pemrograman & Robotika",
                speaker = "Budi Wicaksono (Senior Fullstack Engineer)",
                dateTime = "Selasa, 02 Des 2026 • 19:30 - 21:30 WIB",
                location = "Online via Zoom Meeting",
                isOnline = true,
                quota = 300,
                registeredCount = 210,
                price = 0.0,
                description = "Webinar teknis membangun aplikasi web skala enterprise dari frontend modern hingga deployment ke cloud server.",
                benefits = listOf("E-Sertifikat Digital", "Recording Video Sesi", "Grup Diskusi Mentoring"),
                terms = listOf("Koneksi internet stabil", "Terbuka untuk umum dan mahasiswa")
            )
        )

        for (event in seedEvents) {
            val values = ContentValues().apply {
                put(COL_EVENT_ID, event.id)
                put(COL_EVENT_TITLE, event.title)
                put(COL_EVENT_CATEGORY, event.category)
                put(COL_EVENT_ORGANIZER, event.organizer)
                put(COL_EVENT_SPEAKER, event.speaker)
                put(COL_EVENT_DATE_TIME, event.dateTime)
                put(COL_EVENT_LOCATION, event.location)
                put(COL_EVENT_IS_ONLINE, if (event.isOnline) 1 else 0)
                put(COL_EVENT_QUOTA, event.quota)
                put(COL_EVENT_REGISTERED, event.registeredCount)
                put(COL_EVENT_PRICE, event.price)
                put(COL_EVENT_DESCRIPTION, event.description)
                put(COL_EVENT_BENEFITS, event.benefits.joinToString(DELIMITER))
                put(COL_EVENT_TERMS, event.terms.joinToString(DELIMITER))
                put(COL_EVENT_CREATED_AT, System.currentTimeMillis())
            }
            db.insert(TABLE_EVENTS, null, values)
        }
    }

    // ==========================================
    // CRUD: EVENTS (CREATE, READ, UPDATE, DELETE)
    // ==========================================

    fun insertEvent(event: Event): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_ID, event.id)
            put(COL_EVENT_TITLE, event.title)
            put(COL_EVENT_CATEGORY, event.category)
            put(COL_EVENT_ORGANIZER, event.organizer)
            put(COL_EVENT_SPEAKER, event.speaker)
            put(COL_EVENT_DATE_TIME, event.dateTime)
            put(COL_EVENT_LOCATION, event.location)
            put(COL_EVENT_IS_ONLINE, if (event.isOnline) 1 else 0)
            put(COL_EVENT_QUOTA, event.quota)
            put(COL_EVENT_REGISTERED, event.registeredCount)
            put(COL_EVENT_PRICE, event.price)
            put(COL_EVENT_DESCRIPTION, event.description)
            put(COL_EVENT_BENEFITS, event.benefits.joinToString(DELIMITER))
            put(COL_EVENT_TERMS, event.terms.joinToString(DELIMITER))
            put(COL_EVENT_CREATED_AT, System.currentTimeMillis())
            put(COL_EVENT_START_EPOCH, event.startEpoch)
            put(COL_EVENT_END_EPOCH, event.endEpoch)
        }
        return db.insertWithOnConflict(TABLE_EVENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * READ: Mengambil seluruh daftar kegiatan dari SQLite
     */
    fun getAllEvents(): List<Event> {
        val list = mutableListOf<Event>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_EVENT_CREATED_AT DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToEvent(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    /**
     * READ BY ID: Mengambil satu kegiatan berdasarkan ID
     */
    fun getEventById(id: String): Event? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$COL_EVENT_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToEvent(it)
            }
        }
        return null
    }

    /**
     * READ: Filter dan pencarian kegiatan
     */
    fun filterEvents(category: String?, query: String?): List<Event> {
        val allEvents = getAllEvents()
        return allEvents.filter { event ->
            val matchCategory = category.isNullOrEmpty() ||
                    category.equals("all", true) ||
                    category.equals("Semua", true) ||
                    event.category.equals(category, true)

            val matchQuery = query.isNullOrBlank() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.organizer.contains(query, ignoreCase = true) ||
                    event.speaker.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true)

            matchCategory && matchQuery
        }
    }

    /**
     * UPDATE: Memperbarui data kegiatan yang ada di SQLite
     */
    fun updateEvent(event: Event): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_TITLE, event.title)
            put(COL_EVENT_CATEGORY, event.category)
            put(COL_EVENT_ORGANIZER, event.organizer)
            put(COL_EVENT_SPEAKER, event.speaker)
            put(COL_EVENT_DATE_TIME, event.dateTime)
            put(COL_EVENT_LOCATION, event.location)
            put(COL_EVENT_IS_ONLINE, if (event.isOnline) 1 else 0)
            put(COL_EVENT_QUOTA, event.quota)
            put(COL_EVENT_REGISTERED, event.registeredCount)
            put(COL_EVENT_PRICE, event.price)
            put(COL_EVENT_DESCRIPTION, event.description)
            put(COL_EVENT_BENEFITS, event.benefits.joinToString(DELIMITER))
            put(COL_EVENT_TERMS, event.terms.joinToString(DELIMITER))
            put(COL_EVENT_START_EPOCH, event.startEpoch)
            put(COL_EVENT_END_EPOCH, event.endEpoch)
        }
        return db.update(TABLE_EVENTS, values, "$COL_EVENT_ID = ?", arrayOf(event.id))
    }

    /**
     * DELETE: Menghapus kegiatan dari SQLite
     */
    fun deleteEvent(id: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_EVENTS, "$COL_EVENT_ID = ?", arrayOf(id))
    }

    fun incrementRegisteredCount(eventId: String) {
        val current = getEventById(eventId) ?: return
        val updated = current.copy(registeredCount = current.registeredCount + 1)
        updateEvent(updated)
    }

    private fun cursorToEvent(cursor: Cursor): Event {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TITLE))
        val category = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_CATEGORY))
        val organizer = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_ORGANIZER))
        val speaker = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_SPEAKER))
        val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_DATE_TIME))
        val location = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_LOCATION))
        val isOnline = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_IS_ONLINE)) == 1
        val quota = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_QUOTA))
        val registeredCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_REGISTERED))
        val price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_EVENT_PRICE))
        val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_DESCRIPTION))

        val benefitsRaw = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_BENEFITS)) ?: ""
        val benefits = if (benefitsRaw.isNotBlank()) benefitsRaw.split(DELIMITER) else emptyList()

        val termsRaw = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TERMS)) ?: ""
        val terms = if (termsRaw.isNotBlank()) termsRaw.split(DELIMITER) else emptyList()

        return Event(
            id = id,
            title = title,
            category = category,
            organizer = organizer,
            speaker = speaker,
            dateTime = dateTime,
            location = location,
            isOnline = isOnline,
            quota = quota,
            registeredCount = registeredCount,
            price = price,
            description = description,
            benefits = benefits,
            terms = terms,
            startEpoch = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_START_EPOCH)),
            endEpoch   = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_END_EPOCH))
        )
    }

    // ==========================================
    // CRUD: TICKETS (PENDAFTARAN KEGIATAN)
    // ==========================================

    fun insertTicket(ticket: Ticket): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TICKET_ID, ticket.ticketId)
            put(COL_TICKET_EVENT_ID, ticket.eventId)
            put(COL_TICKET_TITLE, ticket.eventTitle)
            put(COL_TICKET_CATEGORY, ticket.eventCategory)
            put(COL_TICKET_DATE_TIME, ticket.eventDateTime)
            put(COL_TICKET_LOCATION, ticket.eventLocation)
            put(COL_TICKET_NAME, ticket.participantName)
            put(COL_TICKET_NIM, ticket.participantNim)
            put(COL_TICKET_PRODI, ticket.participantProdi)
            put(COL_TICKET_QR, ticket.qrToken)
            put(COL_TICKET_DATE, ticket.registeredDate)
            put(COL_TICKET_STATUS, ticket.status)
            put(COL_TICKET_PRICE, ticket.price)
        }
        return db.insertWithOnConflict(TABLE_TICKETS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllTickets(): List<Ticket> {
        val list = mutableListOf<Ticket>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TICKETS, null, null, null, null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToTicket(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    /**
     * READ: Ambil tiket milik satu NIM tertentu (untuk tab "Tiket Saya" user biasa)
     */
    fun getTicketsByNim(nim: String): List<Ticket> {
        val list = mutableListOf<Ticket>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TICKETS,
            null,
            "$COL_TICKET_NIM = ?",
            arrayOf(nim),
            null, null,
            "$COL_TICKET_DATE DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToTicket(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    private fun cursorToTicket(cursor: android.database.Cursor): Ticket {
        return Ticket(
            ticketId        = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_ID)),
            eventId         = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_EVENT_ID)),
            eventTitle      = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_TITLE)),
            eventCategory   = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_CATEGORY)),
            eventDateTime   = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_DATE_TIME)),
            eventLocation   = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_LOCATION)),
            participantName = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_NAME)),
            participantNim  = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_NIM)),
            participantProdi= cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_PRODI)),
            qrToken         = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_QR)),
            registeredDate  = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_DATE)),
            status          = cursor.getString(cursor.getColumnIndexOrThrow(COL_TICKET_STATUS)),
            price           = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TICKET_PRICE))
        )
    }

    fun deleteTicket(ticketId: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_TICKETS, "$COL_TICKET_ID = ?", arrayOf(ticketId))
    }

    fun clearAllTickets() {
        val db = writableDatabase
        db.delete(TABLE_TICKETS, null, null)
    }

    /**
     * UPDATE: Perbarui status tiket tertentu (untuk fitur approve/reject oleh admin)
     */
    fun updateTicketStatus(ticketId: String, newStatus: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TICKET_STATUS, newStatus)
        }
        return db.update(TABLE_TICKETS, values, "$COL_TICKET_ID = ?", arrayOf(ticketId))
    }

    /**
     * READ: Ambil semua tiket untuk event tertentu (untuk halaman kelola peserta)
     */
    fun getTicketsByEventId(eventId: String): List<Ticket> {
        val list = mutableListOf<Ticket>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TICKETS, null,
            "$COL_TICKET_EVENT_ID = ?", arrayOf(eventId),
            null, null, "$COL_TICKET_DATE ASC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do { list.add(cursorToTicket(it)) } while (it.moveToNext())
            }
        }
        return list
    }
}
