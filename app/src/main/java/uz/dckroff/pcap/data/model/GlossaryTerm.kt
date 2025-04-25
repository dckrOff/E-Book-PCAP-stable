package uz.dckroff.pcap.data.model

/**
 * Модель термина глоссария
 */
data class GlossaryTerm(
    val id: String,
    val term: String,
    val definition: String,
    val category: String,
    val relatedTerms: List<String> = emptyList(),
    val relatedSectionIds: List<String> = emptyList()
) 