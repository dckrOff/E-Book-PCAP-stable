package uz.dckroff.pcap.data.model

/**
 * Модель содержимого, которая может быть главой или подразделом
 */
sealed class ContentItem {
    /**
     * Глава (содержит подразделы)
     */
    data class Chapter(
        val id: String,
        val title: String,
        val description: String = "",
        val number: Int,
        val progress: Int = 0,
        val hasSubchapters: Boolean = false,
        var isExpanded: Boolean = false,
        val sections: List<Section> = emptyList(),
        val order: Int = 0
    ) : ContentItem()

    /**
     * Подраздел (часть главы)
     */
    data class Subchapter(
        val id: String,
        val parentId: String,
        val title: String,
        val number: Int,
        val progress: Int = 0,
        val contentUrl: String? = null
    ) : ContentItem()
    
    /**
     * Раздел главы (используется в некоторых частях приложения)
     */
    data class Section(
        val id: String,
        val chapterId: String,
        val title: String,
        val order: Int,
        val progress: Int = 0,
        val contentUrl: String? = null,
        val number: String = ""
    ) : ContentItem()
} 