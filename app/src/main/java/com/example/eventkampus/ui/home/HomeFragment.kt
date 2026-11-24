package com.example.eventkampus.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventkampus.MainActivity
import com.example.eventkampus.R
import com.example.eventkampus.data.model.Category
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.FragmentHomeBinding
import com.example.eventkampus.ui.adapter.CategoryAdapter
import com.example.eventkampus.ui.adapter.EventAdapter
import com.example.eventkampus.ui.detail.DetailEventActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: EventRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var eventAdapter: EventAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private var selectedCategory: String = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = EventRepository.getInstance(requireContext())
        authRepository = AuthRepository.getInstance(requireContext())

        setupUserGreeting()
        setupCategories()
        setupEventsRecyclerView()
        setupActions()
        loadEvents()
    }

    override fun onResume() {
        super.onResume()
        setupUserGreeting()
        loadEvents()
    }

    private fun setupUserGreeting() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            binding.tvGreeting.text = "Halo, ${currentUser.namaLengkap} 👋"
            val roleLabel = if (currentUser.isAdmin) "Admin Kampus" else "Mahasiswa"
            binding.tvSubGreeting.text = "$roleLabel • ${currentUser.prodi} • UNIMAL"
        } else {
            binding.tvGreeting.text = "Halo, Mahasiswa 👋"
            binding.tvSubGreeting.text = "Temukan kegiatan kampus terbaik untukmu"
        }
    }

    private fun setupCategories() {
        categoryAdapter = CategoryAdapter(repository.getCategories()) { category ->
            selectedCategory = category.name
            loadEvents()
        }
        binding.rvCategories.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupEventsRecyclerView() {
        eventAdapter = EventAdapter { event ->
            val intent = Intent(requireContext(), DetailEventActivity::class.java).apply {
                putExtra(DetailEventActivity.EXTRA_EVENT, event)
            }
            startActivity(intent)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventAdapter
    }

    private fun setupActions() {
        binding.btnExploreHero.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.menu_explore)
        }

        // Tombol tambah kegiatan — hanya untuk admin
        val currentUser = authRepository.getCurrentUser()
        val isAdmin = currentUser?.isAdmin == true
        binding.btnAddNewEvent.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnAddFirstEvent.visibility = if (isAdmin) View.VISIBLE else View.GONE

        binding.btnAddNewEvent.setOnClickListener {
            val intent = Intent(requireContext(), com.example.eventkampus.ui.manage.AddEditEventActivity::class.java)
            startActivity(intent)
        }

        binding.btnAddFirstEvent.setOnClickListener {
            val intent = Intent(requireContext(), com.example.eventkampus.ui.manage.AddEditEventActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadEvents() {
        // Tampilkan loading state
        binding.layoutLoadingHome.visibility = View.VISIBLE
        binding.rvEvents.visibility = View.GONE
        binding.layoutEmptyHome.visibility = View.GONE

        val events = repository.filterEvents(selectedCategory, null)
        eventAdapter.submitList(events)

        binding.layoutLoadingHome.visibility = View.GONE
        binding.tvEventCount.text = "${events.size} Acara"

        if (events.isEmpty()) {
            binding.layoutEmptyHome.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.layoutEmptyHome.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}