package com.example.eventkampus.ui.certificate

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.databinding.ItemCertificateBinding
import com.example.eventkampus.util.CertificateLinkStore
import com.example.eventkampus.util.SkpHelper.skpForCategory

class CertificateAdapter(
    private val onOpenLink: (Ticket, String) -> Unit
) : ListAdapter<Ticket, CertificateAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCertificateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCertificateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            binding.tvCertTitle.text     = ticket.eventTitle
            binding.tvCertOrganizer.text = ticket.eventCategory
            binding.tvCertDate.text      = ticket.eventDateTime
            binding.tvCertLocation.text  = ticket.eventLocation
            binding.tvCertSkp.text       = skpForCategory(ticket.eventCategory).toString()
            binding.tvCertNumber.text    = "CERT/UNIMAL/${ticket.eventId}/${ticket.ticketId}"

            // Status badge
            val (bgColor, textColor) = when (ticket.status) {
                "Hadir" -> "#E3F2FD" to "#0D47A1"
                else    -> "#E8F5E9" to "#2E7D32"
            }
            binding.tvCertStatus.text = ticket.status
            binding.tvCertStatus.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(bgColor))
            binding.tvCertStatus.setTextColor(Color.parseColor(textColor))

            // Tombol input link disembunyikan — hanya admin yang bisa input
            binding.btnInputLink.visibility = View.GONE

            // Cek link dari eventId (satu link untuk semua peserta event)
            val ctx       = binding.root.context
            val savedLink = CertificateLinkStore.getLink(ctx, ticket.eventId)

            if (savedLink != null) {
                // Link sudah ada — tampilkan preview dan tombol buka
                binding.layoutCertLink.visibility  = View.VISIBLE
                binding.tvCertLink.text            = savedLink
                binding.btnDownloadCert.visibility = View.VISIBLE
                binding.btnDownloadCert.setOnClickListener { onOpenLink(ticket, savedLink) }
            } else {
                // Link belum diisi admin
                binding.layoutCertLink.visibility  = View.GONE
                binding.btnDownloadCert.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket) =
            oldItem.ticketId == newItem.ticketId
        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket) =
            oldItem == newItem
    }
}
