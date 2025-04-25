package uz.dckroff.pcap.data.repository

import kotlinx.coroutines.delay
import timber.log.Timber
import uz.dckroff.pcap.data.model.Bookmark
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с закладками
 */
interface BookmarkRepository {
    /**
     * Получить все закладки
     */
    suspend fun getAllBookmarks(): List<Bookmark>
    
    /**
     * Добавить закладку
     */
    suspend fun addBookmark(bookmark: Bookmark): Boolean
    
    /**
     * Удалить закладку
     */
    suspend fun deleteBookmark(bookmarkId: String): Boolean
    
    /**
     * Проверить существует ли закладка для раздела
     */
    suspend fun hasBookmarkForSection(sectionId: String): Boolean
}

/**
 * Реализация репозитория закладок с демо-данными
 */
@Singleton
class BookmarkRepositoryImpl @Inject constructor() : BookmarkRepository {
    
    // Временное хранилище закладок
    private val bookmarks = mutableListOf<Bookmark>()
    
    init {
        // Демо-данные
        if (bookmarks.isEmpty()) {
            loadDummyBookmarks()
        }
    }
    
    override suspend fun getAllBookmarks(): List<Bookmark> {
        // Удалена эмуляция задержки загрузки
        return bookmarks.sortedByDescending { it.createdAt }
    }
    
    override suspend fun addBookmark(bookmark: Bookmark): Boolean {
        try {
            // Проверяем, существует ли уже закладка для этого раздела
            val existingBookmark = bookmarks.find { it.sectionId == bookmark.sectionId }
            if (existingBookmark != null) {
                Timber.d("Закладка для раздела ${bookmark.sectionId} уже существует")
                return false
            }
            
            // Добавляем новую закладку
            bookmarks.add(bookmark)
            Timber.d("Добавлена закладка: ${bookmark.sectionTitle}")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении закладки: ${e.message}")
            return false
        }
    }
    
    override suspend fun deleteBookmark(bookmarkId: String): Boolean {
        try {
            val bookmark = bookmarks.find { it.id == bookmarkId }
            if (bookmark != null) {
                bookmarks.remove(bookmark)
                Timber.d("Удалена закладка: ${bookmark.sectionTitle}")
                return true
            }
            Timber.d("Закладка с ID $bookmarkId не найдена")
            return false
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при удалении закладки: ${e.message}")
            return false
        }
    }
    
    override suspend fun hasBookmarkForSection(sectionId: String): Boolean {
        return bookmarks.any { it.sectionId == sectionId }
    }
    
    /**
     * Загрузить демо-данные закладок
     */
    private fun loadDummyBookmarks() {
        bookmarks.addAll(
            listOf(
                Bookmark(
                    id = UUID.randomUUID().toString(),
                    sectionId = "section1",
                    sectionTitle = "Основы компьютерных сетей",
                    chapterTitle = "Глава 1: Введение в сети",
                    createdAt = System.currentTimeMillis() - 3600000 * 2 // 2 часа назад
                ),
                Bookmark(
                    id = UUID.randomUUID().toString(),
                    sectionId = "section2",
                    sectionTitle = "Модель OSI и TCP/IP",
                    chapterTitle = "Глава 1: Введение в сети",
                    createdAt = System.currentTimeMillis() - 3600000 // 1 час назад
                ),
                Bookmark(
                    id = UUID.randomUUID().toString(),
                    sectionId = "section3",
                    sectionTitle = "Протоколы передачи данных",
                    chapterTitle = "Глава 2: Протоколы",
                    createdAt = System.currentTimeMillis() - 1800000 // 30 минут назад
                )
            )
        )
        
        Timber.d("Загружено ${bookmarks.size} демо-закладок")
    }
} 