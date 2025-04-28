package uz.dckroff.pcap.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import uz.dckroff.pcap.data.local.dao.ChapterDao
import uz.dckroff.pcap.data.local.dao.SectionDao
import uz.dckroff.pcap.data.local.dao.UserProgressDao
import uz.dckroff.pcap.data.local.entity.RecentChapterEntity
import uz.dckroff.pcap.data.local.entity.UserProgressEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с прогрессом пользователя
 */
@Singleton
class UserProgressRepository @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val chapterDao: ChapterDao,
    private val sectionDao: SectionDao
) {

    // Хранит сет разделов, которые уже были отмечены как прочитанные в текущей сессии
    private val completedSections = mutableSetOf<String>()

    /**
     * Получить общий прогресс пользователя по всем разделам
     */
    fun getOverallProgress(): Flow<Int> {
        return userProgressDao.getUserProgress().map { entity ->
            entity?.overallProgress ?: 0
        }
    }

    /**
     * Получить полную статистику прогресса пользователя
     */
    fun getUserProgress(): Flow<UserProgressEntity?> {
        return userProgressDao.getUserProgress()
    }

    /**
     * Полностью пересчитать общий прогресс на основе прочитанных разделов
     */
    suspend fun resetAndRecalculateCompletedSections() {
        try {
            // Принудительно обновляем общий прогресс
            updateOverallProgress(true)

            Timber.d("Общий прогресс пользователя пересчитан")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при пересчете общего прогресса пользователя")
        }
    }

    /**
     * Получить прогресс чтения для конкретной главы
     */
    suspend fun getChapterProgress(chapterId: String): Int {
        try {
            // Проверяем прогресс в локальной базе
            val localProgress = chapterDao.getChapterById(chapterId)?.progress ?: 0
            return localProgress
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении прогресса главы")
            return 0
        }
    }

    /**
     * Обновить прогресс пользователя по конкретному разделу
     */
    suspend fun updateSectionProgress(sectionId: String, progress: Int) {
        try {
            // Обновляем прогресс в локальной базе данных
            if (progress > 75) {
                userProgressDao.updateSectionReadStatus(sectionId, true)
                
                // Если прогресс больше 75% и раздел еще не в списке выполненных
                if (!completedSections.contains(sectionId)) {
                    // Добавляем в кэш
                    completedSections.add(sectionId)
                    Timber.d("Раздел $sectionId добавлен в кэш как прочитанный (прогресс: $progress%)")
                }
            }

            // Принудительно пересчитываем общий прогресс
            updateOverallProgress(true)

            // Выводим информацию о прогрессе
            Timber.d("Обновлен прогресс раздела $sectionId: $progress%")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении прогресса раздела $sectionId: ${e.message}")
        }
    }

    /**
     * Обновить прогресс пользователя по конкретному разделу
     */
    suspend fun updateChapterProgress(chapterId: String, progress: Int) {
        try {
            // Получаем текущий прогресс раздела
            val currentProgress = getChapterProgress(chapterId)

            // Обновляем прогресс в локальной базе данных
            userProgressDao.updateChapterProgress(chapterId, progress)

            // Добавляем в список недавно просмотренных
            chapterDao.insertRecentChapter(RecentChapterEntity(chapterId))

            // Принудительно пересчитываем общий прогресс
            updateOverallProgress(true)

            // Если раздел завершен (прогресс 100%) и не был завершен ранее,
            // и не является дубликатом в текущей сессии
            if (progress >= 100 && currentProgress < 100 && !completedSections.contains(chapterId)) {
                // Запоминаем этот раздел в сессии
                completedSections.add(chapterId)

                // Делаем полный пересчет вместо инкремента
                resetAndRecalculateCompletedSections()

                Timber.d("Раздел $chapterId отмечен как прочитанный")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении прогресса раздела")
        }
    }

    /**
     * Сохранить позицию чтения в разделе
     */
    suspend fun saveReadingPosition(chapterId: String, sectionId: String, scrollPosition: Int) {
        try {
            val timestamp = System.currentTimeMillis()
            userProgressDao.updateReadingPosition(
                chapterId = chapterId,
                sectionId = sectionId,
                position = scrollPosition,
                timestamp = timestamp
            )

            Timber.d("Сохранена позиция чтения: раздел $sectionId, позиция $scrollPosition")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сохранении позиции чтения")
        }
    }

    /**
     * Получить ID последнего прочитанного раздела для главы
     */
    suspend fun getLastReadSectionId(chapterId: String): String? {
        return try {
            userProgressDao.getLastReadSectionId(chapterId)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении ID последнего прочитанного раздела")
            null
        }
    }

    /**
     * Получить последнюю позицию скролла в разделе
     */
    suspend fun getLastReadPosition(chapterId: String): Int {
        return try {
            userProgressDao.getLastReadPosition(chapterId) ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении последней позиции чтения")
            0
        }
    }

    /**
     * Обновить общий прогресс пользователя на основе прочитанных разделов
     */
    private suspend fun updateOverallProgress(forceUpdate: Boolean = false) {
        try {
            // Получаем количество прочитанных разделов
            val readSectionsCount = sectionDao.getTotalReadSectionsCount()

            // Получаем общее количество разделов
            val allSectionsCount = sectionDao.getTotalAllSectionsCount()

            // Вычисляем процент прогресса
            val calculatedProgress = if (allSectionsCount > 0) {
                (readSectionsCount * 100) / allSectionsCount
            } else {
                0
            }

            // Получаем текущую статистику или создаем новую
            val currentStats = userProgressDao.getUserProgress().first() ?: UserProgressEntity(
                overallProgress = calculatedProgress,
                completedSections = readSectionsCount,
                totalSections = allSectionsCount
            )

            // Проверяем, отличается ли рассчитанный прогресс от текущего
            if (forceUpdate || currentStats.overallProgress != calculatedProgress ||
                currentStats.completedSections != readSectionsCount ||
                currentStats.totalSections != allSectionsCount
            ) {

                Timber.d(
                    "Обновление общего прогресса: $calculatedProgress% " +
                            "(прочитано $readSectionsCount из $allSectionsCount разделов)"
                )

                // Сохраняем обновленную статистику
                val updatedStats = currentStats.copy(
                    overallProgress = calculatedProgress,
                    completedSections = readSectionsCount,
                    totalSections = allSectionsCount
                )
                userProgressDao.insertUserProgress(updatedStats)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении общего прогресса: ${e.message}")
        }
    }

    /**
     * Обновить общее количество разделов
     */
    suspend fun updateTotalSections(count: Int) {
        try {
            // Получаем текущую статистику
            val currentStats = userProgressDao.getUserProgress().first()

            if (currentStats == null || currentStats.totalSections != count) {
                // Обновляем общий прогресс
                updateOverallProgress(true)

                Timber.d("Обновлено общее количество разделов и пересчитан прогресс")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении общего количества разделов")
        }
    }

    /**
     * Отметить раздел как прочитанный
     */
    suspend fun markSectionAsRead(sectionId: String, chapterId: String) {
        try {
            // Обновляем статус чтения раздела
            sectionDao.updateSectionReadStatus(sectionId, true)

            // Добавляем в список недавно просмотренных
            chapterDao.insertRecentChapter(RecentChapterEntity(chapterId))

            // Добавляем в кэш (это важная часть, которая отсутствовала)
            completedSections.add(sectionId)

            // Обновляем прогресс главы на основе количества прочитанных разделов
            updateChapterProgressBasedOnReadSections(chapterId)

            // Выполняем принудительное обновление общего прогресса
            resetAndRecalculateCompletedSections()

            Timber.d("Раздел $sectionId отмечен как прочитанный")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при отметке раздела $sectionId как прочитанного")
        }
    }

    /**
     * Получить статус прочтения раздела
     */
    suspend fun isSectionRead(sectionId: String): Boolean {
        return try {
            // Проверяем статус в локальной базе
            val localStatus = sectionDao.getSectionReadStatus(sectionId) ?: false
            return localStatus
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении статуса прочтения раздела")
            false
        }
    }

    /**
     * Обновить прогресс главы на основе прочитанных разделов
     */
    private suspend fun updateChapterProgressBasedOnReadSections(chapterId: String) {
        try {
            // Получаем количество прочитанных разделов
            val readCount = sectionDao.getReadSectionsCount(chapterId)

            // Получаем общее количество разделов
            val totalCount = sectionDao.getTotalSectionsCount(chapterId)

            // Вычисляем процент прогресса
            val progress = if (totalCount > 0) (readCount * 100) / totalCount else 0

            // Обновляем прогресс главы
            userProgressDao.updateChapterProgress(chapterId, progress)

            // Принудительно пересчитываем общий прогресс
            updateOverallProgress(true)

            Timber.d("Обновлен прогресс главы $chapterId: $progress% ($readCount из $totalCount разделов)")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении прогресса главы $chapterId: ${e.message}")
        }
    }
} 