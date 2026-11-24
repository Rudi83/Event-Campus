package com.example.eventkampus.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eventkampus.R
import com.example.eventkampus.data.model.Category
import com.example.eventkampus.databinding.ItemCategoryChipBinding

class CategoryAdapter(
    private var categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, isSelected: Boolean) {
            val context = binding.root.context
            binding.tvCategoryName.text = category.name

            if (isSelected) {
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
                binding.cardCategory.strokeColor = ContextCompat.getColor(context, R.color.primary)
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(context, R.color.white)
                )
            } else {
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_card)
                )
                binding.cardCategory.strokeColor = ContextCompat.getColor(context, R.color.stroke_border)
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
            }

            binding.root.setOnClickListener {
                val prev = selectedIndex
                selectedIndex = adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedIndex)
                onCategoryClick(category)
            }
        }
    }
}