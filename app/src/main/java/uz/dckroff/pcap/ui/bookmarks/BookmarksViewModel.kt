package uz.dckroff.pcap.ui.bookmarks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.Bookmark
import uz.dckroff.pcap.data.model.GlossaryBookmark
import uz.dckroff.pcap.data.repository.BookmarkRepository
import uz.dckroff.pcap.data.repository.BookmarksRepository
import javax.inject.Inject

/**
 * ViewModel для экрана закладок
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val bookmarksRepository: BookmarksRepository
) : ViewModel() {
    
    // Список закладок разделов
    private val _bookmarks = MutableLiveData<List<Bookmark>>()
    val bookmarks: LiveData<List<Bookmark>> = _bookmarks
    
    // Список закладок терминов глоссария
    private val _glossaryBookmarks = MutableLiveData<List<GlossaryBookmark>>()
    val glossaryBookmarks: LiveData<List<GlossaryBookmark>> = _glossaryBookmarks
    
    // Флаг загрузки
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    
    // Ошибка
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    // Флаг пустого списка
    private val _empty = MutableLiveData(false)
    val empty: LiveData<Boolean> = _empty
    
    // Текущий тип отображаемых закладок (0 - разделы, 1 - термины)
    private val _currentTab = MutableLiveData(0)
    val currentTab: LiveData<Int> = _currentTab
    
    init {
        loadAllBookmarks()
    }
    
    /**
     * Загрузить все типы закладок
     */
    fun loadAllBookmarks() {
        loadBookmarks()
        loadGlossaryBookmarks()
    }
    
    /**
     * Установить текущую вкладку
     */
    fun setCurrentTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }
    
    /**
     * Загрузить закладки разделов
     */
    fun loadBookmarks() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val bookmarksList = bookmarkRepository.getAllBookmarks()
                _bookmarks.value = bookmarksList
                updateEmptyState()
                Timber.d("Загружено ${bookmarksList.size} закладок разделов")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке закладок разделов: ${e.message}")
                _error.value = "Ошибка при загрузке закладок: ${e.message}"
                updateEmptyState()
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Загрузить закладки терминов глоссария
     */
    fun loadGlossaryBookmarks() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val bookmarksList = bookmarksRepository.getAllBookmarkedTerms()
                _glossaryBookmarks.value = bookmarksList
                updateEmptyState()
                Timber.d("Загружено ${bookmarksList.size} закладок терминов")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке закладок терминов: ${e.message}")
                _error.value = "Ошибка при загрузке закладок: ${e.message}"
                updateEmptyState()
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Удалить закладку раздела
     */
    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            try {
                val success = bookmarkRepository.deleteBookmark(bookmarkId)
                if (success) {
                    // Обновляем список закладок
                    val currentList = _bookmarks.value?.toMutableList() ?: mutableListOf()
                    val updatedList = currentList.filter { it.id != bookmarkId }
                    _bookmarks.value = updatedList
                    updateEmptyState()
                    Timber.d("Закладка раздела $bookmarkId удалена")
                } else {
                    Timber.d("Не удалось удалить закладку раздела $bookmarkId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при удалении закладки раздела: ${e.message}")
            }
        }
    }
    
    /**
     * Удалить закладку термина глоссария
     */
    fun deleteGlossaryBookmark(termId: String) {
        viewModelScope.launch {
            try {
                val success = bookmarksRepository.removeTermFromBookmarks(termId)
                if (success) {
                    // Обновляем список закладок
                    val currentList = _glossaryBookmarks.value?.toMutableList() ?: mutableListOf()
                    val updatedList = currentList.filter { it.termId != termId }
                    _glossaryBookmarks.value = updatedList
                    updateEmptyState()
                    Timber.d("Закладка термина $termId удалена")
                } else {
                    Timber.d("Не удалось удалить закладку термина $termId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при удалении закладки термина: ${e.message}")
            }
        }
    }
    
    /**
     * Обновить состояние пустого списка в зависимости от выбранной вкладки
     */
    private fun updateEmptyState() {
        val isEmpty = when (_currentTab.value) {
            0 -> _bookmarks.value.isNullOrEmpty()
            1 -> _glossaryBookmarks.value.isNullOrEmpty()
            else -> true
        }
        _empty.value = isEmpty
    }
} 
