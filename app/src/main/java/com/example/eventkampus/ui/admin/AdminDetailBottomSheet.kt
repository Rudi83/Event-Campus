package com.example.eventkampus.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventkampus.databinding.BottomSheetAdminDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AdminDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAdminDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAdminDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        val rows = arguments?.getSerializable(ARG_ROWS) as? ArrayList<RowParcel> ?: arrayListOf()

        binding.tvSheetTitle.text = title
        binding.tvSheetCount.text = rows.size.toString()

        val summaryRows = rows.map {
            SummaryRow(
                primary       = it.primary,
                secondary     = it.secondary,
                badge         = it.badge,
                badgeBg       = it.badgeBg,
                badgeText     = it.badgeTextColor
            )
        }

        binding.rvSheetItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSheetItems.adapter       = SummaryRowAdapter(summaryRows)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG             = "AdminDetailBottomSheet"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_ROWS  = "arg_rows"

        fun newInstance(title: String, rows: List<RowParcel>): AdminDetailBottomSheet {
            return AdminDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putSerializable(ARG_ROWS, ArrayList(rows))
                }
            }
        }
    }
}
