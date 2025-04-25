package uz.dckroff.pcap.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    suspend fun getContent(forceRefresh: Boolean = false): Resource<List<ContentItem.Chapter>> {
        return try {
            // Для демонстрации используем временные данные из ContentRepository
            // В реальном приложении здесь будет логика обращения к API или кэшу
            val chapters = contentRepository.getDummyContentStructure()

            // Преобразуем данные из ContentRepository к формату ContentItem
            val contentItems = chapters.map { chapter ->
                ContentItem.Chapter(
                    id = chapter.id,
                    title = chapter.title.substringAfter("Глава ${chapter.number}. ").trim(),
                    description = chapter.description,
                    order = chapter.order,
                    progress = chapter.progress,
                    number = chapter.number,
                    hasSubchapters = chapter.hasSubchapters,
                    sections = chapter.sections.map { section ->
                        ContentItem.Section(
                            id = section.id,
                            chapterId = section.chapterId,
                            title = section.title.substringAfter("${chapter.number}.${section.order} ").trim(),
                            order = section.order,
                            progress = section.progress,
                            contentUrl = section.contentUrl,
                            number = "${chapter.number}.${section.order}"
                        )
                    }
                )
            }

            Resource.Success(contentItems)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении содержания учебника")
            Resource.Error("Не удалось загрузить содержание: ${e.message}")
        }
    }
} 