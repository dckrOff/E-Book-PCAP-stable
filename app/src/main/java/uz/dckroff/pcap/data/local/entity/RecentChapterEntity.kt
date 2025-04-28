package uz.dckroff.pcap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "recent_chapters")
data class RecentChapterEntity(
    @PrimaryKey
    val id: String,
    val lastViewedAt: Date = Date() // Время последнего просмотра, по умолчанию текущее время
)