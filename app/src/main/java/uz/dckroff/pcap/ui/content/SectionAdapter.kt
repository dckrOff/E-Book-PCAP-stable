package uz.dckroff.pcap.ui.content

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.databinding.ItemChapterBinding

/**
 * Адаптер для отображения списка разделов
 */
class SectionAdapter(
    private val onSectionClick: (ContentItem.Section) -> Unit
) : ListAdapter<ContentItem.Section, SectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SectionViewHolder(private val binding: ItemChapterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.sectionContainer.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val section = getItem(position)
                    onSectionClick(section)
                }
            }
        }

        fun bind(section: ContentItem.Section) {
            binding.apply {
                tvChapterNumber.text = "Глава ${section.order + 1}"
                tvChapterTitle.text = section.title
                tvChapterDescription.text = section.description

                // Показываем прогресс, если прочитано
                ivReadStatus.visibility = if (section.progress >= 100) View.VISIBLE else View.GONE
            }
        }
    }

    private class SectionDiffCallback : DiffUtil.ItemCallback<ContentItem.Section>() {
        override fun areItemsTheSame(
            oldItem: ContentItem.Section,
            newItem: ContentItem.Section
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ContentItem.Section,
            newItem: ContentItem.Section
        ): Boolean {
            return oldItem == newItem
        }
    }
}
