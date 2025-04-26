package uz.dckroff.pcap.data.repository

import kotlinx.coroutines.delay
import timber.log.Timber
import uz.dckroff.pcap.data.model.GlossaryBookmark
import uz.dckroff.pcap.data.model.GlossaryTerm
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с закладками терминов глоссария
 */
interface BookmarksRepository {
    /**
     * Получить все закладки терминов
     */
    suspend fun getAllBookmarkedTerms(): List<GlossaryBookmark>
    
    /**
     * Добавить термин в закладки
     */
    suspend fun addTermToBookmarks(term: GlossaryTerm): Boolean
    
    /**
     * Удалить термин из закладок
     */
    suspend fun removeTermFromBookmarks(termId: String): Boolean
    
    /**
     * Проверить, находится ли термин в закладках
     */
    suspend fun isTermBookmarked(termId: String): Boolean
}

/**
 * Реализация репозитория закладок терминов с демо-данными
 */
@Singleton
class BookmarksRepositoryImpl @Inject constructor() : BookmarksRepository {
    
    // Временное хранилище закладок терминов
    private val bookmarkedTerms = mutableListOf<GlossaryBookmark>()
    
    init {
        // Загружаем демо-данные
        if (bookmarkedTerms.isEmpty()) {
            loadDummyBookmarks()
        }
    }
    
    override suspend fun getAllBookmarkedTerms(): List<GlossaryBookmark> {
        // Эмуляция задержки загрузки
        delay(500)
        return bookmarkedTerms.sortedByDescending { it.createdAt }
    }
    
    override suspend fun addTermToBookmarks(term: GlossaryTerm): Boolean {
        try {
            // Проверяем, существует ли уже закладка для этого термина
            if (isTermBookmarked(term.id)) {
                Timber.d("Термин ${term.term} уже в закладках")
                return false
            }
            
            // Создаем новую закладку
            val bookmark = GlossaryBookmark(
                termId = term.id,
                term = term.term,
                definition = term.definition,
                category = term.category,
                createdAt = System.currentTimeMillis()
            )
            
            // Добавляем в список
            bookmarkedTerms.add(bookmark)
            Timber.d("Термин ${term.term} добавлен в закладки")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении термина в закладки: ${e.message}")
            return false
        }
    }
    
    override suspend fun removeTermFromBookmarks(termId: String): Boolean {
        try {
            val bookmark = bookmarkedTerms.find { it.termId == termId }
            if (bookmark != null) {
                bookmarkedTerms.remove(bookmark)
                Timber.d("Термин ${bookmark.term} удален из закладок")
                return true
            }
            Timber.d("Термин с ID $termId не найден в закладках")
            return false
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при удалении термина из закладок: ${e.message}")
            return false
        }
    }
    
    override suspend fun isTermBookmarked(termId: String): Boolean {
        return bookmarkedTerms.any { it.termId == termId }
    }
    
    /**
     * Загрузить демо-данные закладок терминов
     */
    private fun loadDummyBookmarks() {
        bookmarkedTerms.addAll(
            listOf(
                GlossaryBookmark(
                    id = UUID.randomUUID().toString(),
                    termId = "term1",
                    term = "Параллелизм",
                    definition = "Способность системы выполнять несколько операций одновременно",
                    category = "Основные понятия",
                    createdAt = System.currentTimeMillis() - 3600000 * 2 // 2 часа назад
                ),
                GlossaryBookmark(
                    id = UUID.randomUUID().toString(),
                    termId = "term2",
                    term = "Многоядерный процессор",
                    definition = "Процессор, содержащий два или более вычислительных ядра на одном кристалле",
                    category = "Аппаратное обеспечение",
                    createdAt = System.currentTimeMillis() - 3600000 // 1 час назад
                ),
                GlossaryBookmark(
                    id = UUID.randomUUID().toString(),
                    termId = "term3",
                    term = "SIMD",
                    definition = "Single Instruction, Multiple Data - модель параллельных вычислений, при которой одна инструкция выполняется над множеством элементов данных",
                    category = "Модели программирования",
                    createdAt = System.currentTimeMillis() - 1800000 // 30 минут назад
                )
            )
        )
        
        Timber.d("Загружено ${bookmarkedTerms.size} демо-закладок терминов")
    }
} 