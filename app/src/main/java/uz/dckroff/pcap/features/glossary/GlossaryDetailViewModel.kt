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

/**
 * ViewModel для экрана детальной информации о термине глоссария
 */
@HiltViewModel
class GlossaryDetailViewModel @Inject constructor(
    private val glossaryRepository: GlossaryRepository
) : ViewModel() {

    // Данные о термине
    private val _term = MutableLiveData<GlossaryTerm>()
    val term: LiveData<GlossaryTerm> = _term

    // Состояние загрузки
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Сообщение об ошибке
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Навигация к разделу
    private val _navigateToSection = MutableLiveData<String?>()
    val navigateToSection: LiveData<String?> = _navigateToSection

    // Навигация к связанному термину
    private val _navigateToRelatedTerm = MutableLiveData<String?>()
    val navigateToRelatedTerm: LiveData<String?> = _navigateToRelatedTerm

    /**
     * Загружает детальную информацию о термине по его идентификатору
     */
    fun loadTermDetails(termId: String) {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val result = glossaryRepository.getTermById(termId)
                if (result != null) {
                    _term.value = result
                    Timber.d("Термин загружен успешно: ${result.term}")
                } else {
                    _error.value = "Термин с ID $termId не найден"
                    Timber.e("Термин с ID $termId не найден")
                }
            } catch (e: Exception) {
                _error.value = "Ошибка при загрузке термина: ${e.message}"
                Timber.e(e, "Ошибка при загрузке термина")
            } finally {
                _loading.value = false
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