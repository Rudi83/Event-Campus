package com.example.eventkampus.ui.explore

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventkampus.R
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.data.repository.AuthRepository
import com.example.eventkampus.data.repository.EventRepository
import com.example.eventkampus.databinding.FragmentExploreBinding
import com.example.eventkampus.ui.adapter.EventAdapter
import com.example.eventkampus.ui.detail.DetailEventActivity
import com.example.eventkampus.ui.manage.AddEditEventActivity

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: EventRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var eventAdapter: EventAdapter

    // Filters
    private var currentCategoryFilter: String = "ALL" // ALL, Seminar, Workshop, Lomba, Kepanitiaan, FREE, ONLINE, OFFLINE
    private var currentSortOption: String = "NEWEST" // NEWEST, PRICE_LOW, PRICE_HIGH, QUOTA, NAME
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = EventRepository.getInstance(requireContext())
        authRepository = AuthRepository.getInstance(requireContext())

        setupRecyclerView()
        setupSearch()
        setupCategoryChips()
        setupSortChips()
        setupFab()
        setupEmptyStateActions()
        filterAndLoadEvents()
    }

    override fun onResume() {
        super.onResume()
        filterAndLoadEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            val intent = Intent(requireContext(), DetailEventActivity::class.java).apply {
                putExtra(DetailEventActivity.EXTRA_EVENT, event)
                putExtra(DetailEventActivity.EXTRA_EVENT_ID, event.id)
            }
            startActivity(intent)
        }
        binding.rvExploreEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExploreEvents.adapter = eventAdapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim().orEmpty()
                binding.ivClearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                filterAndLoadEvents()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            currentCategoryFilter = when {
                checkedIds.contains(R.id.chipCatSeminar) -> "Seminar"
                checkedIds.contains(R.id.chipCatWorkshop) -> "Workshop"
                checkedIds.contains(R.id.chipCatLomba) -> "Lomba"
                checkedIds.contains(R.id.chipCatKepanitiaan) -> "Kepanitiaan"
                checkedIds.contains(R.id.chipCatFree) -> "FREE"
                checkedIds.contains(R.id.chipCatOnline) -> "ONLINE"
                checkedIds.contains(R.id.chipCatOffline) -> "OFFLINE"
                else -> "ALL"
            }
            filterAndLoadEvents()
        }
    }

    private fun setupSortChips() {
        binding.chipGroupSort.setOnCheckedStateChangeListener { _, checkedIds ->
            currentSortOption = when {
                checkedIds.contains(R.id.chipSortPriceLow) -> "PRICE_LOW"
                checkedIds.contains(R.id.chipSortPriceHigh) -> "PRICE_HIGH"
                checkedIds.contains(R.id.chipSortQuota) -> "QUOTA"
                checkedIds.contains(R.id.chipSortName) -> "NAME"
                else -> "NEWEST"
            }
            filterAndLoadEvents()
        }
    }

    private fun setupFab() {
        // FAB tambah kegiatan — hanya untuk admin
        val currentUser = authRepository.getCurrentUser()
        binding.fabAddEvent.visibility = if (currentUser?.isAdmin == true) View.VISIBLE else View.GONE

        binding.fabAddEvent.setOnClickListener {
            val intent = Intent(requireContext(), AddEditEventActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupEmptyStateActions() {
        binding.btnResetFilter.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.chipCatAll.isChecked = true
            binding.chipSortNewest.isChecked = true
            currentCategoryFilter = "ALL"
            currentSortOption = "NEWEST"
            currentQuery = ""
            filterAndLoadEvents()
        }
    }

    private fun filterAndLoadEvents() {
        // Tampilkan loading state sekilas
        binding.layoutLoadingExplore.visibility = View.VISIBLE
        binding.layoutEmptyExplore.visibility = View.GONE
        binding.rvExploreEvents.visibility = View.GONE

        val allEvents = repository.getAllEvents()

        // 1. Text Search Filter (Query matching title, speaker, organizer, location, description)
        val searchFiltered = if (currentQuery.isBlank()) {
            allEvents
        } else {
            allEvents.filter { event ->
                event.title.contains(currentQuery, ignoreCase = true) ||
                        event.speaker.contains(currentQuery, ignoreCase = true) ||
                        event.organizer.contains(currentQuery, ignoreCase = true) ||
                        event.location.contains(currentQuery, ignoreCase = true) ||
                        event.category.contains(currentQuery, ignoreCase = true) ||
                        event.description.contains(currentQuery, ignoreCase = true)
            }
        }

        // 2. Category / Type Filter
        val categoryFiltered = searchFiltered.filter { event ->
            when (currentCategoryFilter) {
                "Seminar" -> event.category.equals("Seminar", ignoreCase = true)
                "Workshop" -> event.category.equals("Workshop", ignoreCase = true)
                "Lomba" -> event.category.equals("Lomba", ignoreCase = true)
                "Kepanitiaan" -> event.category.equals("Kepanitiaan", ignoreCase = true)
                "FREE" -> event.isFree
                "ONLINE" -> event.isOnline
                "OFFLINE" -> !event.isOnline
                else -> true
            }
        }

        // 3. Sorting (Urutkan)
        val sortedList = when (currentSortOption) {
            "PRICE_LOW" -> categoryFiltered.sortedBy { it.price }
            "PRICE_HIGH" -> categoryFiltered.sortedByDescending { it.price }
            "QUOTA" -> categoryFiltered.sortedByDescending { it.remainingQuota }
            "NAME" -> categoryFiltered.sortedBy { it.title.lowercase() }
            else -> categoryFiltered // "NEWEST" (urutan asli SQLite DESC)
        }

        eventAdapter.submitList(sortedList)

        // Sembunyikan loading
        binding.layoutLoadingExplore.visibility = View.GONE

        // Result count & indicators
        binding.tvResultCount.text = "Menampilkan ${sortedList.size} dari ${allEvents.size} kegiatan"

        val hasActiveFilter = currentQuery.isNotBlank() || currentCategoryFilter != "ALL" || currentSortOption != "NEWEST"
        binding.tvActiveFiltersIndicator.visibility = if (hasActiveFilter) View.VISIBLE else View.GONE
        binding.tvActiveFiltersIndicator.text = if (hasActiveFilter) getString(R.string.explore_filter_active) else ""

        // Empty state handling
        if (sortedList.isEmpty()) {
            binding.layoutEmptyExplore.visibility = View.VISIBLE
            binding.rvExploreEvents.visibility = View.GONE

            if (currentQuery.isNotBlank()) {
                binding.tvEmptySubtitle.text = "Tidak ada kegiatan yang cocok dengan kata kunci \"$currentQuery\". Silakan coba kata kunci lain atau reset filter."
            } else {
                binding.tvEmptySubtitle.text = getString(R.string.explore_empty_subtitle)
            }
        } else {
            binding.layoutEmptyExplore.visibility = View.GONE
            binding.rvExploreEvents.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
