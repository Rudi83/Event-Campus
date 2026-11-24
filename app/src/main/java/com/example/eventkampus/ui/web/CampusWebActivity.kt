package com.example.eventkampus.ui.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.databinding.ActivityCampusWebBinding

class CampusWebActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCampusWebBinding

    // Handler timeout — tampilkan error jika halaman tidak selesai dalam 15 detik
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (binding.pbWebLoading.visibility == View.VISIBLE) {
            binding.pbWebLoading.visibility = View.GONE
            binding.layoutWebError.visibility = View.VISIBLE
        }
    }
    private val LOAD_TIMEOUT_MS = 15_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCampusWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // Validasi dan sanitasi URL masuk dari Intent
        val rawUrl = intent.getStringExtra(EXTRA_URL)
        val safeUrl = sanitizeUrl(rawUrl)
        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: "Portal Kampus"

        if (safeUrl == null) {
            showErrorState()
            setupToolbar(title)
            setupBackNavigation()
            return
        }

        // Periksa koneksi internet sebelum memuat
        if (!isNetworkAvailable()) {
            showErrorState()
            setupToolbar(title)
            setupWebView(safeUrl) // tetap siapkan WebView agar tombol reload berfungsi
            setupBackNavigation()
            return
        }

        setupToolbar(title)
        setupWebView(safeUrl)
        setupBackNavigation()
    }

    /**
     * Hanya izinkan URL dengan scheme http:// atau https://.
     * Tolak file://, javascript:, data:, dan scheme lain yang berbahaya.
     */
    private fun sanitizeUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val lowerCase = trimmed.lowercase()
        return when {
            lowerCase.startsWith("https://") || lowerCase.startsWith("http://") -> trimmed
            else -> null // tolak file://, javascript:, data:, dsb.
        }
    }

    /**
     * Periksa apakah perangkat terhubung ke internet.
     * Dibungkus try-catch karena beberapa perangkat dapat membatasi akses
     * ConnectivityManager meskipun permission ACCESS_NETWORK_STATE sudah dideklarasikan.
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            // Jika tidak bisa memeriksa, asumsikan ada koneksi dan biarkan WebView mencoba
            true
        }
    }

    private fun showErrorState() {
        binding.pbWebLoading.visibility = View.GONE
        binding.layoutWebError.visibility = View.VISIBLE
        binding.webViewCampus.visibility = View.GONE
    }

    private fun setupToolbar(title: String) {
        binding.toolbarWeb.title = title
        binding.toolbarWeb.setNavigationOnClickListener {
            if (binding.webViewCampus.canGoBack()) {
                binding.webViewCampus.goBack()
            } else {
                finish()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        val webView = binding.webViewCampus
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            // Nonaktifkan akses file lokal untuk mencegah file:// traversal
            allowFileAccess = false
            allowContentAccess = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    binding.pbWebLoading.visibility = View.VISIBLE
                    binding.pbWebLoading.isIndeterminate = false
                    binding.pbWebLoading.progress = newProgress
                } else {
                    binding.pbWebLoading.visibility = View.GONE
                    // Halaman selesai dimuat — batalkan timeout
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.pbWebLoading.visibility = View.VISIBLE
                binding.layoutWebError.visibility = View.GONE
                binding.webViewCampus.visibility = View.VISIBLE
                // Mulai timer timeout
                timeoutHandler.removeCallbacks(timeoutRunnable)
                timeoutHandler.postDelayed(timeoutRunnable, LOAD_TIMEOUT_MS)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.pbWebLoading.visibility = View.GONE
                timeoutHandler.removeCallbacks(timeoutRunnable)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Hanya tampilkan error state jika main frame yang gagal
                if (request?.isForMainFrame == true) {
                    binding.pbWebLoading.visibility = View.GONE
                    binding.layoutWebError.visibility = View.VISIBLE
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                }
            }

            // Blokir navigasi ke URL non-HTTPS secara diam-diam
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val destination = request?.url?.toString() ?: return false
                return if (sanitizeUrl(destination) == null) {
                    // URL tidak aman — blokir navigasi
                    true
                } else {
                    false // izinkan browser normal
                }
            }
        }

        binding.btnReloadWeb.setOnClickListener {
            if (!isNetworkAvailable()) {
                binding.layoutWebError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            binding.layoutWebError.visibility = View.GONE
            webView.reload()
        }

        webView.loadUrl(url)
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webViewCampus.canGoBack()) {
                    binding.webViewCampus.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        binding.webViewCampus.stopLoading()
        binding.webViewCampus.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }
}
