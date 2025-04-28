package uz.dckroff.pcap.features.glossary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.GlossaryTerm
import uz.dckroff.pcap.data.repository.BookmarksRepository
import uz.dckroff.pcap.data.repository.GlossaryRepository
import javax.inject.Inject

/**
 * ViewModel для экрана деталей термина глоссария
 */
@HiltViewModel
class GlossaryDetailViewModel @Inject constructor(
    private val glossaryRepository: GlossaryRepository,
    private val bookmarksRepository: BookmarksRepository
) : ViewModel() {

    // Данные о термине
    private val _term = MutableLiveData<GlossaryTerm?>()
    val term: MutableLiveData<GlossaryTerm?> = _term

    // Состояние загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Сообщение об ошибке
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Навигация к разделу
    private val _navigateToSection = MutableLiveData<String?>()
    val navigateToSection: LiveData<String?> = _navigateToSection

    // Навигация к связанному термину
    private val _navigateToRelatedTerm = MutableLiveData<String?>()
    val navigateToRelatedTerm: LiveData<String?> = _navigateToRelatedTerm

    // Статус закладки
    private val _isBookmarked = MutableLiveData<Boolean>()
    val isBookmarked: LiveData<Boolean> = _isBookmarked

    private var currentTermId: String? = null

    /**
     * Загружает детали термина по его идентификатору
     */
    fun loadTermDetails(termId: String) {
        currentTermId = termId
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val result = glossaryRepository.getTermById(termId)
                _term.value = result
                checkBookmarkStatus(termId)
                _isLoading.value = false
            } catch (e: Exception) {
                Timber.e(e, "Error loading term details for ID: $termId")
                _errorMessage.value = "Ошибка загрузки термина: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Проверяет, добавлен ли термин в закладки
     */
    private fun checkBookmarkStatus(termId: String) {
        viewModelScope.launch {
            try {
                val isInBookmarks = bookmarksRepository.isTermBookmarked(termId)
                _isBookmarked.value = isInBookmarks
            } catch (e: Exception) {
                Timber.e(e, "Error checking bookmark status for term ID: $termId")
                // Не показываем ошибку пользователю, просто предполагаем, что термин не в закладках
                _isBookmarked.value = false
            }
        }
    }

    /**
     * Переключает статус закладки для текущего термина
     */
    fun toggleBookmark() {
        val termId = currentTermId ?: return
        val term = _term.value ?: return
        val currentBookmarkState = _isBookmarked.value ?: false

        viewModelScope.launch {
            try {
                if (currentBookmarkState) {
                    bookmarksRepository.removeTermFromBookmarks(termId)
                } else {
                    bookmarksRepository.addTermToBookmarks(term)
                }
                _isBookmarked.value = !currentBookmarkState
            } catch (e: Exception) {
                Timber.e(e, "Error toggling bookmark status for term ID: $termId")
                _errorMessage.value = "Ошибка при изменении закладки: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Обрабатывает клик по связанному разделу
     */
    fun onRelatedSectionClicked(sectionId: String) {
        _navigateToSection.value = sectionId
    }

    /**
     * Обрабатывает клик по связанному термину
     */
    fun onRelatedTermClicked(relatedTerm: String) {
        viewModelScope.launch {
            try {
                // Поиск ID термина по его названию
                val terms = glossaryRepository.searchTerms(relatedTerm)
                val matchingTerm = terms.find { it.term == relatedTerm }
                
                if (matchingTerm != null) {
                    _navigateToRelatedTerm.value = matchingTerm.id
                    Timber.d("Найден связанный термин: ${matchingTerm.term} с ID: ${matchingTerm.id}")
                } else {
                    Timber.e("Не удалось найти ID для связанного термина: $relatedTerm")
                    // В реальном приложении здесь можно показать сообщение пользователю
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске связанного термина")
            }
        }
    }

    /**
     * Сбрасывает флаг навигации после обработки
     */
    fun navigationHandled() {
        _navigateToSection.value = null
        _navigateToRelatedTerm.value = null
    }
} 