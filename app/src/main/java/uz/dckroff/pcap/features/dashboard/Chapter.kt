package uz.dckroff.pcap.features.dashboard

/**
 * Модель данных для главы/раздела учебника
 */
data class Chapter(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val order: Int,
    val parentId: String? = null
)