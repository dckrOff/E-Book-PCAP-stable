package uz.dckroff.pcap.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uz.dckroff.pcap.data.local.dao.ChapterDao
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
    private val firestore: FirebaseFirestore,
    private val userProgressDao: UserProgressDao,
    private val chapterDao: ChapterDao
) {
    /**
     * Получить общий прогресс пользователя по всем разделам
     */
    fun getOverallProgress(): Flow<Int> {
        refreshOverallProgressFromFirestore()
        return userProgressDao.getUserProgress().map { entity ->
            entity?.overallProgress ?: 0
        }
    }

    /**
     * Обновить прогресс пользователя по конкретному разделу
     */
    suspend fun updateChapterProgress(chapterId: String, progress: Int) {
        try {
            // Обновляем прогресс в локальной базе данных
            userProgressDao.updateChapterProgress(chapterId, progress)
            
            // Добавляем в список недавно просмотренных
            chapterDao.insertRecentChapter(RecentChapterEntity(chapterId))
            
            // Пересчитываем общий прогресс
            updateOverallProgress()
            
            // Синхронизируем с Firestore
            syncProgressToFirestore(chapterId, progress)
        } catch (e: Exception) {
            Timber.e(e, "Error updating chapter progress")
        }
    }
    
    /**
     * Обновить общий прогресс пользователя на основе прогресса по всем разделам
     */
    private suspend fun updateOverallProgress() {
        try {
            // Вычисляем средний прогресс по всем разделам
            val overallProgress = userProgressDao.calculateOverallProgress() ?: 0
            
            // Сохраняем в локальную базу данных
            userProgressDao.insertUserProgress(
                UserProgressEntity(
                    overallProgress = overallProgress
                )
            )
            
            // Синхронизируем с Firestore
            syncOverallProgressToFirestore(overallProgress)
        } catch (e: Exception) {
            Timber.e(e, "Error updating overall progress")
        }
    }
    
    /**
     * Синхронизировать прогресс по разделу с Firestore
     */
    private suspend fun syncProgressToFirestore(chapterId: String, progress: Int) {
        try {
            // Обновляем прогресс по разделу в Firestore
            firestore.collection("userProgress")
                .document("currentUser") // В будущем здесь будет идентификатор текущего пользователя
                .collection("chapterProgress")
                .document(chapterId)
                .set(mapOf(
                    "progress" to progress,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                ))
                .await()
            
            // Добавляем в список недавно просмотренных
            firestore.collection("userProgress")
                .document("currentUser")
                .collection("recentChapters")
                .document(chapterId)
                .set(mapOf(
                    "chapterId" to chapterId,
                    "lastViewedAt" to com.google.firebase.Timestamp.now()
                ))
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error syncing chapter progress to Firestore")
        }
    }
    
    /**
     * Синхронизировать общий прогресс с Firestore
     */
    private suspend fun syncOverallProgressToFirestore(overallProgress: Int) {
        try {
            // Обновляем общий прогресс пользователя в Firestore
            firestore.collection("userProgress")
                .document("currentUser")
                .set(mapOf(
                    "overallProgress" to overallProgress,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                ))
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error syncing overall progress to Firestore")
        }
    }
    
    /**
     * Загрузить общий прогресс из Firestore
     */
    private fun refreshOverallProgressFromFirestore() {
        try {
            firestore.collection("userProgress")
                .document("currentUser")
                .get()
                .addOnSuccessListener { document ->
                    val progress = document.getLong("overallProgress")?.toInt() ?: 0
                    
                    // Сохраняем в локальную базу данных
                    kotlinx.coroutines.GlobalScope.launch {
                        userProgressDao.insertUserProgress(
                            UserProgressEntity(
                                overallProgress = progress
                            )
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Error fetching overall progress from Firestore")
                }
        } catch (e: Exception) {
            Timber.e(e, "Error in refreshOverallProgressFromFirestore")
        }
    }
} 