package uz.dckroff.pcap.ui.reading

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.data.repository.ContentRepository
import uz.dckroff.pcap.data.repository.UserProgressRepository
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    // Список всех разделов
    private val _allSections = MutableLiveData<List<ContentItem.Section>>()
    val allSections: LiveData<List<ContentItem.Section>> = _allSections

    // Текущий раздел
    private val _currentSection = MutableLiveData<ContentItem.Section>()
    val currentSection: LiveData<ContentItem.Section> = _currentSection

    // Контент текущего раздела
    private val _sectionContent = MutableLiveData<List<uz.dckroff.pcap.data.model.SectionContent>>()
    val sectionContent: LiveData<List<uz.dckroff.pcap.data.model.SectionContent>> = _sectionContent

    // Прогресс чтения текущего раздела (0-100%)
    private val _readingProgress = MutableLiveData<Int>()
    val readingProgress: LiveData<Int> = _readingProgress

    // Позиция скролла для восстановления
    private val _savedScrollPosition = MutableLiveData<Int>()
    val savedScrollPosition: LiveData<Int> = _savedScrollPosition

    // Инициализация без загрузки разделов, так как chapterId еще не доступен
    init {
        Timber.tag("PCAP_READING").d("Инициализация ReadingViewModel")
    }

    /**
     * Загрузить все разделы для указанной главы
     */
    private fun loadAllSections(chapterId: String) {
        viewModelScope.launch {
            try {
                val sections = contentRepository.getSectionsForChapter(chapterId).first()
                _allSections.postValue(sections)

                // Обновляем общее количество разделов в репозитории прогресса
                userProgressRepository.updateTotalSections(sections.size)

                Timber.tag("PCAP_READING")
                    .d("Загружено ${sections.size} разделов для главы $chapterId")
            } catch (e: Exception) {
                Timber.tag("PCAP_READING").e(e, "Ошибка при загрузке разделов для главы $chapterId")
            }
        }
    }

    /**
     * Загрузить информацию о разделе по ID
     */
    fun loadSection(chapterId: String, sectionId: String) {
        Timber.tag("PCAP_READING").d("Загрузка раздела с ID: $sectionId, глава: $chapterId")

        if (sectionId.isEmpty() || chapterId.isEmpty()) {
            Timber.tag("PCAP_READING").e("Ошибка: пустой ID раздела или главы")
            return
        }

        // Загружаем все разделы для этой главы
        loadAllSections(chapterId)

        viewModelScope.launch {
            try {
                // Получаем все разделы из этой главы
                val sections = contentRepository.getSectionsForChapter(chapterId).first()
                sections.forEach { Timber.e("Раздел ${it.id + 1}: ${it.title}") }

                // Находим нужный раздел
                val section = sections.find { it.id == sectionId }
                if (section == null) {
                    Timber.tag("PCAP_READING").e("Раздел не найден: $sectionId")
                    return@launch
                }

                // Обновляем LiveData с информацией о разделе
                _currentSection.postValue(section!!)

                // Загружаем контент раздела
                val content = contentRepository.getSectionContent(chapterId, sectionId)
                _sectionContent.postValue(content)

                Timber.tag("PCAP_READING")
                    .d("Загружен раздел: ${section.title} с ${content.size} элементами контента")

                // Восстанавливаем позицию скролла
                restoreReadingPosition(chapterId)

                // Обновляем прогресс чтения - 30% при открытии
                updateReadingProgress(sectionId, 30)
            } catch (e: Exception) {
                Timber.tag("PCAP_READING").e(e, "Ошибка при загрузке раздела $sectionId")
            }
        }
    }

    /**
     * Обновляет прогресс чтения раздела
     */
    fun updateReadingProgress(sectionId: String, progress: Int) {
        _readingProgress.value = progress

        // Обновляем прогресс в репозитории
        viewModelScope.launch {
            userProgressRepository.updateSectionProgress(sectionId, progress)
        }
    }

    /**
     * Сохраняет текущую позицию чтения
     */
    fun saveReadingPosition(chapterId: String, sectionId: String, scrollPosition: Int) {
        viewModelScope.launch {
            userProgressRepository.saveReadingPosition(chapterId, sectionId, scrollPosition)
            Timber.d("Сохранена позиция чтения для раздела $sectionId: $scrollPosition")
        }
    }

    /**
     * Восстанавливает последнюю позицию чтения
     */
    private fun restoreReadingPosition(chapterId: String) {
        viewModelScope.launch {
            try {
                val position = userProgressRepository.getLastReadPosition(chapterId)
                _savedScrollPosition.postValue(position)
                Timber.d("Восстановлена позиция скролла: $position")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при восстановлении позиции чтения")
            }
        }
    }

    /**
     * Получить следующий раздел
     */
    fun getNextSection(currentSectionId: String): ContentItem.Section? {
        Timber.tag("PCAP_READING").d("Поиск следующего раздела после: $currentSectionId")

        val sections = _allSections.value
        Timber.d("section is null: ${sections.isNullOrEmpty()}")
        if (sections.isNullOrEmpty()) {
            Timber.tag("PCAP_READING").d("Нет доступных разделов для поиска следующего")
            return null
        }

        val currentIndex = sections.indexOfFirst { it.id == currentSectionId }
        Timber.tag("PCAP_READING").d("Текущий индекс раздела: $currentIndex из ${sections.size}")

        if (currentIndex >= 0 && currentIndex < sections.size - 1) {
            val nextSection = sections[currentIndex + 1]
            Timber.tag("PCAP_READING").d("Найден следующий раздел: ${nextSection.title}")
            return nextSection
        }

        Timber.tag("PCAP_READING").d("Следующий раздел не найден")
        return null
    }

    /**
     * Получить предыдущий раздел
     */
    fun getPreviousSection(currentSectionId: String): ContentItem.Section? {
        Timber.tag("PCAP_READING").d("Поиск предыдущего раздела перед: $currentSectionId")

        val sections = _allSections.value
        if (sections.isNullOrEmpty()) {
            Timber.tag("PCAP_READING").d("Нет доступных разделов для поиска предыдущего")
            return null
        }

        val currentIndex = sections.indexOfFirst { it.id == currentSectionId }
        Timber.tag("PCAP_READING").d("Текущий индекс раздела: $currentIndex из ${sections.size}")

        if (currentIndex > 0) {
            val prevSection = sections[currentIndex - 1]
            Timber.tag("PCAP_READING").d("Найден предыдущий раздел: ${prevSection.title}")
            return prevSection
        }

        Timber.tag("PCAP_READING").d("Предыдущий раздел не найден")
        return null
    }

    /**
     * Отметить раздел как прочитанный (100% прогресса)
     */
    fun markSectionAsRead(sectionId: String) {
        viewModelScope.launch {
            try {
                // Получаем текущий раздел
                val currentSection = _currentSection.value
                if (currentSection != null) {
                    // Сначала установим прогресс на 100% в UI
                    _readingProgress.postValue(100)
                    
                    // Используем метод для отметки раздела как прочитанного
                    userProgressRepository.markSectionAsRead(sectionId, currentSection.chapterId)
                    Timber.d("Раздел $sectionId отмечен как прочитанный")
                } else {
                    Timber.e("Невозможно отметить раздел как прочитанный: текущий раздел не определен")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при отметке раздела как прочитанного: $sectionId")
            }
        }
    }

    /**
     * Обновить прогресс главы на основе прогресса её разделов
     */
    private suspend fun updateChapterProgress(sectionId: String) {
        try {
            val section = _allSections.value?.find { it.id == sectionId }
            if (section != null) {
                // Текущая реализация оставлена пустой, так как в предыдущем коде функционал был закомментирован
                // TODO: Реализация будет добавлена после уточнения структуры данных и бизнес-логики
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении прогресса главы для раздела $sectionId")
        }
    }
} 