package com.example.eventkampus.ui.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eventkampus.databinding.ItemAdminSummaryRowBinding

data class SummaryRow(
    val primary: String,
    val secondary: String,
    val badge: String? = null,
    val badgeBg: String = "#E3F2FD",
    val badgeText: String = "#0D47A1"
)

class SummaryRowAdapter(
    private val rows: List<SummaryRow>
) : RecyclerView.Adapter<SummaryRowAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAdminSummaryRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(position + 1, rows[position])

    inner class VH(private val b: ItemAdminSummaryRowBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(number: Int, row: SummaryRow) {
            b.tvRowNumber.text    = number.toString()
            b.tvRowPrimary.text   = row.primary
            b.tvRowSecondary.text = row.secondary

            if (row.badge != null) {
                b.tvRowBadge.visibility = View.VISIBLE
                b.tvRowBadge.text = row.badge
                b.tvRowBadge.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(row.badgeBg))
                b.tvRowBadge.setTextColor(Color.parseColor(row.badgeText))
            } else {
                b.tvRowBadge.visibility = View.GONE
            }
        }
    }
}
