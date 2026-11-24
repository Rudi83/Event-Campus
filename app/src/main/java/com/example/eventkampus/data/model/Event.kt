package com.example.eventkampus.data.model

import java.io.Serializable

data class Event(
    val id: String,
    val title: String,
    val category: String,
    val organizer: String,
    val speaker: String,
    val dateTime: String,
    val location: String,
    val isOnline: Boolean,
    val quota: Int,
    var registeredCount: Int,
    val price: Double,
    val description: String,
    val benefits: List<String>,
    val terms: List<String>,
    val startEpoch: Long = 0L,  // epoch millis waktu mulai event (0 = belum diset)
    val endEpoch: Long = 0L     // epoch millis waktu selesai event
) : Serializable {
    val isFree: Boolean
        get() = price <= 0.0

    val remainingQuota: Int
        get() = (quota - registeredCount).coerceAtLeast(0)

    val formattedPrice: String
        get() = if (isFree) "Gratis" else "Rp %,d".format(price.toLong()).replace(',', '.')

    /** Apakah waktu epoch sudah diset oleh admin */
    val hasSchedule: Boolean
        get() = startEpoch > 0L && endEpoch > startEpoch

    /** Apakah saat ini sedang dalam rentang 10 menit terakhir sebelum event selesai */
    fun isAttendanceWindowOpen(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!hasSchedule) return false
        val tenMinBefore = endEpoch - 10 * 60 * 1000L
        return nowMillis in tenMinBefore..endEpoch
    }
}