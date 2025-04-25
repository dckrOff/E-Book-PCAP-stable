package uz.dckroff.pcap.features.glossary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import uz.dckroff.pcap.R

/**
 * Адаптер для отображения категорий глоссария в виде чипов
 */
class GlossaryCategoriesAdapter(
    private val onCategorySelected: (String) -> Unit
) : ListAdapter<GlossaryCategoryItem, GlossaryCategoriesAdapter.CategoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val chip = LayoutInflater.from(parent.context).inflate(
            R.layout.item_category_chip, parent, false
        ) as Chip
        return CategoryViewHolder(chip)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val chip: Chip
    ) : RecyclerView.ViewHolder(chip) {

        init {
            chip.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCategorySelected(getItem(position).id)
                }
            }
        }

        fun bind(category: GlossaryCategoryItem) {
            chip.text = category.name
            chip.isChecked = category.isSelected
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GlossaryCategoryItem>() {
            override fun areItemsTheSame(
                oldItem: GlossaryCategoryItem, 
                newItem: GlossaryCategoryItem
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: GlossaryCategoryItem,
                newItem: GlossaryCategoryItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
} 