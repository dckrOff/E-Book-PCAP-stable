package uz.dckroff.pcap.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uz.dckroff.pcap.data.local.dao.ChapterDao
import uz.dckroff.pcap.data.local.entity.ChapterEntity
import uz.dckroff.pcap.data.local.entity.RecentChapterEntity
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.features.dashboard.Chapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Репозиторий для работы с содержимым учебника
 */
@Singleton
class ContentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chapterDao: ChapterDao
) {
    /**
     * Получить все главы/разделы учебника
     */
    fun getChapters(): Flow<List<Chapter>> {
        refreshChaptersFromFirestore()
        return chapterDao.getAllChapters().map { entities ->
            entities.map { it.toChapter() }
        }
    }

    /**
     * Получить недавно просмотренные главы
     */
    fun getRecentChapters(limit: Int = 5): Flow<List<Chapter>> {
        return chapterDao.getRecentChapters(limit).map { entities ->
            entities.map { it.toChapter() }
        }
    }

    /**
     * Получить рекомендуемые главы для изучения
     */
    fun getRecommendedChapters(limit: Int = 3): Flow<List<Chapter>> {
        return chapterDao.getRecommendedChapters(limit).map { entities ->
            entities.map { it.toChapter() }
        }
    }

    /**
     * Получить полное содержание учебника с главами и разделами
     */
    suspend fun getContentStructure(): Flow<List<ContentItem.Chapter>> = flow {
        try {
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
                        progress = sectionDoc.getLong("progress")?.toInt() ?: 0,
                        contentUrl = sectionDoc.getString("contentUrl"),
                        number = ""
                    )
                }

                ContentItem.Chapter(
                    id = chapterId,
                    title = doc.getString("title") ?: "",
                    order = doc.getLong("order")?.toInt() ?: 0,
                    progress = doc.getLong("progress")?.toInt() ?: 0,
                    sections = sections,
                    number = doc.getLong("order")?.toInt() ?: 0,
                    description = doc.getString("description") ?: ""
                )
            }

            emit(chapters)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching content structure from Firestore")
            emit(emptyList())
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
    private fun refreshChaptersFromFirestore() {
        try {
            firestore.collection("chapters")
                .orderBy("order")
                .get()
                .addOnSuccessListener { snapshot ->
                    val chapterEntities = snapshot.documents.map { document ->
                        ChapterEntity(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            description = document.getString("description") ?: "",
                            progress = document.getLong("progress")?.toInt() ?: 0,
                            order = document.getLong("order")?.toInt() ?: 0,
                            parentId = document.getString("parentId")
                        )
                    }

                    // Сохраняем данные в локальную базу данных
                    GlobalScope.launch {
                        chapterDao.replaceAllChapters(chapterEntities)
                    }
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Error fetching chapters from Firestore")
                }
        } catch (e: Exception) {
            Timber.e(e, "Error in refreshChaptersFromFirestore")
        }
    }

    /**
     * Получить временные (демонстрационные) данные для содержания
     */
    fun getDummyContentStructure(): List<ContentItem.Chapter> {
        Timber
        val chapters = mutableListOf<ContentItem.Chapter>()

        // Глава 1
        val chapter1 = ContentItem.Chapter(
            id = "ch1",
            title = "Глава 1. Введение в параллельные вычисления",
            description = "Основные понятия и принципы параллельных вычислений",
            order = 1,
            progress = 80,
            number = 1,
            hasSubchapters = true,
            sections = listOf(
                ContentItem.Section(
                    id = "1.1",
                    chapterId = "ch1",
                    title = "1.1 История развития параллельных вычислений",
                    order = 1,
                    progress = 100,
                    number = "1.1"
                ),
                ContentItem.Section(
                    id = "1.2",
                    chapterId = "ch1",
                    title = "1.2 Основные понятия параллельных вычислений",
                    order = 2,
                    progress = 100,
                    number = "1.2"
                ),
                ContentItem.Section(
                    id = "1.3",
                    chapterId = "ch1",
                    title = "1.3 Законы Амдала и Густафсона",
                    order = 3,
                    progress = 50,
                    number = "1.3"
                )
            )
        )

        // Глава 2
        val chapter2 = ContentItem.Chapter(
            id = "ch2",
            title = "Глава 2. Архитектура параллельных вычислительных систем",
            description = "Основные понятия и принципы параллельных вычислений",
            order = 2,
            progress = 40,
            number = 2,
            hasSubchapters = true,
            sections = listOf(
                ContentItem.Section(
                    id = "ch2_sec1",
                    chapterId = "ch2",
                    title = "2.1 Классификация вычислительных систем",
                    order = 1,
                    progress = 100,
                    number = "2.1"
                ),
                ContentItem.Section(
                    id = "ch2_sec2",
                    chapterId = "ch2",
                    title = "2.2 Многоядерные процессоры",
                    order = 2,
                    progress = 50,
                    number = "2.2"
                ),
                ContentItem.Section(
                    id = "ch2_sec3",
                    chapterId = "ch2",
                    title = "2.3 Системы с общей памятью",
                    order = 3,
                    progress = 0,
                    number = "2.3"
                ),
                ContentItem.Section(
                    id = "ch2_sec4",
                    chapterId = "ch2",
                    title = "2.4 Системы с распределенной памятью",
                    order = 4,
                    progress = 0,
                    number = "2.4"
                )
            )
        )

        // Глава 3
        val chapter3 = ContentItem.Chapter(
            id = "ch3",
            title = "Глава 3. Модели параллельного программирования",
            order = 3,
            progress = 10,
            number = 3,
            hasSubchapters = true,
            sections = listOf(
                ContentItem.Section(
                    id = "ch3_sec1",
                    chapterId = "ch3",
                    title = "3.1 Модель передачи сообщений (MPI)",
                    order = 1,
                    progress = 50,
                    number = "3.1"
                ),
                ContentItem.Section(
                    id = "ch3_sec2",
                    chapterId = "ch3",
                    title = "3.2 Модель общей памяти (OpenMP)",
                    order = 2,
                    progress = 0,
                    number = "3.2"
                ),
                ContentItem.Section(
                    id = "ch3_sec3",
                    chapterId = "ch3",
                    title = "3.3 Гибридная модель",
                    order = 3,
                    progress = 0,
                    number = "3.3"
                )
            )
        )

        // Глава 4
        val chapter4 = ContentItem.Chapter(
            id = "ch4",
            title = "Глава 4. Параллельные алгоритмы",
            order = 4,
            progress = 0,
            number = 4,
            hasSubchapters = true,
            sections = listOf(
                ContentItem.Section(
                    id = "ch4_sec1",
                    chapterId = "ch4",
                    title = "4.1 Параллельное умножение матриц",
                    order = 1,
                    progress = 0,
                    number = "4.1"
                ),
                ContentItem.Section(
                    id = "ch4_sec2",
                    chapterId = "ch4",
                    title = "4.2 Параллельная сортировка",
                    order = 2,
                    progress = 0,
                    number = "4.2"
                ),
                ContentItem.Section(
                    id = "ch4_sec3",
                    chapterId = "ch4",
                    title = "4.3 Параллельные графовые алгоритмы",
                    order = 3,
                    progress = 0,
                    number = "4.3"
                )
            )
        )

        chapters.add(chapter1)
        chapters.add(chapter2)
        chapters.add(chapter3)
        chapters.add(chapter4)

        return chapters
    }
} 