package uz.dckroff.pcap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность раздела для базы данных Room
 */
@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey
    val id: String,
    val chapterId: String,
    val title: String,
    val description: String = "",
    val order: Int,
    val number: Int,
    val contentUrl: String? = null,
    val hasSectionRead: Boolean = false
) 