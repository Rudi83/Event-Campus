package com.example.eventkampus

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.databinding.ActivityMainBinding
import com.example.eventkampus.ui.admin.AdminParticipantsFragment
import com.example.eventkampus.ui.auth.LoginActivity
import com.example.eventkampus.ui.explore.ExploreFragment
import com.example.eventkampus.ui.home.HomeFragment
import com.example.eventkampus.ui.profile.ProfileFragment
import com.example.eventkampus.ui.tickets.MyTicketsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authRepository: AuthRepository

    private val homeFragment by lazy { HomeFragment() }
    private val exploreFragment by lazy { ExploreFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    // Tab ketiga berbeda tergantung role — dibuat lazy setelah authRepository siap
    private val ticketsFragment: Fragment by lazy {
        if (authRepository.getCurrentUser()?.isAdmin == true)
            AdminParticipantsFragment()
        else
            MyTicketsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authRepository = AuthRepository.getInstance(this)
        if (!authRepository.isLoggedIn()) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Ubah label DAN ikon menu_tickets sesuai role
        val isAdmin = authRepository.getCurrentUser()?.isAdmin == true
        binding.bottomNav.menu.findItem(R.id.menu_tickets)?.apply {
            title = if (isAdmin) "Kelola" else "Tiket Saya"
            setIcon(if (isAdmin) R.drawable.ic_person else R.drawable.ic_ticket)
        }

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(homeFragment)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home    -> { loadFragment(homeFragment);    true }
                R.id.menu_explore -> { loadFragment(exploreFragment); true }
                R.id.menu_tickets -> { loadFragment(ticketsFragment); true }
                R.id.menu_profile -> { loadFragment(profileFragment); true }
                else -> false
            }
        }
    }

    fun switchTab(menuId: Int) {
        binding.bottomNav.selectedItemId = menuId
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
