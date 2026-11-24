package com.example.eventkampus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.MainActivity
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    private var isRegisterInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        authRepository = AuthRepository.getInstance(this)

        setupToolbar()
        setupProdiDropdown()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbarRegisterAccount.setNavigationOnClickListener { finish() }
    }

    private fun setupProdiDropdown() {
        val prodiList = listOf(
            "Teknik Informatika",
            "Sistem Informasi",
            "Teknik Sipil",
            "Teknik Mesin",
            "Teknik Elektro",
            "Teknik Kimia",
            "Arsitektur",
            "Ilmu Komunikasi",
            "Manajemen",
            "Akuntansi",
            "Ekonomi Pembangunan",
            "Ilmu Hukum",
            "Kedokteran",
            "Administrasi Publik"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, prodiList)
        binding.actvRegProdi.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnSubmitRegisterAccount.setOnClickListener {
            if (!isRegisterInProgress) performRegister()
        }
        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    private fun performRegister() {
        hideError()
        clearFieldErrors()

        val nama = binding.etRegNama.text?.toString()?.trim().orEmpty()
        val nim = binding.etRegNim.text?.toString()?.trim().orEmpty()
        val email = binding.etRegEmail.text?.toString()?.trim().orEmpty()
        val prodi = binding.actvRegProdi.text?.toString()?.trim().orEmpty()
        val phone = binding.etRegPhone.text?.toString()?.trim().orEmpty()
        val password = binding.etRegPassword.text?.toString() ?: ""
        val confirmPassword = binding.etRegConfirmPassword.text?.toString() ?: ""

        var hasError = false
        var firstErrorView: View? = null

        // Validasi Nama
        if (nama.isEmpty()) {
            binding.tilRegNama.error = "Nama lengkap wajib diisi"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegNama
        } else if (nama.length < 3) {
            binding.tilRegNama.error = "Nama terlalu pendek (minimal 3 karakter)"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegNama
        } else if (!nama.matches(Regex("^[a-zA-Z .'-]+$"))) {
            binding.tilRegNama.error = "Nama hanya boleh berisi huruf, spasi, dan tanda titik"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegNama
        }

        // Validasi NIM
        if (nim.isEmpty()) {
            binding.tilRegNim.error = "NIM wajib diisi"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegNim
        } else if (!nim.matches(Regex("^[0-9]+$"))) {
            binding.tilRegNim.error = "NIM hanya boleh berisi angka"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegNim
        } else if (nim.length < 5 || nim.length > 15) {
            binding.tilRegNim.error = "NIM harus antara 5–15 digit angka"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegNim
        }

        // Validasi Email
        if (email.isEmpty()) {
            binding.tilRegEmail.error = "Email wajib diisi"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegEmail
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilRegEmail.error = "Format email tidak valid (contoh: nama@mhs.unimal.ac.id)"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegEmail
        }

        // Validasi Program Studi
        if (prodi.isEmpty()) {
            binding.tilRegProdi.error = "Pilih program studi"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.actvRegProdi
        }

        // Validasi Nomor WhatsApp
        val phoneDigits = phone.replace(Regex("[\\s\\-()]"), "")
        if (phone.isEmpty()) {
            binding.tilRegPhone.error = "Nomor WhatsApp wajib diisi"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegPhone
        } else if (!phoneDigits.matches(Regex("^(\\+62|62|0)[0-9]{8,13}$"))) {
            binding.tilRegPhone.error = "Format nomor tidak valid (contoh: 0812-3456-7890 atau +6281234567890)"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegPhone
        }

        // Validasi Password
        if (password.isEmpty()) {
            binding.tilRegPassword.error = "Password wajib diisi"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegPassword
        } else if (password.length < 6) {
            binding.tilRegPassword.error = "Password minimal 6 karakter"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegPassword
        } else if (password.length > 50) {
            binding.tilRegPassword.error = "Password terlalu panjang (maksimal 50 karakter)"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegPassword
        }

        // Validasi Konfirmasi Password
        if (confirmPassword.isEmpty()) {
            binding.tilRegConfirmPassword.error = "Konfirmasi password wajib diisi"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegConfirmPassword
        } else if (password != confirmPassword) {
            binding.tilRegConfirmPassword.error = "Konfirmasi password tidak cocok"
            hasError = true
            if (firstErrorView == null) firstErrorView = binding.etRegConfirmPassword
        }

        if (hasError) {
            showError("Silakan perbaiki kesalahan pada kolom di atas.")
            firstErrorView?.requestFocus()
            return
        }

        setRegisterLoading(true)

        val result = authRepository.register(
            namaLengkap = nama,
            nim = nim,
            email = email,
            prodi = prodi,
            noWhatsapp = phoneDigits,
            password = password,
            confirmPassword = confirmPassword
        )

        setRegisterLoading(false)

        result.onSuccess { user ->
            Toast.makeText(
                this,
                "Pendaftaran berhasil! Selamat datang, ${user.namaLengkap}",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }.onFailure { exception ->
            val errorMsg = exception.message ?: "Pendaftaran gagal. Silakan coba kembali."
            showError(errorMsg)
            when {
                errorMsg.contains("Email", ignoreCase = true) -> binding.tilRegEmail.error = errorMsg
                errorMsg.contains("NIM", ignoreCase = true) -> binding.tilRegNim.error = errorMsg
            }
        }
    }

    private fun setRegisterLoading(isLoading: Boolean) {
        isRegisterInProgress = isLoading
        binding.btnSubmitRegisterAccount.isEnabled = !isLoading
        binding.btnSubmitRegisterAccount.text = if (isLoading) "Mendaftarkan…" else "Daftar Akun Sekarang"
        binding.tilRegNama.isEnabled = !isLoading
        binding.tilRegNim.isEnabled = !isLoading
        binding.tilRegEmail.isEnabled = !isLoading
        binding.tilRegPhone.isEnabled = !isLoading
        binding.tilRegPassword.isEnabled = !isLoading
        binding.tilRegConfirmPassword.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        binding.cardRegisterError.visibility = View.VISIBLE
        binding.tvRegisterErrorMsg.text = message
    }

    private fun hideError() {
        binding.cardRegisterError.visibility = View.GONE
    }

    private fun clearFieldErrors() {
        binding.tilRegNama.error = null
        binding.tilRegNim.error = null
        binding.tilRegEmail.error = null
        binding.tilRegProdi.error = null
        binding.tilRegPhone.error = null
        binding.tilRegPassword.error = null
        binding.tilRegConfirmPassword.error = null
    }
}
