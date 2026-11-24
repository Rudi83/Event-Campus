package com.example.eventkampus.ui.certificate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.ActivityMyCertificatesBinding
import com.example.eventkampus.util.SkpHelper
import com.example.eventkampus.util.SkpHelper.isEligibleForCertificate

class MyCertificatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyCertificatesBinding
    private lateinit var adapter: CertificateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMyCertificatesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        binding.toolbarCert.setNavigationOnClickListener { finish() }
        setupRecyclerView()
        loadCertificates()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        adapter = CertificateAdapter { ticket, link ->
            openCertificateLink(link, ticket.eventTitle)
        }
        binding.rvCertificates.layoutManager = LinearLayoutManager(this)
        binding.rvCertificates.adapter = adapter
    }

    private fun loadCertificates() {
        val nim        = AuthRepository.getInstance(this).getCurrentUser()?.nim.orEmpty()
        val allTickets = EventRepository.getInstance(this).getMyTickets(nim)
        val certs      = allTickets.filter { it.isEligibleForCertificate }

        val totalSkp  = SkpHelper.totalSkp(allTickets)
        val skpTarget = 100

        binding.tvTotalSkpBanner.text = totalSkp.toString()
        binding.tvCertCount.text      = certs.size.toString()
        binding.tvSkpProgress.text    = "$totalSkp / $skpTarget SKP"
        binding.progressSkp.max       = skpTarget
        binding.progressSkp.progress  = totalSkp.coerceAtMost(skpTarget)

        if (certs.isEmpty()) {
            binding.layoutCertEmpty.visibility = View.VISIBLE
            binding.rvCertificates.visibility  = View.GONE
        } else {
            binding.layoutCertEmpty.visibility = View.GONE
            binding.rvCertificates.visibility  = View.VISIBLE
            adapter.submitList(certs)
        }
    }

    private fun openCertificateLink(link: String, eventTitle: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        } catch (e: Exception) {
            Toast.makeText(this,
                "Tidak dapat membuka link. Pastikan browser terpasang.",
                Toast.LENGTH_LONG).show()
        }
    }
}
