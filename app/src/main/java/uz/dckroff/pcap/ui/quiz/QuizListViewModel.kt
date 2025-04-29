package uz.dckroff.pcap.ui.quiz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.Quiz
import uz.dckroff.pcap.data.model.QuizDifficulty
import uz.dckroff.pcap.data.repository.QuizRepository
import javax.inject.Inject

/**
 * ViewModel для экрана списка тестов
 */
@HiltViewModel
class QuizListViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    // Состояние загрузки тестов
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Список всех тестов
    private val _allQuizzes = MutableLiveData<List<Quiz>>()
    val allQuizzes: LiveData<List<Quiz>> = _allQuizzes

    // Отфильтрованный список тестов
    private val _filteredQuizzes = MutableLiveData<List<Quiz>>()
    val filteredQuizzes: LiveData<List<Quiz>> = _filteredQuizzes

    // Текущий фильтр статуса (все, выполненные, невыполненные)
    private var statusFilter: StatusFilter = StatusFilter.ALL

    // Текущий фильтр сложности
    private var difficultyFilter: QuizDifficulty? = null

    // Состояние ошибки
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadQuizzes()
    }

    /**
     * Загрузить список тестов
     */
    fun loadQuizzes() {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val quizzes = quizRepository.getAllQuizzes()
                _allQuizzes.value = quizzes
                applyFilters() // Применяем текущие фильтры
                Timber.tag("TAG").d("Загружено " + quizzes.size + " тестов")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке тестов")
                _error.value = "Не удалось загрузить тесты: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Установить фильтр по статусу
     */
    fun setStatusFilter(filter: StatusFilter) {
        statusFilter = filter
        applyFilters()
    }

    /**
     * Установить фильтр по сложности
     */
    fun setDifficultyFilter(difficulty: QuizDifficulty?) {
        difficultyFilter = difficulty
        applyFilters()
    }

    /**
     * Применить фильтры к списку тестов
     */
    private fun applyFilters() {
        val allQuizzes = _allQuizzes.value ?: return
        var filtered = allQuizzes

        // Применяем фильтр по статусу
        filtered = when (statusFilter) {
            StatusFilter.ALL -> filtered
            StatusFilter.COMPLETED -> filtered.filter { it.isCompleted }
            StatusFilter.PENDING -> filtered.filter { !it.isCompleted }
        }

        // Применяем фильтр по сложности
        difficultyFilter?.let { difficulty ->
            filtered = filtered.filter { it.difficulty == difficulty }
        }

        _filteredQuizzes.value = filtered
    }

    /**
     * Сбросить все фильтры
     */
    fun resetFilters() {
        statusFilter = StatusFilter.ALL
        difficultyFilter = null
        applyFilters()
    }

    /**
     * Фильтр по статусу выполнения теста
     */
    enum class StatusFilter {
        ALL,        // Все тесты
        COMPLETED,  // Выполненные тесты
        PENDING     // Невыполненные тесты
    }
} 