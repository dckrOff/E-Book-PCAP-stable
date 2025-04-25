package uz.dckroff.pcap.data.model

import java.util.*

/**
 * Модель данных для закладок
 */
data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val sectionId: String,      // ID раздела
    val sectionTitle: String,   // Название раздела
    val chapterTitle: String,   // Название главы
    val createdAt: Long = System.currentTimeMillis()
) 