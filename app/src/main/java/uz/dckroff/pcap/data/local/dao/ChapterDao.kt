package uz.dckroff.pcap.data.local.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import uz.dckroff.pcap.data.local.entity.ChapterEntity
import uz.dckroff.pcap.data.local.entity.RecentChapterEntity

/**
 * DAO для взаимодействия с главами учебника в Room
 */
@Dao
interface ChapterDao {

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun getChaptersCount(): Int

    @Query("SELECT * FROM chapters ORDER BY `order`")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY `order` LIMIT :limit")
    fun getLimitedChapters(limit: Int): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()

    @Transaction
    suspend fun replaceAllChapters(chapters: List<ChapterEntity>) {
        chapters.forEach { Timber.d("Глава: ${it.id} - ${it.title}") }
        deleteAllChapters()
        Log.e("TAG", "replaceAllChapters: insertedChapters " + insertChapters(chapters).size)
    }

    // Методы для работы с недавно просмотренными главами
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentChapter(recentChapter: RecentChapterEntity)

    @Query("SELECT c.* FROM chapters c INNER JOIN recent_chapters rc ON c.id = rc.id ORDER BY rc.lastViewedAt DESC")
    fun getRecentChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE progress < 50 ORDER BY progress LIMIT :limit")
    fun getRecommendedChapters(limit: Int): Flow<List<ChapterEntity>>
} 