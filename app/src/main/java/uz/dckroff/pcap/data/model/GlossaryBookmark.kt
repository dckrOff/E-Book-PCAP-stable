package uz.dckroff.pcap.data.model

import java.util.UUID

/**
 * Модель данных для закладок терминов глоссария
 */
data class GlossaryBookmark(
    val id: String = UUID.randomUUID().toString(),
    val termId: String,         // ID термина
    val term: String,           // Название термина
    val definition: String,     // Определение термина
    val category: String,       // Категория термина
    val createdAt: Long = System.currentTimeMillis()
) 