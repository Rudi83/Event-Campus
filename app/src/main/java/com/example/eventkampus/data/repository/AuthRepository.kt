package com.example.eventkampus.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import com.example.eventkampus.data.model.User
import com.example.eventkampus.data.session.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AuthRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val sessionManager = SessionManager.getInstance(appContext)
    private val userPrefs: SharedPreferences = appContext.getSharedPreferences(PREF_USERS, Context.MODE_PRIVATE)

    init {
        seedInitialUsersIfEmpty()
        refreshCurrentSession()
    }

    /**
     * SEED_VERSION: naikkan angka ini setiap kali data seed default diubah.
     * Ini memaksa data seed lama di perangkat tertimpa data terbaru.
     */
    private val SEED_VERSION = 3

    private fun seedInitialUsersIfEmpty() {
        val savedVersion = userPrefs.getInt(KEY_SEED_VERSION, 0)
        val currentJson = userPrefs.getString(KEY_USERS_LIST, null)

        // Re-seed jika: data kosong ATAU versi seed lama (data berubah)
        if (currentJson.isNullOrBlank() || savedVersion < SEED_VERSION) {
            val existingUsers = if (currentJson.isNullOrBlank()) emptyList()
            else getAllRegisteredUsers().filter { !it.id.startsWith("USR-") }
            // Pertahankan user yang daftar sendiri (non-seed), timpa akun seed

            val seedUsers = listOf(
                User(
                    id = "USR-001",
                    namaLengkap = "Nasyiruddin",
                    nim = "240170050",
                    email = "nasyir@mhs.unimal.ac.id",
                    prodi = "Teknik Informatika",
                    noWhatsapp = "082216850216",
                    password = "password123",
                    role = "admin"
                ),
                User(
                    id = "USR-002",
                    namaLengkap = "Sarah Nabila",
                    nim = "220180045",
                    email = "sarah@mhs.unimal.ac.id",
                    prodi = "Sistem Informasi",
                    noWhatsapp = "082345678901",
                    password = "password123",
                    role = "user"
                )
            )
            // Gabungkan: seed baru + user yang daftar sendiri (non-seed)
            val mergedUsers = seedUsers + existingUsers
            saveUsersList(mergedUsers)
            userPrefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).apply()

            // Jika user yang sedang login adalah akun seed lama, perbarui sesi
            val currentSession = sessionManager.getCurrentUser()
            if (currentSession != null) {
                val updated = mergedUsers.find { it.email.equals(currentSession.email, ignoreCase = true) }
                if (updated != null) {
                    sessionManager.saveUserSession(updated)
                }
            }
        }
    }

    /**
     * Perbarui sesi aktif dengan data terbaru dari DB.
     * Penting saat field baru (seperti 'role') ditambahkan ke model User —
     * sesi lama yang tersimpan tidak punya field tersebut.
     */
    private fun refreshCurrentSession() {
        val currentSession = sessionManager.getCurrentUser() ?: return
        val freshUser = getAllRegisteredUsers().find {
            it.email.equals(currentSession.email, ignoreCase = true)
        } ?: return
        // Simpan ulang sesi dengan data terlengkap dari DB
        sessionManager.saveUserSession(freshUser)
    }

    fun getAllRegisteredUsers(): List<User> {
        val jsonString = userPrefs.getString(KEY_USERS_LIST, null) ?: return emptyList()
        val list = mutableListOf<User>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    User(
                        id = obj.getString("id"),
                        namaLengkap = obj.getString("namaLengkap"),
                        nim = obj.getString("nim"),
                        email = obj.getString("email"),
                        prodi = obj.getString("prodi"),
                        noWhatsapp = obj.getString("noWhatsapp"),
                        password = obj.optString("password", ""),
                        role = obj.optString("role", "user")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Ambil nomor WhatsApp user berdasarkan NIM.
     * Digunakan admin untuk ekspor kontak peserta event.
     */
    fun getPhoneByNim(nim: String): String? =
        getAllRegisteredUsers().find { it.nim == nim }?.noWhatsapp?.takeIf { it.isNotBlank() }

    private fun saveUsersList(users: List<User>) {
        val array = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("namaLengkap", u.namaLengkap)
                put("nim", u.nim)
                put("email", u.email)
                put("prodi", u.prodi)
                put("noWhatsapp", u.noWhatsapp)
                put("password", u.password)
                put("role", u.role)
            }
            array.put(obj)
        }
        userPrefs.edit().putString(KEY_USERS_LIST, array.toString()).apply()
    }

    fun login(emailOrNim: String, password: String): Result<User> {
        val trimmedInput = emailOrNim.trim()
        val trimmedPassword = password.trim()

        if (trimmedInput.isEmpty()) {
            return Result.failure(IllegalArgumentException("Email atau NIM tidak boleh kosong"))
        }
        if (trimmedInput.length > 100) {
            return Result.failure(IllegalArgumentException("Input email/NIM terlalu panjang"))
        }
        if (trimmedPassword.isEmpty()) {
            return Result.failure(IllegalArgumentException("Password tidak boleh kosong"))
        }
        if (trimmedPassword.length > 100) {
            return Result.failure(IllegalArgumentException("Password terlalu panjang"))
        }

        val allUsers = getAllRegisteredUsers()
        val matchedUser = allUsers.find {
            it.email.equals(trimmedInput, ignoreCase = true) || it.nim.equals(trimmedInput, ignoreCase = true)
        }

        if (matchedUser == null) {
            return Result.failure(NoSuchElementException("Akun dengan Email atau NIM tersebut belum terdaftar"))
        }

        if (matchedUser.password != trimmedPassword) {
            return Result.failure(SecurityException("Password yang Anda masukkan salah"))
        }

        // Save session
        sessionManager.saveUserSession(matchedUser)
        return Result.success(matchedUser)
    }

    fun register(
        namaLengkap: String,
        nim: String,
        email: String,
        prodi: String,
        noWhatsapp: String,
        password: String,
        confirmPassword: String
    ): Result<User> {
        val trimmedNama = namaLengkap.trim()
        val trimmedNim = nim.trim()
        val trimmedEmail = email.trim()
        val trimmedProdi = prodi.trim()
        val trimmedPhone = noWhatsapp.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        // Validations
        if (trimmedNama.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nama lengkap wajib diisi"))
        }
        if (trimmedNim.length < 5) {
            return Result.failure(IllegalArgumentException("NIM minimal 5 karakter digit angka"))
        }
        if (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Format email tidak valid (contoh: nama@mhs.unimal.ac.id)"))
        }
        if (trimmedProdi.isEmpty()) {
            return Result.failure(IllegalArgumentException("Program Studi wajib dipilih/diisi"))
        }
        if (trimmedPhone.length < 10) {
            return Result.failure(IllegalArgumentException("Nomor WhatsApp minimal 10 digit"))
        }
        if (trimmedPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password minimal 6 karakter"))
        }
        if (trimmedPassword != trimmedConfirm) {
            return Result.failure(IllegalArgumentException("Konfirmasi password tidak cocok"))
        }

        val allUsers = getAllRegisteredUsers().toMutableList()

        if (allUsers.any { it.email.equals(trimmedEmail, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Email '$trimmedEmail' sudah terdaftar. Silakan login."))
        }
        if (allUsers.any { it.nim.equals(trimmedNim, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("NIM '$trimmedNim' sudah terdaftar pada akun lain."))
        }

        val newUser = User(
            id = "USR-${UUID.randomUUID().toString().take(8)}",
            namaLengkap = trimmedNama,
            nim = trimmedNim,
            email = trimmedEmail,
            prodi = trimmedProdi,
            noWhatsapp = trimmedPhone,
            password = trimmedPassword,
            role = "user"
        )

        allUsers.add(newUser)
        saveUsersList(allUsers)

        // Automatically log in newly registered user
        sessionManager.saveUserSession(newUser)
        return Result.success(newUser)
    }

    fun getCurrentUser(): User? = sessionManager.getCurrentUser()

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun logout() {
        sessionManager.clearSession()
    }

    companion object {
        private const val PREF_USERS = "event_kampus_registered_users"
        private const val KEY_USERS_LIST = "users_list_json"
        private const val KEY_SEED_VERSION = "seed_version"

        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}