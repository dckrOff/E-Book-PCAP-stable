package uz.dckroff.pcap.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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

    // Статистика прогресса: пройдено/всего разделов
    private val _progressStats = MutableLiveData<Pair<Int, Int>>()
    val progressStats: LiveData<Pair<Int, Int>> = _progressStats

    // Недавно просмотренные главы
    private val _recentChapters = MutableLiveData<List<Chapter>>()
    val recentChapters: LiveData<List<Chapter>> = _recentChapters

    // Рекомендуемые главы
    private val _allChapters = MutableLiveData<List<Chapter>>()
    val allChapters: LiveData<List<Chapter>> = _allChapters

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
        Log.e("TAG", "Загрузка данных при инициализации ViewModel")
        // Загрузка данных при инициализации ViewModel
        loadDashboardData()
    }

    /**
     * Загрузка данных для главного экрана
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                Timber.d("Загрузка данных для главного экрана...")
                _isLoading.value = true

                coroutineScope {
                    // Загружаем общий прогресс обучения
                    val overallProgressDeferred = async {
                        userProgressRepository.getOverallProgress()
                            .catch { e ->
                                Timber.e(e, "Error loading overall progress")
                                _error.value = e.message
                            }
                            .firstOrNull()
                    }

                    // Загружаем статистику по завершенным разделам
                    val progressStatsDeferred = async {
                        val userProgress = userProgressRepository.getUserProgress().first()
                        Pair(userProgress?.completedSections ?: 0, userProgress?.totalSections ?: 0)
                    }

                    // Загружаем недавно просмотренные главы
                    val recentChaptersDeferred = async {
                        contentRepository.getRecentChapters()
                            .catch { e ->
                                Timber.e(e, "Error loading recent chapters")
                                _error.value = e.message
                            }
                            .firstOrNull()
                    }

                    // Загружаем все главы
                    val allChaptersDeferred = async {
                        contentRepository.getChapters()
                            .catch { e ->
                                Timber.e(e, "Error loading recommended chapters")
                                _error.value = e.message
                            }
                            .firstOrNull()
                    }

                    // Ждём все результаты
                    _overallProgress.value = overallProgressDeferred.await() ?: 0
                    _progressStats.value = progressStatsDeferred.await()
                    Timber.d("Получение прогресса: ${_overallProgress.value}%, пройдено ${_progressStats.value?.first ?: 0} из ${_progressStats.value?.second ?: 0} разделов")

                    // Обрабатываем результаты загрузки глав
                    val recentChapters = recentChaptersDeferred.await() ?: emptyList()
                    val allChapters = allChaptersDeferred.await() ?: emptyList()

                    // Обновляем данные с учетом прогресса чтения для каждой главы
                    updateChaptersWithReadingProgress(recentChapters, allChapters)

                    Timber.d("Загружены последние главы: ${_recentChapters.value?.size} шт.")
                    Timber.d("Загружено всех глав: ${_allChapters.value?.size} шт.")
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
     * Обновляем данные глав с учетом прогресса чтения
     */
    private suspend fun updateChaptersWithReadingProgress(
        recentChapters: List<Chapter>,
        allChapters: List<Chapter>
    ) {
        try {
            // Получаем прогресс чтения для каждой главы
            val updatedRecentChapters = recentChapters.map { chapter ->
                val progress = userProgressRepository.getChapterProgress(chapter.id)
                Timber.d("Получен прогресс для главы ${chapter.id}: $progress%")
                chapter.copy(progress = progress)
            }

            val updatedAllChapters = allChapters.map { chapter ->
                val progress = userProgressRepository.getChapterProgress(chapter.id)
                Timber.d("Получен прогресс для главы ${chapter.id}: $progress%")
                chapter.copy(progress = progress)
            }

            // Обновляем LiveData
            _recentChapters.value = updatedRecentChapters
            _allChapters.value = updatedAllChapters
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении прогресса чтения глав")
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

    /**
     * Обновить данные из Firebase
     */
    fun refreshDataFromFirebase() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Принудительно обновляем главы
                contentRepository.refreshChaptersFromFirestore()

                // Загружаем данные снова после обновления
                loadDashboardData()

                _error.value = "Данные успешно обновлены"
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Ошибка при обновлении данных: ${e.message}"
                Timber.e(e, "Error refreshing data from Firebase")
            }
        }
    }

    /**
     * Обновить данные при возвращении с экрана чтения
     * Этот метод должен вызываться в onResume фрагмента
     */
    fun refreshOnReturn() {
        Timber.d("Обновление данных после возвращения с экрана чтения")
        loadDashboardData()
    }
} 