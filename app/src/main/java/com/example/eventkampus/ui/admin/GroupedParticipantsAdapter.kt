package com.example.eventkampus.ui.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eventkampus.data.model.Ticket
import com.example.eventkampus.databinding.ItemEventSectionHeaderBinding
import com.example.eventkampus.databinding.ItemParticipantBinding

/** Item data untuk RecyclerView multi-viewtype */
sealed class GroupedItem {
    data class Header(
        val eventId: String,
        val eventTitle: String,
        val category: String,
        val count: Int
    ) : GroupedItem()

    data class ParticipantItem(val ticket: Ticket) : GroupedItem()
}

class GroupedParticipantsAdapter(
    private val onApprove: (Ticket) -> Unit,
    private val onReject:  (Ticket) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<GroupedItem>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM   = 1
    }

    fun submitGrouped(grouped: Map<String, List<Ticket>>, allEvents: List<com.example.eventkampus.data.model.Event>) {
        items.clear()
        // Urutkan event berdasarkan urutan di allEvents
        val eventOrder = allEvents.associate { it.id to it }

        grouped.entries
            .sortedBy { (eventId, _) -> allEvents.indexOfFirst { it.id == eventId } }
            .forEach { (eventId, tickets) ->
                val ev = eventOrder[eventId]
                items.add(
                    GroupedItem.Header(
                        eventId    = eventId,
                        eventTitle = tickets.firstOrNull()?.eventTitle ?: "Kegiatan",
                        category   = tickets.firstOrNull()?.eventCategory ?: "",
                        count      = tickets.size
                    )
                )
                tickets.forEach { items.add(GroupedItem.ParticipantItem(it)) }
            }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is GroupedItem.Header) TYPE_HEADER else TYPE_ITEM

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemEventSectionHeaderBinding.inflate(inflater, parent, false))
        } else {
            ItemVH(ItemParticipantBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GroupedItem.Header          -> (holder as HeaderVH).bind(item)
            is GroupedItem.ParticipantItem -> (holder as ItemVH).bind(item.ticket)
        }
    }

    // ── ViewHolder: Section Header ───────────────────────────────────────────

    inner class HeaderVH(private val b: ItemEventSectionHeaderBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(header: GroupedItem.Header) {
            b.tvSectionTitle.text = header.eventTitle
            b.tvSectionMeta.text  = "${header.category} • ${header.count} peserta"
            b.tvSectionCount.text = header.count.toString()
        }
    }

    // ── ViewHolder: Participant Item ─────────────────────────────────────────

    inner class ItemVH(private val b: ItemParticipantBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(ticket: Ticket) {
            val initials = ticket.participantName.trim().split(" ")
                .filter { it.isNotBlank() }.take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifEmpty { "?" }
            b.tvParticipantInitial.text = initials
            b.tvParticipantName.text    = ticket.participantName
            b.tvParticipantNim.text     = "${ticket.participantNim} • ${ticket.participantProdi}"
            b.tvParticipantDate.text    = "Daftar: ${ticket.registeredDate}"

            val (bgColor, textColor, label) = when (ticket.status) {
                "Disetujui" -> Triple("#E8F5E9", "#2E7D32", "Disetujui ✓")
                "Ditolak"   -> Triple("#FFEBEE", "#C62828", "Ditolak ✗")
                "Hadir"     -> Triple("#E3F2FD", "#0D47A1", "Hadir")
                "Batal"     -> Triple("#F5F5F5", "#757575", "Batal")
                else        -> Triple("#FFF3E0", "#E65100", "Menunggu")
            }
            b.tvParticipantStatus.text = label
            b.tvParticipantStatus.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(bgColor))
            b.tvParticipantStatus.setTextColor(Color.parseColor(textColor))

            val showActions = ticket.status == "Terdaftar"
            b.layoutActionButtons.visibility = if (showActions) View.VISIBLE else View.GONE
            b.btnApprove.setOnClickListener { onApprove(ticket) }
            b.btnReject.setOnClickListener  { onReject(ticket) }
        }
    }
}
