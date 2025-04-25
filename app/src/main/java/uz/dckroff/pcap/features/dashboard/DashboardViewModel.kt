package uz.dckroff.pcap.features.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.repository.ContentRepository
import uz.dckroff.pcap.data.repository.UserProgressRepository
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    // Общий прогресс обучения
    private val _overallProgress = MutableLiveData<Int>()
    val overallProgress: LiveData<Int> = _overallProgress

    // Недавно просмотренные главы
    private val _recentChapters = MutableLiveData<List<Chapter>>()
    val recentChapters: LiveData<List<Chapter>> = _recentChapters

    // Рекомендуемые главы
    private val _recommendedChapters = MutableLiveData<List<Chapter>>()
    val AllChapters: LiveData<List<Chapter>> = _recommendedChapters

    // Статус загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Обработка ошибок
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Событие для навигации к чтению главы
    private val _navigateToReading = MutableLiveData<Chapter?>()
    val navigateToReading: LiveData<Chapter?> = _navigateToReading

    init {
        // Загрузка данных при инициализации ViewModel
        loadDashboardData()
    }

    /**
     * Загрузка данных для главного экрана
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Загружаем общий прогресс
                userProgressRepository.getOverallProgress()
                    .catch { e -> 
                        Timber.e(e, "Error loading overall progress")
                        _error.value = e.message
                    }
                    .collectLatest { progress ->
                        _overallProgress.value = progress
                    }

                // Загружаем недавно просмотренные главы
                contentRepository.getRecentChapters()
                    .catch { e -> 
                        Timber.e(e, "Error loading recent chapters")
                        _error.value = e.message
                    }
                    .collectLatest { chapters ->
                        _recentChapters.value = chapters
                    }

                // Загружаем рекомендуемые главы
                contentRepository.getRecommendedChapters()
                    .catch { e -> 
                        Timber.e(e, "Error loading recommended chapters")
                        _error.value = e.message
                    }
                    .collectLatest { chapters ->
                        _recommendedChapters.value = chapters
                    }

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message
                Timber.e(e, "Error loading dashboard data")
            }
        }
    }

    /**
     * Обработка нажатия на кнопку "Продолжить обучение"
     * В реальном приложении здесь будет навигация к последнему просмотренному разделу
     */
    fun onContinueLearningClicked() {
        viewModelScope.launch {
            try {
                val recentChapters = _recentChapters.value
                if (!recentChapters.isNullOrEmpty()) {
                    // В реальной реализации здесь будет переход к последней просмотренной главе
                    val lastChapter = recentChapters.first()
                    Timber.d("Navigate to chapter: ${lastChapter.title}")
                    _navigateToReading.value = lastChapter
                } else {
                    // Если нет недавно просмотренных разделов, перейти к первому разделу
                    Timber.d("No recent chapters, navigate to first chapter")
                }
            } catch (e: Exception) {
                _error.value = e.message
                Timber.e(e, "Error in continue learning")
            }
        }
    }

    /**
     * Обработка нажатия на главу/раздел
     */
    fun onChapterClicked(chapter: Chapter) {
        Timber.d("Chapter clicked: ${chapter.title}")
        
        viewModelScope.launch {
            try {
                // Обновляем недавно просмотренную главу
                contentRepository.updateRecentChapter(chapter.id)
                
                // Инициируем навигацию к экрану содержания
                _navigateToReading.value = chapter
            } catch (e: Exception) {
                _error.value = e.message
                Timber.e(e, "Error while processing chapter click")
            }
        }
    }
    
    /**
     * Метод для сброса события навигации после его обработки
     */
    fun onReadingNavigated() {
        _navigateToReading.value = null
    }

    /**
     * Сброс сообщения об ошибке
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Обновить данные главного экрана
     */
    fun refreshData() {
        loadDashboardData()
    }
} 