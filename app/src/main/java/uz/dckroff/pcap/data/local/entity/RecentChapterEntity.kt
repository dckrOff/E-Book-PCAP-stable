package uz.dckroff.pcap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность для недавно просмотренных разделов
 */
@Entity(tableName = "recent_chapters")
data class RecentChapterEntity(
    @PrimaryKey
    val chapterId: String,
    val lastViewedAt: Long = System.currentTimeMillis()
) 