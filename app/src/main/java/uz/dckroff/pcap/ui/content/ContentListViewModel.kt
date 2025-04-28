package uz.dckroff.pcap.features.content

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

    private val _contentState =
        MutableStateFlow<UiState<List<ContentItem.Section>>>(UiState.Loading)
    val contentState: StateFlow<UiState<List<ContentItem.Section>>> = _contentState

    private val _originalContent = mutableListOf<ContentItem.Section>()

    // Поисковый запрос
    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Загружает содержание учебника
     * @param forceRefresh Принудительное обновление данных
     */
    fun loadContent(chapterId: String, forceRefresh: Boolean = false) {
        _contentState.value = UiState.Loading

        viewModelScope.launch {
            when (val result = bookRepository.getContent(chapterId, forceRefresh)) {
                is Resource.Success -> {
                    val sections = result.data
                    _originalContent.clear()
                    _originalContent.addAll(sections!!)

                    if (sections.isEmpty()) {
                        _contentState.value = UiState.Empty
                    } else {
                        _contentState.value = UiState.Success(sections)
                    }
                    Timber.d("Загружено ${sections.size} разделов")
                }
                is Resource.Error -> {
                    _contentState.value = UiState.Error(result.message!!)
                    Timber.e("Ошибка загрузки содержания: ${result.message}")
                }

                else -> {

                }
            }
        }
    }

    /**
     * Фильтрует содержание по поисковому запросу
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            _contentState.value = UiState.Success(_originalContent)
            return
        }

        val filteredSections = _originalContent.filter { section ->
            section.title.contains(query, ignoreCase = true) || 
            section.description.contains(query, ignoreCase = true)
        }

        if (filteredSections.isEmpty()) {
            _contentState.value = UiState.Empty
        } else {
            _contentState.value = UiState.Success(filteredSections)
        }
    }
} 