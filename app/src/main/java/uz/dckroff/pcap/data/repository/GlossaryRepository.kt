package uz.dckroff.pcap.data.repository

import timber.log.Timber
import uz.dckroff.pcap.data.model.GlossaryCategories
import uz.dckroff.pcap.data.model.GlossaryTerm
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с глоссарием
 */
interface GlossaryRepository {
    /**
     * Получить все термины глоссария
     */
    suspend fun getAllTerms(): List<GlossaryTerm>
    
    /**
     * Получить термины глоссария по категории
     */
    suspend fun getTermsByCategory(category: String): List<GlossaryTerm>
    
    /**
     * Поиск терминов по тексту
     */
    suspend fun searchTerms(query: String): List<GlossaryTerm>
    
    /**
     * Получить термин по ID
     */
    suspend fun getTermById(termId: String): GlossaryTerm?
    
    /**
     * Получить список всех категорий
     */
    suspend fun getAllCategories(): List<String>
    
    companion object {
        @Volatile
        private var INSTANCE: GlossaryRepository? = null
        
        fun getInstance(): GlossaryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = GlossaryRepositoryImpl()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Реализация репозитория глоссария с демо-данными
 */
@Singleton
class GlossaryRepositoryImpl @Inject constructor() : GlossaryRepository {
    
    // Кэш терминов
    private val terms = mutableListOf<GlossaryTerm>()
    
    /**
     * Получить все термины глоссария
     */
    override suspend fun getAllTerms(): List<GlossaryTerm> {
        // Если кэш пуст, загружаем демо-данные
        if (terms.isEmpty()) {
            loadDummyTerms()
        }
        return terms
    }
    
    /**
     * Получить термины глоссария по категории
     */
    override suspend fun getTermsByCategory(category: String): List<GlossaryTerm> {
        // Если кэш пуст, загружаем демо-данные
        if (terms.isEmpty()) {
            loadDummyTerms()
        }
        
        // Если выбрана категория "Все", возвращаем все термины
        if (category == GlossaryCategories.ALL) {
            return terms
        }
        
        // Иначе фильтруем по выбранной категории
        return terms.filter { it.category == category }
    }
    
    /**
     * Поиск терминов по тексту
     */
    override suspend fun searchTerms(query: String): List<GlossaryTerm> {
        // Если кэш пуст, загружаем демо-данные
        if (terms.isEmpty()) {
            loadDummyTerms()
        }
        
        // Если запрос пустой, возвращаем все термины
        if (query.isBlank()) {
            return terms
        }
        
        // Иначе ищем по термину и определению
        val queryLower = query.lowercase()
        return terms.filter { 
            it.term.lowercase().contains(queryLower) || 
            it.definition.lowercase().contains(queryLower)
        }
    }
    
    /**
     * Получить термин по ID
     */
    override suspend fun getTermById(termId: String): GlossaryTerm? {
        // Если кэш пуст, загружаем демо-данные
        if (terms.isEmpty()) {
            loadDummyTerms()
        }
        
        return terms.find { it.id == termId }
    }
    
    /**
     * Получить список всех категорий
     */
    override suspend fun getAllCategories(): List<String> {
        return GlossaryCategories.getAllCategories()
    }
    
    /**
     * Загрузить демо-данные терминов
     */
    private fun loadDummyTerms() {
        terms.clear()
        
        // Добавляем демо-термины
        terms.addAll(
            listOf(
                // Термины категории "Основы сетей"
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "Локальная сеть (LAN)",
                    definition = "Компьютерная сеть, покрывающая относительно небольшую территорию, обычно ограниченную одним зданием или группой зданий.",
                    category = GlossaryCategories.NETWORKING,
                    relatedTerms = listOf("WAN", "VLAN"),
                    relatedSectionIds = listOf("chapter1", "chapter2")
                ),
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "Глобальная сеть (WAN)",
                    definition = "Компьютерная сеть, охватывающая большие территории, включая континенты и страны.",
                    category = GlossaryCategories.NETWORKING,
                    relatedTerms = listOf("LAN", "MAN"),
                    relatedSectionIds = listOf("chapter1")
                ),
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "IP-адрес",
                    definition = "Уникальный идентификатор устройства в IP-сети. Бывает IPv4 (32-битный) и IPv6 (128-битный).",
                    category = GlossaryCategories.NETWORKING,
                    relatedTerms = listOf("Маска подсети", "DHCP"),
                    relatedSectionIds = listOf("chapter2", "chapter3")
                ),
                
                // Термины категории "Протоколы"
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "TCP (Transmission Control Protocol)",
                    definition = "Транспортный протокол, обеспечивающий надежную передачу данных с установлением соединения.",
                    category = GlossaryCategories.NETWORK_PROTOCOLS,
                    relatedTerms = listOf("UDP", "IP"),
                    relatedSectionIds = listOf("chapter3", "chapter4")
                ),
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "UDP (User Datagram Protocol)",
                    definition = "Транспортный протокол без установления соединения, не гарантирующий доставку пакетов.",
                    category = GlossaryCategories.NETWORK_PROTOCOLS,
                    relatedTerms = listOf("TCP", "IP"),
                    relatedSectionIds = listOf("chapter3", "chapter4")
                ),
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "HTTP (Hypertext Transfer Protocol)",
                    definition = "Протокол прикладного уровня для передачи гипертекстовых документов.",
                    category = GlossaryCategories.NETWORK_PROTOCOLS,
                    relatedTerms = listOf("HTTPS", "TCP"),
                    relatedSectionIds = listOf("chapter5")
                ),
                
                // Термины категории "Безопасность"
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "Брандмауэр (Firewall)",
                    definition = "Программно-аппаратное средство, осуществляющее контроль и фильтрацию сетевого трафика.",
                    category = GlossaryCategories.SECURITY,
                    relatedTerms = listOf("IDS", "IPS"),
                    relatedSectionIds = listOf("chapter6")
                ),
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "VPN (Virtual Private Network)",
                    definition = "Технология, обеспечивающая защищенное соединение через публичную сеть (например, интернет).",
                    category = GlossaryCategories.SECURITY,
                    relatedTerms = listOf("IPsec", "SSL/TLS"),
                    relatedSectionIds = listOf("chapter6")
                ),
                
                // Термины категории "Оборудование"
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "Маршрутизатор (Router)",
                    definition = "Сетевое устройство, пересылающее пакеты между различными сетями на основе правил маршрутизации.",
                    category = GlossaryCategories.HARDWARE,
                    relatedTerms = listOf("Коммутатор", "Шлюз"),
                    relatedSectionIds = listOf("chapter4", "chapter5")
                ),
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "Коммутатор (Switch)",
                    definition = "Устройство, соединяющее несколько узлов компьютерной сети и работающее на канальном уровне модели OSI.",
                    category = GlossaryCategories.HARDWARE,
                    relatedTerms = listOf("Маршрутизатор", "Хаб"),
                    relatedSectionIds = listOf("chapter4")
                ),
                
                // Термины категории "Программное обеспечение"
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "Wireshark",
                    definition = "Программа-анализатор трафика для компьютерных сетей Ethernet и других.",
                    category = GlossaryCategories.SOFTWARE,
                    relatedTerms = listOf("tcpdump", "Анализ пакетов"),
                    relatedSectionIds = listOf("chapter7")
                ),
                GlossaryTerm(
                    id = UUID.randomUUID().toString(),
                    term = "nmap",
                    definition = "Утилита для сканирования портов и исследования сети.",
                    category = GlossaryCategories.SOFTWARE,
                    relatedTerms = listOf("Сканирование портов", "Сетевая безопасность"),
                    relatedSectionIds = listOf("chapter6")
                )
            )
        )
        
        Timber.d("Загружено ${terms.size} терминов")
    }
} 