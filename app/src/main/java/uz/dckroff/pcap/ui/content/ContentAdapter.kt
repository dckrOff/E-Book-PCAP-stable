package uz.dckroff.pcap.ui.content

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.databinding.ItemChapterBinding
import uz.dckroff.pcap.databinding.ItemSubchapterBinding

/**
 * Адаптер для отображения содержания учебника с главами и подразделами
 */
class ContentAdapter(
    private val onChapterClick: (ContentItem.Chapter) -> Unit,
    private val onSubchapterClick: (ContentItem.Subchapter) -> Unit
) : ListAdapter<ContentItem.Chapter, ContentAdapter.ChapterViewHolder>(ChapterDiffCallback()) {

    // Карта для хранения состояния раскрытия каждой главы
    private val expandedChapters = mutableMapOf<String, Boolean>()
    
    /**
     * Преобразует список глав в список для адаптера
     */
    fun submitChaptersWithSubchapters(chapters: List<ContentItem.Chapter>) {
        submitList(chapters)
    }
    
    /**
     * Проверяет, развёрнута ли глава
     */
    private fun isChapterExpanded(chapter: ContentItem.Chapter): Boolean {
        return expandedChapters[chapter.id] ?: false
    }
    
    /**
     * Переключает состояние раскрытия/свёртывания главы
     */
    fun toggleChapterExpanded(chapter: ContentItem.Chapter) {
        expandedChapters[chapter.id] = !(expandedChapters[chapter.id] ?: false)
        notifyItemChanged(currentList.indexOf(chapter))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChapterViewHolder(private val binding: ItemChapterBinding) : 
            RecyclerView.ViewHolder(binding.root) {
        
        private val subchapterAdapter = SubchapterAdapter { subchapter ->
            onSubchapterClick(subchapter)
        }
        
        init {
//            binding.chapterContainer.setOnClickListener {
//                val position = bindingAdapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    val chapter = getItem(position)
//                    onChapterClick(chapter)
//                }
//            }
            
            binding.chapterContainer.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val chapter = getItem(position)
                    toggleChapterExpanded(chapter)
                }
            }
            
            // Настройка вложенного RecyclerView для подразделов
            binding.rvSubchapters.apply {
                adapter = subchapterAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
            }
        }
        
        fun bind(chapter: ContentItem.Chapter) {
            binding.apply {
                tvChapterNumber.text = "Глава ${chapter.number}"
                tvChapterTitle.text = chapter.title
                tvChapterDescription.text = chapter.description
                
                // Устанавливаем видимость индикатора раскрытия
                ivExpandIndicator.visibility = if (chapter.sections.isNotEmpty() || chapter.hasSubchapters) View.VISIBLE else View.GONE
                
                // Устанавливаем состояние индикатора раскрытия
                ivExpandIndicator.setImageResource(
                    if (isChapterExpanded(chapter)) R.drawable.ic_arrow_up
                    else R.drawable.ic_arrow_down
                )
                
                // Отображаем/скрываем контейнер с подразделами
                subchaptersContainer.visibility = 
                    if (isChapterExpanded(chapter) && (chapter.sections.isNotEmpty() || chapter.hasSubchapters)) 
                        View.VISIBLE 
                    else 
                        View.GONE
                
                // Если глава раскрыта, показываем её подразделы
                if (isChapterExpanded(chapter)) {
                    subchapterAdapter.submitList(chapter.sections.map { section ->
                        ContentItem.Subchapter(
                            id = section.id,
                            parentId = section.chapterId,
                            title = section.title,
                            number = section.order,
                            progress = section.progress,
                            contentUrl = section.contentUrl
                        )
                    })
                }
            }
        }
    }
    
    /**
     * Адаптер для отображения подразделов внутри главы
     */
    private class SubchapterAdapter(
        private val onSubchapterClick: (ContentItem.Subchapter) -> Unit
    ) : ListAdapter<ContentItem.Subchapter, SubchapterAdapter.SubchapterViewHolder>(SubchapterDiffCallback()) {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubchapterViewHolder {
            val binding = ItemSubchapterBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return SubchapterViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: SubchapterViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        
        inner class SubchapterViewHolder(private val binding: ItemSubchapterBinding) : 
                RecyclerView.ViewHolder(binding.root) {
            
            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val subchapter = getItem(position)
                        onSubchapterClick(subchapter)
                    }
                }
            }
            
            fun bind(subchapter: ContentItem.Subchapter) {
                binding.apply {
                    tvSubchapterNumber.text = "${subchapter.parentId.substringAfter("ch").toInt()}.${subchapter.number}"
                    tvSubchapterTitle.text = subchapter.title
                    
                    // Отображаем индикатор прогресса, если раздел прочитан полностью
                    ivProgress.visibility = if (subchapter.progress >= 100) View.VISIBLE else View.GONE
                }
            }
        }
        
        private class SubchapterDiffCallback : DiffUtil.ItemCallback<ContentItem.Subchapter>() {
            override fun areItemsTheSame(oldItem: ContentItem.Subchapter, newItem: ContentItem.Subchapter): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: ContentItem.Subchapter, newItem: ContentItem.Subchapter): Boolean {
                return oldItem == newItem
            }
        }
    }
    
    private class ChapterDiffCallback : DiffUtil.ItemCallback<ContentItem.Chapter>() {
        override fun areItemsTheSame(oldItem: ContentItem.Chapter, newItem: ContentItem.Chapter): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ContentItem.Chapter, newItem: ContentItem.Chapter): Boolean {
            return oldItem == newItem
        }
    }
} 