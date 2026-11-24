package com.example.eventkampus.data.model

import java.io.Serializable

data class User(
    val id: String,
    val namaLengkap: String,
    val nim: String,
    val email: String,
    val prodi: String,
    val noWhatsapp: String,
    val password: String = "",
    val role: String = "user" // "admin" atau "user"
) : Serializable {
    val initial: String
        get() = namaLengkap.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "EK" }

    val isAdmin: Boolean
        get() = role.equals("admin", ignoreCase = true)
}
