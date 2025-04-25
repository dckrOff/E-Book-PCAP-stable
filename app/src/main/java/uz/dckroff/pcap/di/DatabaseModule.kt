package uz.dckroff.pcap.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.dckroff.pcap.data.local.AppDatabase
import uz.dckroff.pcap.data.local.dao.ChapterDao
import uz.dckroff.pcap.data.local.dao.UserProgressDao
import javax.inject.Singleton

/**
 * Модуль Dagger Hilt для предоставления Room зависимостей
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideChapterDao(appDatabase: AppDatabase): ChapterDao {
        return appDatabase.chapterDao()
    }
    
    @Provides
    @Singleton
    fun provideUserProgressDao(appDatabase: AppDatabase): UserProgressDao {
        return appDatabase.userProgressDao()
    }
} 