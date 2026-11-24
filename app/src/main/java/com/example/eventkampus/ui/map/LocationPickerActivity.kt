package com.example.eventkampus.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventkampus.databinding.ActivityLocationPickerBinding

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationPickerBinding

    private var pickedLat     = 0.0
    private var pickedLng     = 0.0
    private var pickedAddress = ""
    private var mapReady      = false

    // Permission launcher lokasi
    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) moveToMyLocation()
        else Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, 0)
            insets
        }

        binding.toolbarPicker.setNavigationOnClickListener { finish() }

        setupWebView()
        setupSearch()
        setupButtons()
    }

    // ─── WebView ──────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = binding.webViewMap
        wv.settings.apply {
            javaScriptEnabled   = true
            domStorageEnabled   = true
            loadWithOverviewMode = true
            useWideViewPort     = true
            allowFileAccess     = false
        }
        wv.webChromeClient = WebChromeClient()
        wv.webViewClient   = WebViewClient()

        // Bridge JS → Android
        wv.addJavascriptInterface(JsBridge(), "AndroidBridge")
        wv.loadUrl("file:///android_asset/map_picker.html")
    }

    // ─── JS Bridge ───────────────────────────────────────────────────────────

    inner class JsBridge {

        @JavascriptInterface
        fun onMapReady() {
            mapReady = true
            // Jika ada koordinat awal dari Intent, arahkan peta ke sana
            val initLat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
            val initLng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
            if (initLat != 0.0 && initLng != 0.0) {
                runOnUiThread {
                    binding.webViewMap.evaluateJavascript(
                        "setLocation($initLat, $initLng);", null
                    )
                }
            }
        }

        @JavascriptInterface
        fun onLocationPicked(lat: Double, lng: Double, address: String) {
            pickedLat     = lat
            pickedLng     = lng
            pickedAddress = address
            runOnUiThread {
                binding.layoutPickedInfo.visibility = View.VISIBLE
                binding.tvPickedAddress.text        = address
            }
        }

        @JavascriptInterface
        fun onSearchNotFound() {
            runOnUiThread {
                Toast.makeText(this@LocationPickerActivity,
                    "Lokasi tidak ditemukan. Coba kata kunci lain.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener { performSearch() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { performSearch(); true }
            else false
        }
    }

    private fun performSearch() {
        val q = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (q.isEmpty()) {
            Toast.makeText(this, "Masukkan nama lokasi untuk dicari.", Toast.LENGTH_SHORT).show()
            return
        }
        // Sembunyikan keyboard
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

        if (!mapReady) {
            Toast.makeText(this, "Peta belum siap, tunggu sebentar.", Toast.LENGTH_SHORT).show()
            return
        }
        val escaped = q.replace("'", "\\'")
        binding.webViewMap.evaluateJavascript("searchLocation('$escaped');", null)
    }

    // ─── Buttons ─────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnMyLocation.setOnClickListener {
            checkLocationPermissionAndMove()
        }
        binding.btnConfirmLocation.setOnClickListener {
            if (pickedAddress.isEmpty()) {
                // Picu reverse geocode dari titik tengah peta saat ini
                binding.webViewMap.evaluateJavascript("confirmCurrentLocation();", null)
                // Tunggu callback onLocationPicked sebelum konfirmasi
                Toast.makeText(this, "Memuat nama lokasi…", Toast.LENGTH_SHORT).show()
            } else {
                returnResult()
            }
        }
    }

    private fun checkLocationPermissionAndMove() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            moveToMyLocation()
        } else {
            locationPermLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        val providers = listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER
        )
        var loc: android.location.Location? = null
        for (p in providers) {
            if (lm.isProviderEnabled(p)) {
                loc = lm.getLastKnownLocation(p)
                if (loc != null) break
            }
        }
        if (loc != null) {
            binding.webViewMap.evaluateJavascript(
                "setLocation(${loc.latitude}, ${loc.longitude});", null
            )
        } else {
            Toast.makeText(this, "Tidak dapat mendeteksi lokasi Anda saat ini.", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Kembalikan hasil ke AddEditEventActivity ─────────────────────────────

    private fun returnResult() {
        val intent = Intent().apply {
            putExtra(RESULT_ADDRESS, pickedAddress)
            putExtra(RESULT_LAT,     pickedLat)
            putExtra(RESULT_LNG,     pickedLng)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        if (binding.webViewMap.canGoBack()) binding.webViewMap.goBack()
        else super.onBackPressed()
    }

    companion object {
        const val EXTRA_LAT      = "extra_lat"
        const val EXTRA_LNG      = "extra_lng"
        const val RESULT_ADDRESS = "result_address"
        const val RESULT_LAT     = "result_lat"
        const val RESULT_LNG     = "result_lng"
    }
}
