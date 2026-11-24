package com.example.eventkampus.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.FragmentAdminParticipantsBinding

class AdminParticipantsFragment : Fragment() {

    private var _binding: FragmentAdminParticipantsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: EventRepository
    private lateinit var adapter: GroupedParticipantsAdapter

    private var allTickets: List<Ticket>  = emptyList()
    private var allEvents:  List<Event>   = emptyList()
    private var currentFilter: String     = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminParticipantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = EventRepository.getInstance(requireContext())
        setupRecyclerView()
        setupFilterChips()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = GroupedParticipantsAdapter(
            onApprove = { ticket -> showApproveDialog(ticket) },
            onReject  = { ticket -> showRejectDialog(ticket) }
        )
        binding.rvAllParticipants.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllParticipants.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                binding.chipPending.id  -> "Terdaftar"
                binding.chipApproved.id -> "Disetujui"
                binding.chipRejected.id -> "Ditolak"
                else                   -> "Semua"
            }
            applyFilter()
        }
    }

    private fun loadData() {
        allTickets = repository.getTickets()
        allEvents  = repository.getAllEvents()
        updateSummary()
        applyFilter()
    }

    private fun applyFilter() {
        // Filter tiket sesuai chip
        val filtered = if (currentFilter == "Semua") {
            allTickets
        } else {
            allTickets.filter { it.status == currentFilter }
        }

        // Kelompokkan per eventId, pertahankan hanya event yang punya peserta
        val grouped: Map<String, List<Ticket>> = filtered
            .groupBy { it.eventId }

        val isEmpty = grouped.isEmpty()
        binding.rvAllParticipants.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility        = if (isEmpty) View.VISIBLE else View.GONE

        if (isEmpty) {
            binding.tvEmptyMessage.text = when (currentFilter) {
                "Semua"     -> "Belum ada peserta yang mendaftar"
                "Terdaftar" -> "Tidak ada pendaftaran yang menunggu review"
                "Disetujui" -> "Belum ada peserta yang disetujui"
                "Ditolak"   -> "Belum ada peserta yang ditolak"
                else        -> "Tidak ada data"
            }
        } else {
            adapter.submitGrouped(grouped, allEvents)
        }
    }

    private fun updateSummary() {
        binding.tvSummaryTotal.text    = allTickets.size.toString()
        binding.tvSummaryPending.text  = allTickets.count { it.status == "Terdaftar" }.toString()
        binding.tvSummaryApproved.text = allTickets.count { it.status == "Disetujui" }.toString()
        binding.tvSummaryRejected.text = allTickets.count { it.status == "Ditolak" }.toString()
        binding.tvAdminSummary.text    =
            "${allTickets.size} pendaftaran • ${allTickets.count { it.status == "Terdaftar" }} menunggu review"
    }

    private fun showApproveDialog(ticket: Ticket) {
        AlertDialog.Builder(requireContext())
            .setTitle("Setujui Pendaftaran?")
            .setMessage(
                "Nama     : ${ticket.participantName}\n" +
                "NIM      : ${ticket.participantNim}\n" +
                "Kegiatan : ${ticket.eventTitle}"
            )
            .setPositiveButton("Setujui") { _, _ ->
                val ok = repository.updateTicketStatus(ticket.ticketId, "Disetujui")
                if (ok) {
                    Toast.makeText(requireContext(),
                        "${ticket.participantName} disetujui.", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(requireContext(), "Gagal.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showRejectDialog(ticket: Ticket) {
        AlertDialog.Builder(requireContext())
            .setTitle("Tolak Pendaftaran?")
            .setMessage(
                "Nama     : ${ticket.participantName}\n" +
                "NIM      : ${ticket.participantNim}\n" +
                "Kegiatan : ${ticket.eventTitle}"
            )
            .setPositiveButton("Tolak") { _, _ ->
                val ok = repository.updateTicketStatus(ticket.ticketId, "Ditolak")
                if (ok) {
                    Toast.makeText(requireContext(),
                        "${ticket.participantName} ditolak.", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(requireContext(), "Gagal.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
