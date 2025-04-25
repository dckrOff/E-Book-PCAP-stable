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
import uz.dckroff.pcap.data.model.GlossaryCategories
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
    
    // Флаг, указывающий, были ли данные уже загружены
    private var isDataInitialized = false
    
    // Список терминов для отображения
    private val _terms = MutableLiveData<List<GlossaryTerm>>()
    val terms: LiveData<List<GlossaryTerm>> = _terms
    
    // Список категорий
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories
    
    // Выбранная категория
    private val _selectedCategory = MutableLiveData(GlossaryCategories.ALL)
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
        // Загружаем данные только при первой инициализации
        initializeData()
    }
    
    /**
     * Инициализация данных (загружается только один раз)
     */
    private fun initializeData() {
        if (!isDataInitialized) {
            Timber.d("GlossaryViewModel: Loading initial data")
            loadCategories()
            loadTerms()
            isDataInitialized = true
        } else {
            Timber.d("GlossaryViewModel: Data already loaded, skipping initialization")
        }
    }
    
    /**
     * Загрузить список категорий
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categoriesList = glossaryRepository.getAllCategories()
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
                // Отменяем предыдущий поиск, если он выполняется
                searchJob?.cancel()
                
                val query = _searchQuery.value.orEmpty()
                val category = _selectedCategory.value ?: GlossaryCategories.ALL
                
                val filteredTerms = if (query.isNotEmpty()) {
                    glossaryRepository.searchTerms(query)
                } else if (category != GlossaryCategories.ALL) {
                    glossaryRepository.getTermsByCategory(category)
                } else {
                    glossaryRepository.getAllTerms()
                }
                
                _terms.value = filteredTerms
                _empty.value = filteredTerms.isEmpty()
                _loading.value = false
                
                Timber.d("Загружено ${filteredTerms.size} терминов")
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
                Timber.e(e, "Ошибка при загрузке терминов")
            }
        }
    }
    
    /**
     * Поиск терминов по тексту с дебаунсом
     */
    fun searchTerms(query: String) {
        _searchQuery.value = query
        
        // Отменяем предыдущий поиск, если он выполняется
        searchJob?.cancel()
        
        // Создаем новый поиск с задержкой для дебаунса
        searchJob = viewModelScope.launch {
            delay(300) // Дебаунс 300мс для предотвращения частых запросов
            loadTerms()
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
     * Установить категорию для фильтрации
     */
    fun setCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            loadTerms()
        }
    }
    
    /**
     * Загрузить термин по ID
     */
    fun getTermById(termId: String) {
        viewModelScope.launch {
            _currentTerm.value = Resource.Loading()
            
            try {
                val term = glossaryRepository.getTermById(termId)
                if (term != null) {
                    _currentTerm.value = Resource.Success(term)
                } else {
                    _currentTerm.value = Resource.Error("Термин не найден")
                }
            } catch (e: Exception) {
                _currentTerm.value = Resource.Error(e.message ?: "Ошибка загрузки термина")
                Timber.e(e, "Ошибка при загрузке термина: ${e.message}")
            }
        }
    }
} 