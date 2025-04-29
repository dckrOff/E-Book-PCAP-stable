package uz.dckroff.pcap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uz.dckroff.pcap.data.local.entity.UserProgressEntity

/**
 * DAO для взаимодействия с прогрессом пользователя в Room
 */
@Dao
interface UserProgressDao {
    
    @Query("SELECT * FROM user_progress LIMIT 1")
    fun getUserProgress(): Flow<UserProgressEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(userProgress: UserProgressEntity)
    
    @Query("UPDATE chapters SET progress = :progress WHERE id = :chapterId")
    suspend fun updateChapterProgress(chapterId: String, progress: Int)
    
    @Query("SELECT AVG(progress) FROM chapters")
    suspend fun calculateOverallProgress(): Int?
    
    @Query("UPDATE chapters SET lastReadSectionId = :sectionId, lastReadPosition = :position, lastReadTimestamp = :timestamp WHERE id = :chapterId")
    suspend fun updateReadingPosition(chapterId: String, sectionId: String, position: Int, timestamp: Long)
    
    @Query("SELECT lastReadSectionId FROM chapters WHERE id = :chapterId")
    suspend fun getLastReadSectionId(chapterId: String): String?
    
    @Query("SELECT lastReadPosition FROM chapters WHERE id = :chapterId")
    suspend fun getLastReadPosition(chapterId: String): Int?
    
    @Query("UPDATE user_progress SET completedSections = completedSections + 1")
    suspend fun incrementCompletedSections()
    
    @Query("UPDATE user_progress SET completedSections = :count")
    suspend fun updateCompletedSections(count: Int)
    
    @Query("UPDATE user_progress SET totalSections = :count")
    suspend fun updateTotalSections(count: Int)
    
    /**
     * Обновить статус прочтения раздела
     */
    @Query("UPDATE sections SET hasSectionRead = :isRead WHERE id = :sectionId")
    suspend fun updateSectionReadStatus(sectionId: String, isRead: Boolean)
    
    /**
     * Получить статус прочтения раздела
     */
    @Query("SELECT hasSectionRead FROM sections WHERE id = :sectionId")
    suspend fun getSectionReadStatus(sectionId: String): Boolean?
} 