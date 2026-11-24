package com.example.eventkampus.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.eventkampus.data.model.User
import org.json.JSONObject

class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserSession(user: User) {
        val json = JSONObject().apply {
            put("id", user.id)
            put("namaLengkap", user.namaLengkap)
            put("nim", user.nim)
            put("email", user.email)
            put("prodi", user.prodi)
            put("noWhatsapp", user.noWhatsapp)
            put("role", user.role)
        }
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_DATA, json.toString())
            .apply()
    }

    fun getCurrentUser(): User? {
        if (!isLoggedIn()) return null
        val jsonString = prefs.getString(KEY_USER_DATA, null) ?: return null
        return try {
            val json = JSONObject(jsonString)
            User(
                id = json.getString("id"),
                namaLengkap = json.getString("namaLengkap"),
                nim = json.getString("nim"),
                email = json.getString("email"),
                prodi = json.getString("prodi"),
                noWhatsapp = json.getString("noWhatsapp"),
                role = json.optString("role", "user")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_DATA)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "event_kampus_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_DATA = "current_user_data"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}