package uz.dckroff.pcap.utils

/**
 * Класс для представления состояния UI
 * @param T тип данных
 */
sealed class UiState<out T> {
    /**
     * Состояние загрузки данных
     */
    object Loading : UiState<Nothing>()

    /**
     * Состояние успешной загрузки данных
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Состояние ошибки
     */
    data class Error(val message: String) : UiState<Nothing>()

    /**
     * Состояние пустого результата
     */
    object Empty : UiState<Nothing>()
} 