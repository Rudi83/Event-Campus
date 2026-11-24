package com.example.eventkampus.ui.admin

import java.io.Serializable

data class RowParcel(
    val primary: String,
    val secondary: String,
    val badge: String? = null,
    val badgeBg: String = "#E3F2FD",
    val badgeTextColor: String = "#0D47A1"
) : Serializable
