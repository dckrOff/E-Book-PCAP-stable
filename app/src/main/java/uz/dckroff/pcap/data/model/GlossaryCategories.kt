package uz.dckroff.pcap.data.model

/**
 * Категории для терминов глоссария
 */
object GlossaryCategories {
    const val ALL = "Все категории"
    const val NETWORK_PROTOCOLS = "Сетевые протоколы"
    const val SECURITY = "Безопасность"
    const val HARDWARE = "Аппаратное обеспечение"
    const val SOFTWARE = "Программное обеспечение"
    const val NETWORKING = "Сети"
    
    /**
     * Получить локализованное название категории
     */
    fun getLocalizedName(category: String): String {
        return when (category) {
            ALL -> "Все категории"
            NETWORK_PROTOCOLS -> "Сетевые протоколы"
            SECURITY -> "Безопасность"
            HARDWARE -> "Аппаратное обеспечение"
            SOFTWARE -> "Программное обеспечение"
            NETWORKING -> "Сети"
            else -> category
        }
    }
    
    /**
     * Получить список всех категорий
     */
    fun getAllCategories(): List<String> {
        return listOf(
            ALL,
            NETWORK_PROTOCOLS,
            SECURITY,
            HARDWARE,
            SOFTWARE,
            NETWORKING
        )
    }
} 