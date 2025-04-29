package uz.dckroff.pcap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uz.dckroff.pcap.data.local.entity.SectionEntity

/**
 * DAO для взаимодействия с разделами в Room
 */
@Dao
interface SectionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>)
    
    @Query("SELECT * FROM sections WHERE chapterId = :chapterId ORDER BY `order`")
    fun getSectionsForChapter(chapterId: String): Flow<List<SectionEntity>>
    
    @Query("SELECT * FROM sections WHERE id = :sectionId")
    suspend fun getSectionById(sectionId: String): SectionEntity?
    
    @Query("UPDATE sections SET hasSectionRead = :isRead WHERE id = :sectionId")
    suspend fun updateSectionReadStatus(sectionId: String, isRead: Boolean)
    
    @Query("SELECT hasSectionRead FROM sections WHERE id = :sectionId")
    suspend fun getSectionReadStatus(sectionId: String): Boolean?
    
    @Query("SELECT COUNT(*) FROM sections WHERE chapterId = :chapterId AND hasSectionRead = 1")
    suspend fun getReadSectionsCount(chapterId: String): Int
    
    @Query("SELECT COUNT(*) FROM sections WHERE chapterId = :chapterId")
    suspend fun getTotalSectionsCount(chapterId: String): Int
    
    @Query("SELECT COUNT(*) FROM sections WHERE hasSectionRead = 1")
    suspend fun getTotalReadSectionsCount(): Int
    
    @Query("SELECT COUNT(*) FROM sections")
    suspend fun getTotalAllSectionsCount(): Int
} 