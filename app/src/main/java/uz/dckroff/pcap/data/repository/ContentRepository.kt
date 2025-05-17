package uz.dckroff.pcap.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uz.dckroff.pcap.data.local.dao.ChapterDao
import uz.dckroff.pcap.data.local.dao.SectionDao
import uz.dckroff.pcap.data.local.entity.ChapterEntity
import uz.dckroff.pcap.data.local.entity.RecentChapterEntity
import uz.dckroff.pcap.data.local.entity.SectionEntity
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.data.model.SectionContent
import uz.dckroff.pcap.ui.dashboard.Chapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/**
 * Репозиторий для работы с содержимым учебника
 */
@Singleton
class ContentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chapterDao: ChapterDao,
    private val sectionDao: SectionDao
) {
    /**
     * Получить все главы/разделы учебника
     */
    suspend fun getChapters(): Flow<List<Chapter>> {
        Timber.d("getChapters():")
        refreshChaptersFromFirestore()
        Timber.d("кол-во глав из бд: ${chapterDao.getAllChapters().first().size}")
        return chapterDao.getAllChapters().map { entities ->
            entities.map { it.toChapter() }
        }
    }

    /**
     * Получить недавно просмотренные главы
     */
    fun getRecentChapters(): Flow<List<Chapter>> {
        return chapterDao.getRecentChapters().map { entities ->
            entities.map { it.toChapter() }
        }
    }

    /**
     * Получить все главы для изучения
     */
    fun getAllChapters(): Flow<List<Chapter>> {
        return chapterDao.getAllChapters().map { entities ->
            entities.map { it.toChapter() }
        }
    }

    /**
     * Получить рекомендуемые главы для изучения
     */
    fun getRecommendedChapters(limit: Int = 5): Flow<List<Chapter>> {
        return chapterDao.getRecommendedChapters(limit).map { entities ->
            entities.map { it.toChapter() }
        }
    }

    private fun ChapterEntity.toChapter(): Chapter {
        return Chapter(
            id = this.id,
            title = this.title,
            description = this.description,
            order = this.order,
            progress = this.progress,
        )
    }

    /**
     * Получить полное содержание учебника с главами и разделами
     */
    fun getContentStructure(): Flow<List<ContentItem.Chapter>> = flow {
        val chaptersCollection = firestore.collection("chapters")
            .orderBy("order")
            .get()
            .await()

        val chapters = chaptersCollection.documents.map { doc ->
            val chapterId = doc.id

            // Получаем разделы для этой главы
            val sectionsCollection = firestore.collection("chapters")
                .document(chapterId)
                .collection("sections")
                .orderBy("order")
                .get()
                .await()

            val sections = sectionsCollection.documents.map { sectionDoc ->
                ContentItem.Section(
                    id = sectionDoc.id,
                    chapterId = chapterId,
                    title = sectionDoc.getString("title") ?: "",
                    order = sectionDoc.getLong("order")?.toInt() ?: 0,
                    number = sectionDoc.getLong("order")?.toInt() ?: 0,
                    progress = sectionDoc.getLong("progress")?.toInt() ?: 0,
                    contentUrl = sectionDoc.getString("contentUrl")
                )
            }

            ContentItem.Chapter(
                id = chapterId,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                order = doc.getLong("order")?.toInt() ?: 0,
                sectionsCount = doc.getLong("sectionsCount")?.toInt() ?: sections.size,
                sections = sections,
                progress = 0
            )
        }

        emit(chapters)
    }.catch { e ->
        Timber.e(e, "Error fetching content structure from Firestore")
        emit(emptyList())
    }

    /**
     * Получить разделы главы
     */
    fun getSectionsForChapter(chapterId: String): Flow<List<ContentItem.Section>> = flow {
        val sectionsCollection = firestore.collection("chapters")
            .document(chapterId)
            .collection("sections")
            .orderBy("order")
            .get()
            .await()

        val sections = sectionsCollection.documents.map { sectionDoc ->
            ContentItem.Section(
                id = sectionDoc.id,
                chapterId = chapterId,
                title = sectionDoc.getString("title") ?: "",
                description = sectionDoc.getString("description") ?: "",
                order = sectionDoc.getLong("order")?.toInt() ?: 0,
                number = sectionDoc.getLong("order")?.toInt() ?: 0,
                progress = sectionDoc.getLong("progress")?.toInt() ?: 0,
                contentUrl = sectionDoc.getString("contentUrl")
            )
        }
        
        // Сохраняем полученные разделы в базу данных
        saveSectionsToDatabase(sections)

        emit(sections)
    }.catch { e ->
        Timber.e(e, "Error fetching sections for chapterId=$chapterId")
        emit(emptyList())
    }


    /**
     * Получить контент раздела
     */
    suspend fun getSectionContent(chapterId: String, sectionId: String): List<SectionContent> {
        return try {
            val contentDoc = firestore.collection("content")
                .document(chapterId)
                .collection("sections")
                .document(sectionId)
                .get()
                .await()

            if (!contentDoc.exists()) {
                return emptyList()
            }

            val contentArray =
                contentDoc.get("content") as? List<Map<String, Any>> ?: return emptyList()

            contentArray.mapNotNull { item ->
                val id = item["id"] as? String ?: return@mapNotNull null
                val type = item["type"] as? String ?: return@mapNotNull null

                when (type) {
                    "text" -> {
                        val content = item["content"] as? String ?: ""
                        val isHighlighted = item["isHighlighted"] as? Boolean ?: false
                        SectionContent.Text(id, content, isHighlighted)
                    }

                    "code" -> {
                        val content = item["content"] as? String ?: ""
                        val language = item["language"] as? String ?: ""
                        val caption = item["caption"] as? String ?: ""
                        SectionContent.Code(id, content, language, caption)
                    }

                    "formula" -> {
                        val content = item["content"] as? String ?: ""
                        val caption = item["caption"] as? String ?: ""
                        val isInline = item["isInline"] as? Boolean ?: false
                        SectionContent.Formula(id, content, caption, isInline)
                    }

                    "table" -> {
                        val headers = (item["headers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                        val rows = (item["rows"] as? List<*>)?.mapNotNull { rowItem ->
                            when (rowItem) {
                                is Map<*, *> -> {
                                    val cells = rowItem["cells"] as? List<*> ?: return@mapNotNull null
                                    cells.mapNotNull { it as? String }
                                }
                                is List<*> -> {
                                    // поддержка старого формата, если он вдруг остался
                                    rowItem.mapNotNull { it as? String }
                                }
                                else -> null
                            }
                        } ?: emptyList()

                        val caption = item["caption"] as? String ?: ""
                        SectionContent.Table(id, headers, rows, caption)
                    }


                    "image" -> {
                        val url = item["url"] as? String ?: ""
                        val caption = item["caption"] as? String ?: ""
                        val description = item["description"] as? String ?: ""
                        SectionContent.Image(id, url, caption, description)
                    }

                    "video" -> {
                        val url = item["url"] as? String ?: ""
                        val caption = item["caption"] as? String ?: ""
                        SectionContent.Video(id, url, caption)
                    }

                    "diagram" -> {
                        val elementsData = item["elements"] as? List<Map<String, Any>> ?: emptyList()
                        val elements = elementsData.map { elementData ->
                            val elementId = elementData["id"] as? String ?: ""
                            val elementType = elementData["type"] as? String ?: ""
                            val text = elementData["text"] as? String ?: ""
                            val x = (elementData["x"] as? Number)?.toFloat() ?: 0f
                            val y = (elementData["y"] as? Number)?.toFloat() ?: 0f
                            val width = (elementData["width"] as? Number)?.toFloat() ?: 0f
                            val height = (elementData["height"] as? Number)?.toFloat() ?: 0f
                            val connections = (elementData["connections"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            
                            uz.dckroff.pcap.data.model.DiagramElement(
                                id = elementId,
                                type = elementType,
                                text = text,
                                x = x,
                                y = y,
                                width = width,
                                height = height,
                                connections = connections
                            )
                        }
                        val caption = item["caption"] as? String ?: ""
                        SectionContent.Diagram(id, elements, caption)
                    }

                    else -> null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching section content from Firestore")
            emptyList()
        }
    }

    /**
     * Обновить недавно просмотренную главу
     */
    suspend fun updateRecentChapter(chapterId: String) {
        chapterDao.insertRecentChapter(RecentChapterEntity(chapterId))
    }

    /**
     * Обновить данные глав из Firebase Firestore
     */
    suspend fun refreshChaptersFromFirestore() {
        try {
            val snapshot = firestore.collection("chapters")
                .orderBy("order")
                .get()
                .await()

            Timber.d("Получено ${snapshot.documents.size} глав из Firebase")
            val chapterEntities = snapshot.documents.map { document ->
                ChapterEntity(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    description = document.getString("description") ?: "",
                    progress = 0,
                    order = document.getLong("order")?.toInt() ?: 0,
                )
            }

            // Логируем данные
            Timber.d("Преобразовано ${chapterEntities.size} глав")

            // Сохраняем данные в локальную базу данных
            chapterDao.replaceAllChapters(chapterEntities)
            val chaptersCount = chapterDao.getChaptersCount() // TODO delete after testing
            Timber.d("Добавлено ${chaptersCount} глав в базу данных")

        } catch (e: Exception) {
            Timber.e(e, "Error in refreshChaptersFromFirestore")
            throw e
        }
    }

    /**
     * Конвертировать модель раздела в сущность для базы данных
     */
    private fun mapToSectionEntity(section: ContentItem.Section): SectionEntity {
        return SectionEntity(
            id = section.id,
            chapterId = section.chapterId,
            title = section.title,
            description = section.description,
            order = section.order,
            number = section.number,
            contentUrl = section.contentUrl,
            hasSectionRead = false
        )
    }
    
    /**
     * Сохранить список разделов в базу данных
     */
    suspend fun saveSectionsToDatabase(sections: List<ContentItem.Section>) {
        try {
            val sectionEntities = sections.map { mapToSectionEntity(it) }
            sectionDao.insertSections(sectionEntities)
            Timber.d("Сохранено ${sectionEntities.size} разделов в базу данных")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сохранении разделов в базу данных")
        }
    }
} 