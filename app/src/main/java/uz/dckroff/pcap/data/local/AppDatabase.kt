package uz.dckroff.pcap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import uz.dckroff.pcap.data.local.dao.ChapterDao
import uz.dckroff.pcap.data.local.dao.UserProgressDao
import uz.dckroff.pcap.data.local.entity.ChapterEntity
import uz.dckroff.pcap.data.local.entity.RecentChapterEntity
import uz.dckroff.pcap.data.local.entity.UserProgressEntity
import uz.dckroff.pcap.data.local.util.Converters

/**
 * Room база данных приложения
 */
@Database(
    entities = [
        ChapterEntity::class,
        UserProgressEntity::class,
        RecentChapterEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun chapterDao(): ChapterDao
    abstract fun userProgressDao(): UserProgressDao
    
    companion object {
        private const val DATABASE_NAME = "pcap_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
} 