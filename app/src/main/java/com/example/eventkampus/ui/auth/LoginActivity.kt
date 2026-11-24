package com.example.eventkampus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.MainActivity
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository

    // Mencegah double-submit saat proses login berjalan
    private var isLoginInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        authRepository = AuthRepository.getInstance(this)

        // Jika sudah login, langsung ke MainActivity
        if (authRepository.isLoggedIn()) {
            goToMainActivity()
            return
        }

        setupListeners()
        setupRealTimeValidation()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            if (!isLoginInProgress) {
                performLogin()
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Ketuk baris Admin → isi kredensial Nasyiruddin
        binding.layoutDemoAdmin.setOnClickListener {
            binding.etEmailOrNim.setText("nasyir@mhs.unimal.ac.id")
            binding.etPassword.setText("password123")
            hideErrorBanner()
            binding.tilEmailOrNim.error = null
            binding.tilPassword.error = null
            Toast.makeText(this, "Akun Admin diisikan!", Toast.LENGTH_SHORT).show()
        }

        // Ketuk baris User → isi kredensial Sarah
        binding.layoutDemoUser.setOnClickListener {
            binding.etEmailOrNim.setText("sarah@mhs.unimal.ac.id")
            binding.etPassword.setText("password123")
            hideErrorBanner()
            binding.tilEmailOrNim.error = null
            binding.tilPassword.error = null
            Toast.makeText(this, "Akun User diisikan!", Toast.LENGTH_SHORT).show()
        }
    }

    // Hapus error field saat pengguna mulai mengetik
    private fun setupRealTimeValidation() {
        val clearEmailErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmailOrNim.error = null
                hideErrorBanner()
            }
        }
        val clearPasswordErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
                hideErrorBanner()
            }
        }
        binding.etEmailOrNim.addTextChangedListener(clearEmailErrorWatcher)
        binding.etPassword.addTextChangedListener(clearPasswordErrorWatcher)
    }

    private fun performLogin() {
        hideErrorBanner()
        binding.tilEmailOrNim.error = null
        binding.tilPassword.error = null

        val emailOrNim = binding.etEmailOrNim.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        // Validasi sisi UI sebelum meneruskan ke repository
        if (emailOrNim.isBlank()) {
            binding.tilEmailOrNim.error = "Email atau NIM tidak boleh kosong"
            binding.etEmailOrNim.requestFocus()
            return
        }

        // Jika berisi '@', validasi sebagai format email
        if (emailOrNim.contains('@') && !Patterns.EMAIL_ADDRESS.matcher(emailOrNim).matches()) {
            binding.tilEmailOrNim.error = "Format email tidak valid"
            binding.etEmailOrNim.requestFocus()
            return
        }

        if (password.isBlank()) {
            binding.tilPassword.error = "Password tidak boleh kosong"
            binding.etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password minimal 6 karakter"
            binding.etPassword.requestFocus()
            return
        }

        // Set loading state — cegah double submit
        setLoginLoading(true)

        // Proses login (sinkron dari SharedPreferences/SQLite lokal, tidak butuh coroutine)
        val result = authRepository.login(emailOrNim, password)

        setLoginLoading(false)

        result.onSuccess { user ->
            Toast.makeText(this, "Selamat datang kembali, ${user.namaLengkap}!", Toast.LENGTH_SHORT).show()
            goToMainActivity()
        }.onFailure { exception ->
            val errorMessage = exception.message ?: "Terjadi kesalahan saat masuk. Silakan coba lagi."
            showErrorBanner(errorMessage)

            when (exception) {
                is SecurityException -> binding.tilPassword.error = "Password salah"
                is NoSuchElementException -> binding.tilEmailOrNim.error = "Akun tidak ditemukan"
            }
        }
    }

    private fun setLoginLoading(isLoading: Boolean) {
        isLoginInProgress = isLoading
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "Memproses…" else "Masuk Sekarang"
        binding.tilEmailOrNim.isEnabled = !isLoading
        binding.tilPassword.isEnabled = !isLoading
        binding.layoutDemoAdmin.isClickable = !isLoading
        binding.layoutDemoUser.isClickable = !isLoading
    }

    private fun showErrorBanner(message: String) {
        binding.cardErrorBanner.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun hideErrorBanner() {
        binding.cardErrorBanner.visibility = View.GONE
    }

    private fun goToMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
