package uz.dckroff.pcap.utils

/**
 * Класс для управления состояниями загрузки данных в приложении
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * Состояние успешной загрузки данных
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Состояние ошибки при загрузке данных
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Состояние загрузки данных
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)
} 