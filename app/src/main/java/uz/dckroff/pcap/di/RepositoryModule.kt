package uz.dckroff.pcap.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.dckroff.pcap.data.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGlossaryRepository(
        glossaryRepositoryImpl: GlossaryRepositoryImpl
    ): GlossaryRepository {
        return glossaryRepositoryImpl
    }
    
    @Provides
    @Singleton
    fun provideBookmarkRepository(
        bookmarkRepositoryImpl: BookmarkRepositoryImpl
    ): BookmarkRepository {
        return bookmarkRepositoryImpl
    }
    
    @Provides
    @Singleton
    fun provideBookmarksRepository(
        bookmarksRepositoryImpl: BookmarksRepositoryImpl
    ): BookmarksRepository {
        return bookmarksRepositoryImpl
    }
} 