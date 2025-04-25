package uz.dckroff.pcap.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.databinding.ItemChapterBinding
import uz.dckroff.pcap.databinding.ItemSubchapterBinding

/**
 * Адаптер для отображения содержания учебника с главами и разделами
 */
class ContentAdapter(
    private val onChapterClick: (ContentItem.Chapter) -> Unit,
    private val onSubchapterClick: (ContentItem.Subchapter) -> Unit
) : ListAdapter<ContentItem, RecyclerView.ViewHolder>(ContentDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CHAPTER = 0
        private const val VIEW_TYPE_SUBCHAPTER = 1
        private const val VIEW_TYPE_SECTION = 2
    }
    
    // Карта для хранения состояния раскрытия каждой главы
    private val expandedChapters = mutableMapOf<String, Boolean>()
    
    /**
     * Преобразует список глав и подразделов в плоский список для адаптера
     */
    fun submitChaptersWithSubchapters(chapters: List<ContentItem.Chapter>) {
        val items = mutableListOf<ContentItem>()
        
        chapters.forEach { chapter ->
            items.add(chapter)
            
            if (isChapterExpanded(chapter) && (chapter.hasSubchapters || chapter.sections.isNotEmpty())) {
                // Добавляем подразделы из секций, если глава развёрнута
                chapter.sections.forEach { section ->
                    items.add(section)
                }
            }
        }
        
        submitList(items)
    }
    
    private fun isChapterExpanded(chapter: ContentItem.Chapter): Boolean {
        return expandedChapters[chapter.id] ?: false
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ContentItem.Chapter -> VIEW_TYPE_CHAPTER
            is ContentItem.Subchapter -> VIEW_TYPE_SUBCHAPTER
            is ContentItem.Section -> VIEW_TYPE_SECTION
            else -> throw IllegalArgumentException("Неизвестный тип: ${item.javaClass.simpleName}")
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CHAPTER -> {
                val binding = ItemChapterBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ChapterViewHolder(binding)
            }
            VIEW_TYPE_SUBCHAPTER, VIEW_TYPE_SECTION -> {
                val binding = ItemSubchapterBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SubchapterViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Неизвестный viewType: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContentItem.Chapter -> (holder as ChapterViewHolder).bind(item)
            is ContentItem.Subchapter -> (holder as SubchapterViewHolder).bind(item)
            is ContentItem.Section -> (holder as SubchapterViewHolder).bindSection(item)
        }
    }
    
    /**
     * ViewHolder для отображения главы
     */
    inner class ChapterViewHolder(private val binding: ItemChapterBinding) : 
            RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val chapter = getItem(position) as ContentItem.Chapter
                    toggleChapter(chapter)
                    onChapterClick(chapter)
                }
            }
        }
        
        fun bind(chapter: ContentItem.Chapter) {
            binding.apply {
                tvChapterNumber.text = "Глава ${chapter.number}"
                tvChapterTitle.text = chapter.title
                tvChapterDescription.text = chapter.description
                
                // Настройка индикатора раскрытия/свертывания
                val iconResId = if (isChapterExpanded(chapter)) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                ivExpandIndicator.setImageResource(iconResId)
            }
        }
        
        private fun toggleChapter(chapter: ContentItem.Chapter) {
            expandedChapters[chapter.id] = !(expandedChapters[chapter.id] ?: false)
            submitChaptersWithSubchapters(currentList.filterIsInstance<ContentItem.Chapter>())
        }
    }
    
    /**
     * ViewHolder для отображения подраздела
     */
    inner class SubchapterViewHolder(private val binding: ItemSubchapterBinding) : 
            RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    when (val item = getItem(position)) {
                        is ContentItem.Subchapter -> onSubchapterClick(item)
                        is ContentItem.Section -> onSubchapterClick(
                            ContentItem.Subchapter(
                                id = item.id,
                                parentId = item.chapterId,
                                title = item.title,
                                number = item.order,
                                progress = item.progress,
                                contentUrl = item.contentUrl
                            )
                        )
                        else -> {}
                    }
                }
            }
        }
        
        fun bind(subchapter: ContentItem.Subchapter) {
            binding.apply {
                tvSubchapterNumber.text = "${subchapter.parentId.substringAfter("ch").toInt()}.${subchapter.number}"
                tvSubchapterTitle.text = subchapter.title
            }
        }
        
        fun bindSection(section: ContentItem.Section) {
            binding.apply {
                tvSubchapterNumber.text = section.number.ifEmpty { "${section.chapterId.substringAfter("ch").toInt()}.${section.order}" }
                tvSubchapterTitle.text = section.title
            }
        }
    }
    
    /**
     * DiffCallback для оптимизации обновлений списка
     */
    private class ContentDiffCallback : DiffUtil.ItemCallback<ContentItem>() {
        override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return when {
                oldItem is ContentItem.Chapter && newItem is ContentItem.Chapter ->
                    oldItem.id == newItem.id
                oldItem is ContentItem.Subchapter && newItem is ContentItem.Subchapter ->
                    oldItem.id == newItem.id
                oldItem is ContentItem.Section && newItem is ContentItem.Section ->
                    oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return oldItem == newItem
        }
    }
} 