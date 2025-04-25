package uz.dckroff.pcap.ui.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.data.repository.BookRepository
import uz.dckroff.pcap.utils.Resource
import uz.dckroff.pcap.utils.UiState
import javax.inject.Inject

/**
 * ViewModel для экрана содержания учебника
 */
@HiltViewModel
class ContentListViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _contentState = MutableStateFlow<UiState<List<ContentItem.Chapter>>>(UiState.Loading)
    val contentState: StateFlow<UiState<List<ContentItem.Chapter>>> = _contentState
    
    private val _originalContent = mutableListOf<ContentItem.Chapter>()
    
    // Поисковый запрос
    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Загружает содержание учебника
     * @param forceRefresh Принудительное обновление данных
     */
    fun loadContent(forceRefresh: Boolean = false) {
        _contentState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                when (val result = bookRepository.getContent(forceRefresh)) {
                    is Resource.Success -> {
                        val chapters = result.data ?: emptyList()
                        if (chapters.isEmpty()) {
                            _contentState.value = UiState.Empty
                        } else {
                            _originalContent.clear()
                            _originalContent.addAll(chapters)
                            _contentState.value = UiState.Success(chapters)
                        }
                        Timber.d("Loaded ${chapters.size} chapters")
                    }
                    is Resource.Error -> {
                        Timber.e("Error loading content: ${result.message}")
                        _contentState.value = UiState.Error(result.message ?: "Неизвестная ошибка")
                    }
                    is Resource.Loading -> {
                        _contentState.value = UiState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading content")
                _contentState.value = UiState.Error("Произошла ошибка: ${e.message}")
            }
        }
    }
    
    /**
     * Фильтрует содержимое по идентификатору главы
     * @param chapterId Идентификатор главы
     */
    fun filterContentByChapter(chapterId: String) {
        val currentState = _contentState.value
        if (currentState is UiState.Success) {
            val selectedChapter = _originalContent.find { it.id == chapterId }
            if (selectedChapter != null) {
                _contentState.value = UiState.Success(listOf(selectedChapter))
                Timber.d("Filtered content for chapter: ${selectedChapter.title}")
            }
        }
    }
    
    /**
     * Сбрасывает фильтр и показывает все главы
     */
    fun resetFilter() {
        if (_originalContent.isNotEmpty()) {
            _contentState.value = UiState.Success(_originalContent)
            Timber.d("Reset content filter")
        } else {
            loadContent()
        }
    }
    
    /**
     * Установить поисковый запрос
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterContentBySearchQuery()
    }
    
    /**
     * Фильтрует содержимое по поисковому запросу
     */
    private fun filterContentBySearchQuery() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            // Если запрос пустой, показываем все
            _contentState.value = UiState.Success(_originalContent)
            return
        }
        
        val filteredContent = _originalContent.filter { chapter ->
            // Поиск по названию главы
            chapter.title.contains(query, ignoreCase = true) ||
            // Поиск по секциям
            chapter.sections.any { section ->
                section.title.contains(query, ignoreCase = true)
            }
        }
        
        if (filteredContent.isEmpty()) {
            _contentState.value = UiState.Empty
        } else {
            _contentState.value = UiState.Success(filteredContent)
        }
        
        Timber.d("Filtered content by query '$query': ${filteredContent.size} chapters")
    }
} 