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
} 