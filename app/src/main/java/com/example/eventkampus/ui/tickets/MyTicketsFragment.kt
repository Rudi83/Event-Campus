package com.example.eventkampus.ui.tickets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventkampus.MainActivity
import com.example.eventkampus.R
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.FragmentMyTicketsBinding
import com.example.eventkampus.ui.adapter.TicketAdapter
import com.example.eventkampus.ui.ticket.TicketDetailActivity

class MyTicketsFragment : Fragment() {

    private var _binding: FragmentMyTicketsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: EventRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var ticketAdapter: TicketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = EventRepository.getInstance(requireContext())
        authRepository = AuthRepository.getInstance(requireContext())

        setupRecyclerView()
        setupActions()
        loadTickets()
    }

    override fun onResume() {
        super.onResume()
        loadTickets()
    }

    private fun setupRecyclerView() {
        ticketAdapter = TicketAdapter { ticket ->
            val intent = Intent(requireContext(), TicketDetailActivity::class.java).apply {
                putExtra(TicketDetailActivity.EXTRA_TICKET, ticket)
            }
            startActivity(intent)
        }
        binding.rvTickets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTickets.adapter = ticketAdapter
    }

    private fun setupActions() {
        binding.btnExploreNow.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.menu_home)
        }
    }

    private fun loadTickets() {
        // Hanya tampilkan tiket milik user yang sedang login
        val currentNim = authRepository.getCurrentUser()?.nim.orEmpty()
        val tickets = if (currentNim.isNotEmpty()) {
            repository.getMyTickets(currentNim)
        } else {
            emptyList()
        }

        ticketAdapter.submitList(tickets)

        if (tickets.isEmpty()) {
            binding.layoutEmptyTickets.visibility = View.VISIBLE
            binding.rvTickets.visibility = View.GONE
            binding.tvTicketSummary.text = "Belum ada tiket terdaftar"
        } else {
            binding.layoutEmptyTickets.visibility = View.GONE
            binding.rvTickets.visibility = View.VISIBLE
            binding.tvTicketSummary.text = "${tickets.size} Tiket Terdaftar • Siap Digunakan Saat Hari-H"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}