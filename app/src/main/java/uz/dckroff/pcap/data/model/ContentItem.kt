package uz.dckroff.pcap.data.model

/**
 * Модель содержимого, которая может быть главой или разделом
 */
sealed class ContentItem {
    /**
     * Глава (содержит разделы)
     */
    data class Chapter(
        val id: String,
        val title: String,
        val order: Int,
        val description: String,
        val sectionsCount: Int = 0,
        var isExpanded: Boolean = false,
        val sections: List<Section> = emptyList(),
        val progress: Int = 0
    ) : ContentItem()

    /**
     * Раздел главы
     */
    data class Section(
        val id: String,
        val chapterId: String,
        val title: String,
        val description: String = "",
        val order: Int,
        val number: Int,
        val progress: Int = 0,
        val contentUrl: String? = null
    ) : ContentItem()
} 