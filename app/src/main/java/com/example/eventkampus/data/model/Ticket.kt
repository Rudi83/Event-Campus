package com.example.eventkampus.data.model

import java.io.Serializable

data class Ticket(
    val ticketId: String,
    val eventId: String,
    val eventTitle: String,
    val eventCategory: String,
    val eventDateTime: String,
    val eventLocation: String,
    val participantName: String,
    val participantNim: String,
    val participantProdi: String,
    val qrToken: String,
    val registeredDate: String,
    val status: String = "Terdaftar", // Terdaftar, Disetujui, Ditolak, Hadir, Batal
    val price: Double = 0.0
) : Serializable