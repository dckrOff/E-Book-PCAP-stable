package uz.dckroff.pcap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import uz.dckroff.pcap.features.dashboard.Chapter

/**
 * Сущность главы/раздела для Room
 */
@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val order: Int,
    val parentId: String?,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Преобразование в модель UI
     */
    fun toChapter(): Chapter {
        return Chapter(
            id = id,
            title = title,
            description = description,
            progress = progress,
            order = order,
            parentId = parentId
        )
    }
    
    companion object {
        /**
         * Преобразование из модели UI
         */
        fun fromChapter(chapter: Chapter): ChapterEntity {
            return ChapterEntity(
                id = chapter.id,
                title = chapter.title,
                description = chapter.description,
                progress = chapter.progress,
                order = chapter.order,
                parentId = chapter.parentId
            )
        }
    }
} 