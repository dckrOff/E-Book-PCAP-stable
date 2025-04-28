package uz.dckroff.pcap.features.reading

import android.util.Log
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

                // Обновляем прогресс чтения
//                userProgressRepository.updateReadingProgress(sectionId, 50) // 50% при открытии

            } catch (e: Exception) {
                Timber.tag("PCAP_READING").e(e, "Ошибка при загрузке раздела $sectionId")
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
                userProgressRepository.updateChapterProgress(sectionId, 100)
                Timber.d("Раздел $sectionId отмечен как прочитанный")

                // Обновляем родительскую главу
                updateChapterProgress(sectionId)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обновлении прогресса для раздела $sectionId")
            }
        }
    }

    /**
     * Обновить прогресс главы на основе прогресса её разделов
     */
    private suspend fun updateChapterProgress(sectionId: String) {
//        try {
//            val section = _allSections.value?.find { it.id == sectionId } ?: return
//            val chapterId = section.chapterId
//
//            // Получаем все разделы главы
//            val chaptersWithSections = contentRepository.getDummyContentStructure()
//            val chapter = chaptersWithSections.find { it.id == chapterId } ?: return
//            val sectionsOfChapter = chapter.sections
//
//            // Вычисляем общий прогресс главы как средний прогресс всех её разделов
//            val totalProgress = sectionsOfChapter.sumOf { it.progress } / sectionsOfChapter.size
//
//            // Обновляем прогресс главы
//            userProgressRepository.updateChapterProgress(chapterId, totalProgress)
//            Timber.d("Обновлен прогресс главы $chapterId: $totalProgress%")
//        } catch (e: Exception) {
//            Timber.e(e, "Ошибка при обновлении прогресса главы для раздела $sectionId")
//        }
    }
} 