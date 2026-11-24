package com.example.eventkampus.util

import com.example.eventkampus.data.model.Ticket

object SkpHelper {

    /**
     * Poin SKP per kategori kegiatan sesuai pedoman kemahasiswaan UNIMAL.
     * Seminar        = 3 SKP
     * Workshop       = 4 SKP
     * Lomba          = 5 SKP
     * Kepanitiaan    = 6 SKP
     * Lainnya        = 2 SKP
     */
    fun skpForCategory(category: String): Int = when (category.trim().lowercase()) {
        "seminar"     -> 3
        "workshop"    -> 4
        "lomba"       -> 5
        "kepanitiaan" -> 6
        else          -> 2
    }

    /**
     * Total SKP dari daftar tiket yang sudah berhak mendapat sertifikat
     * (status Disetujui atau Hadir).
     */
    fun totalSkp(tickets: List<Ticket>): Int =
        tickets.filter { it.isEligibleForCertificate }
               .sumOf { skpForCategory(it.eventCategory) }

    /**
     * Tiket yang berhak mendapat e-sertifikat.
     */
    val Ticket.isEligibleForCertificate: Boolean
        get() = status == "Disetujui" || status == "Hadir"
}
