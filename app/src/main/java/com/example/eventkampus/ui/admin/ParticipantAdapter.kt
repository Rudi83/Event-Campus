package com.example.eventkampus.ui.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.databinding.ItemParticipantBinding

class ParticipantAdapter(
    private val onApprove: (Ticket) -> Unit,
    private val onReject: (Ticket) -> Unit
) : ListAdapter<Ticket, ParticipantAdapter.ParticipantViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ParticipantViewHolder(
        private val binding: ItemParticipantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            // Avatar initial
            val initials = ticket.participantName.trim().split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifEmpty { "?" }
            binding.tvParticipantInitial.text = initials

            binding.tvParticipantName.text = ticket.participantName
            binding.tvParticipantNim.text = "${ticket.participantNim} • ${ticket.participantProdi}"
            binding.tvParticipantDate.text = "Daftar: ${ticket.registeredDate}"

            // Status badge styling
            val (bgColor, textColor, label) = when (ticket.status) {
                "Disetujui" -> Triple("#E8F5E9", "#2E7D32", "Disetujui ✓")
                "Ditolak"   -> Triple("#FFEBEE", "#C62828", "Ditolak ✗")
                "Hadir"     -> Triple("#E3F2FD", "#0D47A1", "Hadir")
                "Batal"     -> Triple("#F5F5F5", "#757575", "Batal")
                else        -> Triple("#FFF3E0", "#E65100", "Menunggu")  // Terdaftar
            }
            binding.tvParticipantStatus.text = label
            binding.tvParticipantStatus.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(bgColor))
            binding.tvParticipantStatus.setTextColor(Color.parseColor(textColor))

            // Tombol aksi hanya tampil saat status "Terdaftar" (menunggu review)
            val showActions = ticket.status == "Terdaftar"
            binding.layoutActionButtons.visibility = if (showActions) View.VISIBLE else View.GONE

            binding.btnApprove.setOnClickListener { onApprove(ticket) }
            binding.btnReject.setOnClickListener { onReject(ticket) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket) =
            oldItem.ticketId == newItem.ticketId
        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket) =
            oldItem == newItem
    }
}
