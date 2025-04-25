package uz.dckroff.pcap.features.bookmarks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.Bookmark
import uz.dckroff.pcap.data.repository.BookmarkRepository
import javax.inject.Inject

/**
 * ViewModel для экрана закладок
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    
    // Список закладок
    private val _bookmarks = MutableLiveData<List<Bookmark>>()
    val bookmarks: LiveData<List<Bookmark>> = _bookmarks
    
    // Флаг загрузки
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    
    // Ошибка
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    // Флаг пустого списка
    private val _empty = MutableLiveData(false)
    val empty: LiveData<Boolean> = _empty
    
    init {
        loadBookmarks()
    }
    
    /**
     * Загрузить закладки
     */
    fun loadBookmarks() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val bookmarksList = bookmarkRepository.getAllBookmarks()
                _bookmarks.value = bookmarksList
                _empty.value = bookmarksList.isEmpty()
                Timber.d("Загружено ${bookmarksList.size} закладок")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке закладок: ${e.message}")
                _error.value = "Ошибка при загрузке закладок: ${e.message}"
                _empty.value = true
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Удалить закладку
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
                    _empty.value = updatedList.isEmpty()
                    Timber.d("Закладка $bookmarkId удалена")
                } else {
                    Timber.d("Не удалось удалить закладку $bookmarkId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при удалении закладки: ${e.message}")
            }
        }
    }
} 