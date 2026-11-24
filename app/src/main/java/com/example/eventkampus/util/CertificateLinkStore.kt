package com.example.eventkampus.util

import android.content.Context

/**
 * Menyimpan link Google Drive sertifikat per eventId.
 * Admin input sekali per event → semua peserta yang eligible otomatis bisa mengaksesnya.
 */
object CertificateLinkStore {

    private const val PREF_NAME = "certificate_links"

    fun saveLink(context: Context, eventId: String, link: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(eventId, link.trim())
            .apply()
    }

    fun getLink(context: Context, eventId: String): String? {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(eventId, null)
        return if (raw.isNullOrBlank()) null else raw
    }

    fun hasLink(context: Context, eventId: String): Boolean =
        getLink(context, eventId) != null

    /** Ambil semua link yang tersimpan (eventId → link) */
    fun getAllLinks(context: Context): Map<String, String> {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .all
            .filterValues { it is String }
            .mapValues { it.value as String }
    }
}
