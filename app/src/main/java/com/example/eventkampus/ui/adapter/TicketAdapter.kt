package com.example.eventkampus.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.databinding.ItemTicketCardBinding

class TicketAdapter(
    private val onTicketClick: (Ticket) -> Unit
) : ListAdapter<Ticket, TicketAdapter.TicketViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TicketViewHolder(
        private val binding: ItemTicketCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            binding.tvTicketId.text = "TIKET: #${ticket.ticketId}"
            binding.tvTicketStatus.text = ticket.status
            binding.tvTicketEventTitle.text = ticket.eventTitle
            binding.tvTicketParticipant.text = "Peserta: ${ticket.participantName} (NIM: ${ticket.participantNim})"
            binding.tvTicketDateTime.text = ticket.eventDateTime
            binding.tvTicketLocation.text = ticket.eventLocation

            binding.btnViewQr.setOnClickListener {
                onTicketClick(ticket)
            }
            binding.root.setOnClickListener {
                onTicketClick(ticket)
            }
        }
    }

    class TicketDiffCallback : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean = oldItem.ticketId == newItem.ticketId
        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean = oldItem == newItem
    }
}