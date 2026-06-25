package com.mohanad.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mohanad.tv.R

/**
 * Adapter خفيف لعرض فئات القنوات كشرائح أفقية قابلة للضغط.
 */
class CategoryAdapter(
    private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<String>()
    private var selectedPosition = 0

    fun submitCategories(newCategories: List<String>) {
        categories.clear()
        categories.addAll(newCategories)
        selectedPosition = 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text_category)

        fun bind(category: String, isSelected: Boolean) {
            text.text = category
            text.isSelected = isSelected
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onCategorySelected(category)
            }
        }
    }
}
