package uz.dckroff.pcap.features.glossary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.GlossaryTerm
import uz.dckroff.pcap.data.repository.GlossaryRepository
import uz.dckroff.pcap.utils.Resource
import javax.inject.Inject

/**
 * ViewModel для экрана глоссария
 */
@HiltViewModel
class GlossaryViewModel @Inject constructor(
    private val glossaryRepository: GlossaryRepository
) : ViewModel() {
    
    companion object {
        const val ALL_CATEGORIES = "Все категории"
    }
    
    // Список терминов для отображения
    private val _terms = MutableLiveData<List<GlossaryTerm>>()
    val terms: LiveData<List<GlossaryTerm>> = _terms
    
    // Список категорий
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories
    
    // Выбранная категория
    private val _selectedCategory = MutableLiveData(ALL_CATEGORIES)
    val selectedCategory: LiveData<String> = _selectedCategory
    
    // Текст поиска
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery
    
    // Флаг загрузки
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    
    // Сообщение об ошибке
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    // Флаг пустого списка
    private val _empty = MutableLiveData(false)
    val empty: LiveData<Boolean> = _empty
    
    // Текущий термин для детального просмотра
    private val _currentTerm = MutableLiveData<Resource<GlossaryTerm>>()
    val currentTerm: LiveData<Resource<GlossaryTerm>> = _currentTerm
    
    // Job для дебаунса поиска
    private var searchJob: Job? = null
    
    init {
        // Загружаем категории и термины при инициализации
        loadCategories()
        loadTerms()
    }
    
    /**
     * Загрузить список категорий
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val fetchedCategories = glossaryRepository.getAllCategories()
                val categoriesList = listOf(ALL_CATEGORIES) + fetchedCategories
                _categories.value = categoriesList
                Timber.d("Загружено ${categoriesList.size} категорий")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке категорий")
            }
        }
    }
    
    /**
     * Загрузить термины с учетом фильтров
     */
    fun loadTerms() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val category = _selectedCategory.value ?: ALL_CATEGORIES
                val query = _searchQuery.value ?: ""
                
                val result = if (query.isBlank()) {
                    // Если нет поискового запроса, фильтруем только по категории
                    glossaryRepository.getTermsByCategory(category)
                } else {
                    // Если есть поисковый запрос, выполняем поиск и фильтруем по категории
                    val searchResults = glossaryRepository.searchTerms(query)
                    
                    // Если выбрана категория "Все", возвращаем все результаты поиска
                    if (category == ALL_CATEGORIES) {
                        searchResults
                    } else {
                        // Иначе фильтруем результаты поиска по категории
                        searchResults.filter { it.category == category }
                    }
                }
                
                // Сортируем результаты по алфавиту
                val sortedResult = result.sortedBy { it.term }
                
                _terms.value = sortedResult
                _empty.value = sortedResult.isEmpty()
                
                Timber.d("Загружено ${sortedResult.size} терминов")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке терминов: ${e.message}")
                _error.value = "Ошибка при загрузке терминов: ${e.message}"
                _empty.value = true
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Загрузить термин по ID
     */
    fun loadTerm(termId: String) {
        _currentTerm.value = Resource.Loading()
        
        viewModelScope.launch {
            try {
                val term = glossaryRepository.getTermById(termId)
                if (term != null) {
                    _currentTerm.value = Resource.Success(term)
                    Timber.d("Загружен термин: ${term.term}")
                } else {
                    _currentTerm.value = Resource.Error("Термин не найден")
                    Timber.e("Термин с ID $termId не найден")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке термина: ${e.message}")
                _currentTerm.value = Resource.Error("Ошибка при загрузке термина: ${e.message}")
            }
        }
    }
    
    /**
     * Получить термин по ID
     */
    fun getTermById(termId: String) {
        _currentTerm.value = Resource.Loading()
        
        viewModelScope.launch {
            try {
                val term = glossaryRepository.getTermById(termId)
                if (term != null) {
                    _currentTerm.value = Resource.Success(term)
                    Timber.d("Загружен термин: ${term.term}")
                } else {
                    _currentTerm.value = Resource.Error("Термин не найден")
                    Timber.e("Термин с ID $termId не найден")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке термина: ${e.message}")
                _currentTerm.value = Resource.Error("Ошибка при загрузке термина: ${e.message}")
            }
        }
    }
    
    /**
     * Получить термин по ID (синхронно из кэша, для внутреннего использования)
     */
    fun getTermByIdSync(termId: String): GlossaryTerm? {
        return try {
            // Попытка найти термин в кэше (в текущем списке)
            terms.value?.find { it.id == termId }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении термина: ${e.message}")
            null
        }
    }
    
    /**
     * Установить выбранную категорию
     */
    fun setCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            loadTerms()
        }
    }
    
    /**
     * Установить поисковый запрос
     */
    fun setSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
            
            // Используем дебаунс для поиска (300 мс)
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(300)
                loadTerms()
            }
        }
    }
    
    /**
     * Очистить поисковый запрос
     */
    fun clearSearchQuery() {
        setSearchQuery("")
    }
} 