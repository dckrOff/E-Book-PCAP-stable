package uz.dckroff.pcap.features.glossary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.GlossaryTerm
import uz.dckroff.pcap.data.repository.GlossaryRepository
import javax.inject.Inject

@HiltViewModel
class GlossaryListViewModel @Inject constructor(
    private val repository: GlossaryRepository
) : ViewModel() {

    // LiveData для списка терминов
    private val _terms = MutableLiveData<List<GlossaryTerm>>(emptyList())
    val terms: LiveData<List<GlossaryTerm>> = _terms

    // LiveData для списка категорий
    private val _categories = MutableLiveData<List<String>>(emptyList())
    val categories: LiveData<List<String>> = _categories

    // LiveData для выбранной категории
    private val _selectedCategory = MutableLiveData<String?>(null)
    val selectedCategory: LiveData<String?> = _selectedCategory

    // LiveData для текущего поискового запроса
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    // LiveData для состояния загрузки
    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    // LiveData для ошибок
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // LiveData для навигации к детальному просмотру
    private val _navigation = MutableLiveData<GlossaryTerm?>(null)
    val navigation: LiveData<GlossaryTerm?> = _navigation

    // Кэш всех терминов для фильтрации
    private var allTerms = listOf<GlossaryTerm>()

    /**
     * Загрузка списка терминов
     */
    fun loadTerms(forceRefresh: Boolean = false) {
        if (_loading.value == true && !forceRefresh) return
        
        _loading.value = true
        viewModelScope.launch {
            try {
                allTerms = repository.getAllTerms()
                filterAndUpdateTerms()
                _loading.value = false
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке терминов глоссария")
                _error.value = "Не удалось загрузить термины глоссария: ${e.localizedMessage}"
                _loading.value = false
            }
        }
    }

    /**
     * Загрузка списка категорий
     */
    fun loadCategories(forceRefresh: Boolean = false) {
        if (_loading.value == true && !forceRefresh) return
        
        _loading.value = true
        viewModelScope.launch {
            try {
                _categories.value = repository.getAllCategories()
                _loading.value = false
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке категорий глоссария")
                _error.value = "Не удалось загрузить категории глоссария: ${e.localizedMessage}"
                _loading.value = false
            }
        }
    }

    /**
     * Установка выбранной категории
     */
    fun setSelectedCategory(category: String?) {
        if (_selectedCategory.value == category) {
            // Если категория уже выбрана, сбрасываем выбор
            _selectedCategory.value = null
        } else {
            _selectedCategory.value = category
        }
        filterAndUpdateTerms()
    }

    /**
     * Установка поискового запроса
     */
    fun setSearchQuery(query: String) {
        if (_searchQuery.value == query) return
        
        _searchQuery.value = query
        filterAndUpdateTerms()
    }

    /**
     * Фильтрация и обновление списка терминов
     */
    private fun filterAndUpdateTerms() {
        val query = _searchQuery.value?.lowercase() ?: ""
        val category = _selectedCategory.value
        
        val filteredTerms = allTerms.filter { term ->
            val matchesQuery = query.isEmpty() || 
                term.term.lowercase().contains(query) || 
                term.definition.lowercase().contains(query)
            
            val matchesCategory = category == null || term.category == category
            
            matchesQuery && matchesCategory
        }
        
        _terms.value = filteredTerms
    }

    /**
     * Выбор термина для детального просмотра
     */
    fun selectTerm(term: GlossaryTerm) {
        _navigation.value = term
    }

    /**
     * Сброс навигации
     */
    fun resetNavigation() {
        _navigation.value = null
    }
} 