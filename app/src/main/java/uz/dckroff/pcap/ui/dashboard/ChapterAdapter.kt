package uz.dckroff.pcap.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.dckroff.pcap.databinding.ItemDashboardChapterBinding
import uz.dckroff.pcap.databinding.ItemDashboardChapterWideBinding

/**
 * Адаптер для отображения глав/разделов на главном экране
 */
class ChapterAdapter(
    private val onChapterClicked: (Chapter) -> Unit,
    private val isWideLayout: Boolean = false
) : ListAdapter<Chapter, RecyclerView.ViewHolder>(ChapterDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_WIDE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isWideLayout) VIEW_TYPE_WIDE else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_WIDE -> {
                val binding = ItemDashboardChapterWideBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                WideChapterViewHolder(binding)
            }
            else -> {
                val binding = ItemDashboardChapterBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                NormalChapterViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chapter = getItem(position)
        when (holder) {
            is NormalChapterViewHolder -> holder.bind(chapter)
            is WideChapterViewHolder -> holder.bind(chapter)
        }
    }

    inner class NormalChapterViewHolder(
        private val binding: ItemDashboardChapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onChapterClicked(getItem(position))
                }
            }
        }

        fun bind(chapter: Chapter) {
            binding.apply {
                tvChapterTitle.text = chapter.title
                tvChapterDescription.text = chapter.description
                progressBarChapter.progress = chapter.progress
                tvProgressValue.text = "${chapter.progress}%"
            }
        }
    }

    inner class WideChapterViewHolder(
        private val binding: ItemDashboardChapterWideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onChapterClicked(getItem(position))
                }
            }
        }

        fun bind(chapter: Chapter) {
            binding.apply {
                tvChapterTitle.text = chapter.title
                tvChapterDescription.text = chapter.description
                progressBarChapter.progress = chapter.progress
                tvProgressValue.text = "${chapter.progress}%"
            }
        }
    }

    private class ChapterDiffCallback : DiffUtil.ItemCallback<Chapter>() {
        override fun areItemsTheSame(oldItem: Chapter, newItem: Chapter): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Chapter, newItem: Chapter): Boolean {
            return oldItem == newItem
        }
    }
} 