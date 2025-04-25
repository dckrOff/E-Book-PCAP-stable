package uz.dckroff.pcap.features.reading

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
        loadAllSections()
    }

    /**
     * Загрузить все разделы
     */
    private fun loadAllSections() {
        viewModelScope.launch {
            try {
                // Получаем демо-данные из репозитория
                val chapters = contentRepository.getDummyContentStructure()
                // Собираем все разделы из всех глав
                val sections = chapters.flatMap { it.sections }
                _allSections.postValue(sections)
                Timber.tag("PCAP_READING").d("Загружено ${sections.size} разделов")
            } catch (e: Exception) {
                Timber.tag("PCAP_READING").e(e, "Ошибка при загрузке разделов")
            }
        }
    }

    /**
     * Загрузить информацию о разделе по ID
     */
    fun loadSection(sectionId: String) {
        Timber.tag("PCAP_READING").d("Загрузка раздела с ID: $sectionId")
        viewModelScope.launch {
            try {
                // Получаем все разделы (если список пуст, загружаем)
                var sections = _allSections.value
                if (sections.isNullOrEmpty()) {
                    Timber.tag("PCAP_READING").d("Список разделов пуст, загружаем все разделы")
                    
                    // Загружаем главы и получаем разделы синхронно, не через LiveData
                    val chapters = contentRepository.getDummyContentStructure()
                    sections = chapters.flatMap { it.sections }
                    
                    // Сохраняем в LiveData для будущего использования
                    _allSections.postValue(sections)
                    
                    Timber.tag("PCAP_READING").d("Загружено ${sections.size} разделов (синхронно)")
                }
                
                if (sections.isEmpty()) {
                    Timber.tag("PCAP_READING").d("Разделы не загружены. Проблема с репозиторием")
                    // Создаем заглушку с сообщением об ошибке
                    _currentSection.postValue(
                        ContentItem.Section(
                            id = sectionId,
                            chapterId = "error",
                            title = "Ошибка загрузки данных",
                            order = 0,
                            progress = 0,
                            contentUrl = null
                        )
                    )
                    return@launch
                }
                
                Timber.tag("PCAP_READING").d("Ищем раздел с ID: $sectionId среди ${sections.size} разделов")
                val section = sections.find { it.id == sectionId }
                
                if (section != null) {
                    Timber.tag("PCAP_READING").d("Найден раздел: ${section.title}")
                    _currentSection.postValue(section)
                } else {
                    Timber.tag("PCAP_READING").d("Раздел не найден как Section, ищем среди глав")
                    // Если не найден как раздел, проверяем, может быть это ID главы
                    val chapters = contentRepository.getDummyContentStructure()
                    val chapter = chapters.find { it.id == sectionId }
                    
                    if (chapter != null && chapter.sections.isNotEmpty()) {
                        Timber.tag("PCAP_READING").d("Найдена глава: ${chapter.title}, берем первый раздел")
                        // Если это глава, берем её первый раздел
                        _currentSection.postValue(chapter.sections.first())
                    } else {
                        Timber.tag("PCAP_READING").d("Раздел или глава с ID: $sectionId не найдены")
                        // Если ничего не найдено, загружаем заглушку
                        _currentSection.postValue(
                            ContentItem.Section(
                                id = sectionId,
                                chapterId = "unknown",
                                title = "Раздел не найден: $sectionId",
                                order = 0,
                                progress = 0,
                                contentUrl = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag("PCAP_READING").e(e, "Ошибка при загрузке раздела $sectionId")
                // Даже в случае ошибки, отправляем заглушку чтобы убрать ProgressBar
                _currentSection.postValue(
                    ContentItem.Section(
                        id = sectionId,
                        chapterId = "error",
                        title = "Ошибка загрузки: ${e.message}",
                        order = 0,
                        progress = 0,
                        contentUrl = null
                    )
                )
            }
        }
    }

    /**
     * Получить следующий раздел
     */
    fun getNextSection(currentSectionId: String): ContentItem.Section? {
        Timber.tag("PCAP_READING").d("Поиск следующего раздела после: $currentSectionId")
        
        val sections = _allSections.value
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
        try {
            val section = _allSections.value?.find { it.id == sectionId } ?: return
            val chapterId = section.chapterId
            
            // Получаем все разделы главы
            val chaptersWithSections = contentRepository.getDummyContentStructure()
            val chapter = chaptersWithSections.find { it.id == chapterId } ?: return
            val sectionsOfChapter = chapter.sections
            
            // Вычисляем общий прогресс главы как средний прогресс всех её разделов
            val totalProgress = sectionsOfChapter.sumOf { it.progress } / sectionsOfChapter.size
            
            // Обновляем прогресс главы
            userProgressRepository.updateChapterProgress(chapterId, totalProgress)
            Timber.d("Обновлен прогресс главы $chapterId: $totalProgress%")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении прогресса главы для раздела $sectionId")
        }
    }
} 