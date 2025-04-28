package uz.dckroff.pcap.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import timber.log.Timber
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.data.repository.ContentRepository
import uz.dckroff.pcap.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с содержанием учебника
 */
@Singleton
class BookRepository @Inject constructor(
    private val contentRepository: ContentRepository
) {
    /**
     * Получить содержание учебника
     * @param forceRefresh Принудительное обновление данных
     * @return [Resource] с результатом операции
     */
    suspend fun getContent(
        chapterId: String,
        forceRefresh: Boolean = false
    ): Resource<List<ContentItem.Section>> {
        return try {
            val chapters = contentRepository.getSectionsForChapter(chapterId = chapterId).first()

            // Преобразуем данные из ContentRepository к формату ContentItem
            val contentItems = chapters.map { section ->
                ContentItem.Section(
                    id = section.id,
                    chapterId = section.chapterId,
                    title = section.title,
                    description = section.description,
                    order = section.order,
                    number = section.number,
                    progress = section.progress,
                    contentUrl = section.contentUrl
                )
            }

            Resource.Success(contentItems)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении содержания учебника")
            Resource.Error("Не удалось загрузить содержание: ${e.message}")
        }
    }
} 