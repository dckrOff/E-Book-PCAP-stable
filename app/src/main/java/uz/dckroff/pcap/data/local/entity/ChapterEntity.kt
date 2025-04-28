package uz.dckroff.pcap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.data.model.ContentItem.Section

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val order: Int,
    val description: String,
    var isExpanded: Boolean = false,
    val sections: List<Section> = emptyList(),
    val progress: Int = 0
)