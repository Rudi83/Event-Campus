package com.example.eventkampus.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventkampus.R
import com.example.eventkampus.data.model.Event
import com.example.eventkampus.databinding.ItemEventCardBinding

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemEventCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            val context = binding.root.context

            binding.tvEventTitle.text = event.title
            binding.tvOrganizer.text = event.organizer
            binding.tvBannerSpeaker.text = context.getString(R.string.item_speaker_prefix, event.speaker)
            binding.tvCategoryBadge.text = event.category
            binding.tvEventDate.text = event.dateTime
            binding.tvEventLocation.text = if (event.isOnline) {
                context.getString(R.string.label_online_prefix, event.location)
            } else {
                event.location
            }

            // Price badge
            if (event.isFree) {
                binding.tvPriceBadge.text = "Gratis"
                binding.tvPriceBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.badge_free_container)
                )
                binding.tvPriceBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.badge_free)
                )
            } else {
                binding.tvPriceBadge.text = event.formattedPrice
                binding.tvPriceBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.secondary_container)
                )
                binding.tvPriceBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.secondary)
                )
            }

            // Quota
            val percentage = if (event.quota > 0) {
                ((event.registeredCount.toFloat() / event.quota) * 100).toInt()
            } else 0
            binding.pbQuota.progress = percentage
            binding.tvQuotaInfo.text = context.getString(R.string.item_quota_remaining, event.remainingQuota)

            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
    }
}