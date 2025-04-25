package uz.dckroff.pcap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность прогресса пользователя для Room
 */
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val id: String = "user_progress",
    val overallProgress: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) 