package uz.dckroff.pcap.data.model

/**
 * Модель для хранения различных типов контента раздела
 */
sealed class SectionContent {
    open val id: String = ""

    /**
     * Текстовый параграф (обычный текст)
     */
    data class Text(
        override val id: String,
        val content: String,
        val isHighlighted: Boolean = false
    ) : SectionContent()
    
    /**
     * Изображение со схемой или иллюстрацией
     */
    data class Image(
        override val id: String,
        val url: String,
        val caption: String = "",
        val description: String = "",
        val width: Int = 0,
        val height: Int = 0
    ) : SectionContent()
    
    /**
     * Формула (математическая)
     */
    data class Formula(
        override val id: String,
        val content: String,
        val caption: String = "",
        val isInline: Boolean = false
    ) : SectionContent()
    
    /**
     * Код или листинг программы
     */
    data class Code(
        override val id: String,
        val content: String,
        val language: String = "",
        val caption: String = "",
        val lineNumbers: Boolean = true
    ) : SectionContent()
    
    /**
     * Видео контент
     */
    data class Video(
        override val id: String,
        val url: String,
        val caption: String = "",
        val thumbnailUrl: String = "",
        val durationSeconds: Int = 0
    ) : SectionContent()
    
    /**
     * Интерактивная блок-схема
     */
    data class Diagram(
        override val id: String,
        val elements: List<DiagramElement>,
        val caption: String = ""
    ) : SectionContent()
    
    /**
     * Таблица данных
     */
    data class Table(
        override val id: String,
        val rows: List<List<String>>,
        val headers: List<String>,
        val caption: String = ""
    ) : SectionContent()
    
    /**
     * Интерактивный элемент (например, выпадающие блоки)
     */
    data class Interactive(
        override val id: String,
        val type: String,
        val content: Map<String, Any>,
        val caption: String = ""
    ) : SectionContent()
}

/**
 * Элемент блок-схемы
 */
data class DiagramElement(
    val id: String,
    val type: String, // "block", "connector", "decision", etc.
    val text: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val connections: List<String> = emptyList() // IDs of connected elements
) 